package de.apnmt.appointment.web.rest;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.appointment.IntegrationTest;
import de.apnmt.appointment.common.domain.Customer;
import de.apnmt.appointment.common.domain.Service;
import de.apnmt.appointment.common.repository.ServiceRepository;
import de.apnmt.appointment.common.service.dto.ServiceDTO;
import de.apnmt.appointment.common.service.mapper.ServiceMapper;
import de.apnmt.appointment.common.web.rest.ServiceResource;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.ServiceEventDTO;
import de.apnmt.k8s.common.test.AbstractEventSenderIT;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@link ServiceResource} REST controller.
 */

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.SERVICE_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
class ServiceResourceIT extends AbstractEventSenderIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Integer DEFAULT_DURATION = 1;
    private static final Integer UPDATED_DURATION = 2;

    private static final Double DEFAULT_COST = 1D;
    private static final Double UPDATED_COST = 2D;

    private static final Long DEFAULT_ORGANIZATION_ID = 1L;
    private static final Long UPDATED_ORGANIZATION_ID = 505L;

    private static final String ENTITY_API_URL = "/api/services";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceMapper serviceMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restServiceMockMvc;

    private Service service;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Service createEntity(EntityManager em) {
        Service service = new Service().name(DEFAULT_NAME).description(DEFAULT_DESCRIPTION).duration(DEFAULT_DURATION).cost(DEFAULT_COST).organizationId(DEFAULT_ORGANIZATION_ID);
        return service;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Service createUpdatedEntity(EntityManager em) {
        Service service = new Service().name(UPDATED_NAME).description(UPDATED_DESCRIPTION).duration(UPDATED_DURATION).cost(UPDATED_COST).organizationId(UPDATED_ORGANIZATION_ID);
        return service;
    }

    @Override
    public String getTopic() {
        return TopicConstants.SERVICE_CHANGED_TOPIC;
    }

    @BeforeEach
    public void initTest() {
        this.service = createEntity(this.em);
    }

    @AfterEach
    public void shutDown() throws InterruptedException {
        // All topics should be empty now
        assertThat(this.records.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @Transactional
    void createService() throws Exception {
        int databaseSizeBeforeCreate = this.serviceRepository.findAll().size();
        // Create the Service
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);
        this.restServiceMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isCreated());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeCreate + 1);
        Service testService = serviceList.get(serviceList.size() - 1);
        assertThat(testService.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testService.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testService.getDuration()).isEqualTo(DEFAULT_DURATION);
        assertThat(testService.getCost()).isEqualTo(DEFAULT_COST);
        assertThat(testService.getOrganizationId()).isEqualTo(DEFAULT_ORGANIZATION_ID);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ServiceEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ServiceEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.serviceCreated);
        ServiceEventDTO serviceEventDTO = eventResult.getValue();
        assertThat(serviceEventDTO.getId()).isEqualTo(testService.getId());
        assertThat(serviceEventDTO.getOrganizationId()).isEqualTo(testService.getOrganizationId());
        assertThat(serviceEventDTO.getName()).isEqualTo(testService.getName());
        assertThat(serviceEventDTO.getDescription()).isEqualTo(testService.getDescription());
        assertThat(serviceEventDTO.getDuration()).isEqualTo(testService.getDuration());
        assertThat(serviceEventDTO.getCost()).isEqualTo(testService.getCost());
    }

    @Test
    @Transactional
    void createServiceWithExistingId() throws Exception {
        // Create the Service with an existing ID
        this.service.setId(1L);
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        int databaseSizeBeforeCreate = this.serviceRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restServiceMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.serviceRepository.findAll().size();
        // set the field null
        this.service.setName(null);

        // Create the Service, which fails.
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        this.restServiceMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isBadRequest());

        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDescriptionIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.serviceRepository.findAll().size();
        // set the field null
        this.service.setDescription(null);

        // Create the Service, which fails.
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        this.restServiceMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isBadRequest());

        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDurationIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.serviceRepository.findAll().size();
        // set the field null
        this.service.setDuration(null);

        // Create the Service, which fails.
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        this.restServiceMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isBadRequest());

        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCostIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.serviceRepository.findAll().size();
        // set the field null
        this.service.setCost(null);

        // Create the Service, which fails.
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        this.restServiceMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isBadRequest());

        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOrganizationIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.serviceRepository.findAll().size();
        // set the field null
        this.service.setOrganizationId(null);

        // Create the Service, which fails.
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        this.restServiceMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isBadRequest());

        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllServices() throws Exception {
        // Initialize the database
        this.serviceRepository.saveAndFlush(this.service);

        // Get all the serviceList
        this.restServiceMockMvc.perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.service.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].duration").value(hasItem(DEFAULT_DURATION)))
            .andExpect(jsonPath("$.[*].cost").value(hasItem(DEFAULT_COST.doubleValue())))
            .andExpect(jsonPath("$.[*].organizationId").value(hasItem(DEFAULT_ORGANIZATION_ID.intValue())));
    }

    @Test
    @Transactional
    void getAllServicesForOrganization() throws Exception {
        // Initialize the database
        this.serviceRepository.saveAndFlush(this.service);

        // Get all the serviceList
        this.restServiceMockMvc.perform(get(ENTITY_API_URL + "/organization/" + DEFAULT_ORGANIZATION_ID +  "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*]").value(hasSize(3)))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.service.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].duration").value(hasItem(DEFAULT_DURATION)))
            .andExpect(jsonPath("$.[*].cost").value(hasItem(DEFAULT_COST.doubleValue())))
            .andExpect(jsonPath("$.[*].organizationId").value(hasItem(DEFAULT_ORGANIZATION_ID.intValue())));
    }

    @Test
    @Transactional
    void getAllServicesForOrganizationEmpty() throws Exception {
        // Initialize the database
        this.serviceRepository.saveAndFlush(this.service);

        // Get all the serviceList
        this.restServiceMockMvc.perform(get(ENTITY_API_URL + "/organization/" + UPDATED_ORGANIZATION_ID +  "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*]").value(hasSize(0)));
    }

    @Test
    @Transactional
    void getService() throws Exception {
        // Initialize the database
        this.serviceRepository.saveAndFlush(this.service);

        // Get the service
        this.restServiceMockMvc.perform(get(ENTITY_API_URL_ID, this.service.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(this.service.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.duration").value(DEFAULT_DURATION))
            .andExpect(jsonPath("$.cost").value(DEFAULT_COST.doubleValue()))
            .andExpect(jsonPath("$.organizationId").value(DEFAULT_ORGANIZATION_ID.intValue()));
    }

    @Test
    @Transactional
    void getNonExistingService() throws Exception {
        // Get the service
        this.restServiceMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewService() throws Exception {
        // Initialize the database
        this.serviceRepository.saveAndFlush(this.service);

        int databaseSizeBeforeUpdate = this.serviceRepository.findAll().size();

        // Update the service
        Service updatedService = this.serviceRepository.findById(this.service.getId()).get();
        // Disconnect from session so that the updates on updatedService are not directly saved in db
        this.em.detach(updatedService);
        updatedService.name(UPDATED_NAME).description(UPDATED_DESCRIPTION).duration(UPDATED_DURATION).cost(UPDATED_COST).organizationId(UPDATED_ORGANIZATION_ID);
        ServiceDTO serviceDTO = this.serviceMapper.toDto(updatedService);

        this.restServiceMockMvc.perform(put(ENTITY_API_URL_ID, serviceDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isOk());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeUpdate);
        Service testService = serviceList.get(serviceList.size() - 1);
        assertThat(testService.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testService.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testService.getDuration()).isEqualTo(UPDATED_DURATION);
        assertThat(testService.getCost()).isEqualTo(UPDATED_COST);
        assertThat(testService.getOrganizationId()).isEqualTo(UPDATED_ORGANIZATION_ID);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ServiceEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ServiceEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.serviceCreated);
        ServiceEventDTO serviceEventDTO = eventResult.getValue();
        assertThat(serviceEventDTO.getId()).isEqualTo(testService.getId());
        assertThat(serviceEventDTO.getOrganizationId()).isEqualTo(testService.getOrganizationId());
        assertThat(serviceEventDTO.getName()).isEqualTo(testService.getName());
        assertThat(serviceEventDTO.getDescription()).isEqualTo(testService.getDescription());
        assertThat(serviceEventDTO.getDuration()).isEqualTo(testService.getDuration());
        assertThat(serviceEventDTO.getCost()).isEqualTo(testService.getCost());
    }

    @Test
    @Transactional
    void putNonExistingService() throws Exception {
        int databaseSizeBeforeUpdate = this.serviceRepository.findAll().size();
        this.service.setId(count.incrementAndGet());

        // Create the Service
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restServiceMockMvc.perform(put(ENTITY_API_URL_ID, serviceDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchService() throws Exception {
        int databaseSizeBeforeUpdate = this.serviceRepository.findAll().size();
        this.service.setId(count.incrementAndGet());

        // Create the Service
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restServiceMockMvc.perform(put(ENTITY_API_URL_ID, count.incrementAndGet()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(serviceDTO))).andExpect(status().isBadRequest());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamService() throws Exception {
        int databaseSizeBeforeUpdate = this.serviceRepository.findAll().size();
        this.service.setId(count.incrementAndGet());

        // Create the Service
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restServiceMockMvc.perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateServiceWithPatch() throws Exception {
        // Initialize the database
        this.serviceRepository.saveAndFlush(this.service);

        int databaseSizeBeforeUpdate = this.serviceRepository.findAll().size();

        // Update the service using partial update
        Service partialUpdatedService = new Service();
        partialUpdatedService.setId(this.service.getId());

        partialUpdatedService.duration(UPDATED_DURATION);

        this.restServiceMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedService.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(partialUpdatedService))).andExpect(status().isOk());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeUpdate);
        Service testService = serviceList.get(serviceList.size() - 1);
        assertThat(testService.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testService.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testService.getDuration()).isEqualTo(UPDATED_DURATION);
        assertThat(testService.getCost()).isEqualTo(DEFAULT_COST);
        assertThat(testService.getOrganizationId()).isEqualTo(DEFAULT_ORGANIZATION_ID);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ServiceEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ServiceEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.serviceCreated);
        ServiceEventDTO serviceEventDTO = eventResult.getValue();
        assertThat(serviceEventDTO.getId()).isEqualTo(testService.getId());
        assertThat(serviceEventDTO.getOrganizationId()).isEqualTo(testService.getOrganizationId());
        assertThat(serviceEventDTO.getName()).isEqualTo(testService.getName());
        assertThat(serviceEventDTO.getDescription()).isEqualTo(testService.getDescription());
        assertThat(serviceEventDTO.getDuration()).isEqualTo(testService.getDuration());
        assertThat(serviceEventDTO.getCost()).isEqualTo(testService.getCost());
    }

    @Test
    @Transactional
    void fullUpdateServiceWithPatch() throws Exception {
        // Initialize the database
        this.serviceRepository.saveAndFlush(this.service);

        int databaseSizeBeforeUpdate = this.serviceRepository.findAll().size();

        // Update the service using partial update
        Service partialUpdatedService = new Service();
        partialUpdatedService.setId(this.service.getId());

        partialUpdatedService.name(UPDATED_NAME).description(UPDATED_DESCRIPTION).duration(UPDATED_DURATION).cost(UPDATED_COST).organizationId(UPDATED_ORGANIZATION_ID);

        this.restServiceMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedService.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(partialUpdatedService))).andExpect(status().isOk());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeUpdate);
        Service testService = serviceList.get(serviceList.size() - 1);
        assertThat(testService.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testService.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testService.getDuration()).isEqualTo(UPDATED_DURATION);
        assertThat(testService.getCost()).isEqualTo(UPDATED_COST);
        assertThat(testService.getOrganizationId()).isEqualTo(UPDATED_ORGANIZATION_ID);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ServiceEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ServiceEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.serviceCreated);
        ServiceEventDTO serviceEventDTO = eventResult.getValue();
        assertThat(serviceEventDTO.getId()).isEqualTo(testService.getId());
        assertThat(serviceEventDTO.getOrganizationId()).isEqualTo(testService.getOrganizationId());
        assertThat(serviceEventDTO.getName()).isEqualTo(testService.getName());
        assertThat(serviceEventDTO.getDescription()).isEqualTo(testService.getDescription());
        assertThat(serviceEventDTO.getDuration()).isEqualTo(testService.getDuration());
        assertThat(serviceEventDTO.getCost()).isEqualTo(testService.getCost());
    }

    @Test
    @Transactional
    void patchNonExistingService() throws Exception {
        int databaseSizeBeforeUpdate = this.serviceRepository.findAll().size();
        this.service.setId(count.incrementAndGet());

        // Create the Service
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restServiceMockMvc.perform(patch(ENTITY_API_URL_ID, serviceDTO.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(serviceDTO))).andExpect(status().isBadRequest());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchService() throws Exception {
        int databaseSizeBeforeUpdate = this.serviceRepository.findAll().size();
        this.service.setId(count.incrementAndGet());

        // Create the Service
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restServiceMockMvc.perform(patch(ENTITY_API_URL_ID, count.incrementAndGet()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(serviceDTO))).andExpect(status().isBadRequest());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamService() throws Exception {
        int databaseSizeBeforeUpdate = this.serviceRepository.findAll().size();
        this.service.setId(count.incrementAndGet());

        // Create the Service
        ServiceDTO serviceDTO = this.serviceMapper.toDto(this.service);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restServiceMockMvc.perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(serviceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Service in the database
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteService() throws Exception {
        // Initialize the database
        this.serviceRepository.saveAndFlush(this.service);

        int databaseSizeBeforeDelete = this.serviceRepository.findAll().size();

        // Delete the service
        this.restServiceMockMvc.perform(delete(ENTITY_API_URL_ID, this.service.getId()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeDelete - 1);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ServiceEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ServiceEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.serviceDeleted);
        ServiceEventDTO serviceEventDTO = eventResult.getValue();
        assertThat(serviceEventDTO.getId()).isEqualTo(this.service.getId());
        assertThat(serviceEventDTO.getOrganizationId()).isEqualTo(this.service.getOrganizationId());
        assertThat(serviceEventDTO.getName()).isEqualTo(this.service.getName());
        assertThat(serviceEventDTO.getDescription()).isEqualTo(this.service.getDescription());
        assertThat(serviceEventDTO.getDuration()).isEqualTo(this.service.getDuration());
        assertThat(serviceEventDTO.getCost()).isEqualTo(this.service.getCost());
    }

    @Test
    @Transactional
    void deleteAllServices() throws Exception {
        // Initialize the database
        this.serviceRepository.saveAndFlush(this.service);

        int databaseSizeBeforeDelete = this.serviceRepository.findAll().size();

        // Delete the service
        this.restServiceMockMvc
            .perform(delete(ENTITY_API_URL).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Service> serviceList = this.serviceRepository.findAll();
        assertThat(serviceList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
