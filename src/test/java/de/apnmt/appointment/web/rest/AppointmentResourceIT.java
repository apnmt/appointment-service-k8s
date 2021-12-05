package de.apnmt.appointment.web.rest;

import de.apnmt.appointment.IntegrationTest;
import de.apnmt.appointment.common.domain.Appointment;
import de.apnmt.appointment.common.repository.AppointmentRepository;
import de.apnmt.appointment.common.service.dto.AppointmentDTO;
import de.apnmt.appointment.common.service.mapper.AppointmentMapper;
import de.apnmt.appointment.common.web.rest.AppointmentResource;
import de.apnmt.common.event.value.AppointmentEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link AppointmentResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {AppointmentResourceIT.EventSenderConfig.class})
class AppointmentResourceIT {

    private static final LocalDateTime DEFAULT_START_AT = LocalDateTime.of(2021, 12, 24, 0, 0, 11, 0);
    private static final LocalDateTime UPDATED_START_AT = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    private static final LocalDateTime DEFAULT_END_AT = LocalDateTime.of(2021, 12, 25, 0, 0, 11, 0);
    private static final LocalDateTime UPDATED_END_AT = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Long DEFAULT_ORGANIZATION_ID = 1L;
    private static final Long UPDATED_ORGANIZATION_ID = 2L;

    private static final Long DEFAULT_EMPLOYEE_ID = 1L;
    private static final Long UPDATED_EMPLOYEE_ID = 2L;

    private static final String ENTITY_API_URL = "/api/appointments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAppointmentMockMvc;

    private Appointment appointment;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Appointment createEntity() {
        Appointment appointment = new Appointment()
            .startAt(DEFAULT_START_AT)
            .endAt(DEFAULT_END_AT)
            .organizationId(DEFAULT_ORGANIZATION_ID)
            .employeeId(DEFAULT_EMPLOYEE_ID);
        return appointment;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Appointment createUpdatedEntity() {
        Appointment appointment = new Appointment()
            .startAt(UPDATED_START_AT)
            .endAt(UPDATED_END_AT)
            .organizationId(UPDATED_ORGANIZATION_ID)
            .employeeId(UPDATED_EMPLOYEE_ID);
        return appointment;
    }

    public static List<Appointment> createAppointments() {
        List<Appointment> appointments = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            LocalDateTime start = LocalDateTime.now().minus(i + 1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime end = LocalDateTime.now().minus(i, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
            Appointment appointment = new Appointment().startAt(start).endAt(end).organizationId(DEFAULT_ORGANIZATION_ID).employeeId(DEFAULT_EMPLOYEE_ID);
            appointments.add(appointment);
        }
        LocalDateTime start = LocalDateTime.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = LocalDateTime.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
        Appointment appointment = new Appointment().startAt(start).endAt(end).organizationId(DEFAULT_ORGANIZATION_ID).employeeId(DEFAULT_EMPLOYEE_ID);
        appointments.add(appointment);
        return appointments;
    }

    @BeforeEach
    public void initTest() {
        this.appointment = createEntity();
    }

    @Test
    @Transactional
    void createAppointment() throws Exception {
        int databaseSizeBeforeCreate = this.appointmentRepository.findAll().size();
        // Create the Appointment
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);
        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isCreated());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeCreate + 1);
        Appointment testAppointment = appointmentList.get(appointmentList.size() - 1);
        assertThat(testAppointment.getStartAt()).isEqualTo(DEFAULT_START_AT);
        assertThat(testAppointment.getEndAt()).isEqualTo(DEFAULT_END_AT);
        assertThat(testAppointment.getOrganizationId()).isEqualTo(DEFAULT_ORGANIZATION_ID);
        assertThat(testAppointment.getEmployeeId()).isEqualTo(DEFAULT_EMPLOYEE_ID);
    }

    @Test
    @Transactional
    void createAppointmentWithExistingId() throws Exception {
        // Create the Appointment with an existing ID
        this.appointment.setId(1L);
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        int databaseSizeBeforeCreate = this.appointmentRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void createAppointmentSlotAvailable() throws Exception {
        List<Appointment> appointments = createAppointments();
        this.appointmentRepository.saveAll(appointments);

        this.appointment.startAt(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        this.appointment.endAt(LocalDateTime.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES));
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        int databaseSizeBeforeCreate = this.appointmentRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isCreated());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeCreate + 1);
    }

    @Test
    @Transactional
    void createAppointmentSlotNotAvailable() throws Exception {
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = LocalDateTime.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
        Appointment apnmt = new Appointment().startAt(start).endAt(end).organizationId(DEFAULT_ORGANIZATION_ID).employeeId(DEFAULT_EMPLOYEE_ID);

        this.appointmentRepository.saveAndFlush(apnmt);

        this.appointment.startAt(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        this.appointment.endAt(LocalDateTime.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES));
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        int databaseSizeBeforeCreate = this.appointmentRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().is5xxServerError());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void createAppointmentSlotNotAvailableEndBeforeAppointmentEnd() throws Exception {
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = LocalDateTime.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
        Appointment apnmt = new Appointment().startAt(start).endAt(end).organizationId(DEFAULT_ORGANIZATION_ID).employeeId(DEFAULT_EMPLOYEE_ID);

        this.appointmentRepository.saveAndFlush(apnmt);

        this.appointment.startAt(LocalDateTime.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES));
        this.appointment.endAt(LocalDateTime.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES));
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        int databaseSizeBeforeCreate = this.appointmentRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().is5xxServerError());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void createAppointmentSlotNotAvailableEndAfterAppointmentEnd() throws Exception {
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = LocalDateTime.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
        Appointment apnmt = new Appointment().startAt(start).endAt(end).organizationId(DEFAULT_ORGANIZATION_ID).employeeId(DEFAULT_EMPLOYEE_ID);

        this.appointmentRepository.saveAndFlush(apnmt);

        this.appointment.startAt(LocalDateTime.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES));
        this.appointment.endAt(LocalDateTime.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES));
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        int databaseSizeBeforeCreate = this.appointmentRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().is5xxServerError());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void createAppointmentSlotNotAvailableStartBeforeAppointmentStart() throws Exception {
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = LocalDateTime.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
        Appointment apnmt = new Appointment().startAt(start).endAt(end).organizationId(DEFAULT_ORGANIZATION_ID).employeeId(DEFAULT_EMPLOYEE_ID);

        this.appointmentRepository.saveAndFlush(apnmt);

        this.appointment.startAt(LocalDateTime.now().minus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES));
        this.appointment.endAt(LocalDateTime.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES));
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        int databaseSizeBeforeCreate = this.appointmentRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().is5xxServerError());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void createAppointmentSlotNotAvailableStartBeforeAppointmentStartAndEndAfterAppointmentEnd() throws Exception {
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = LocalDateTime.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
        Appointment apnmt = new Appointment().startAt(start).endAt(end).organizationId(DEFAULT_ORGANIZATION_ID).employeeId(DEFAULT_EMPLOYEE_ID);

        this.appointmentRepository.saveAndFlush(apnmt);

        this.appointment.startAt(LocalDateTime.now().minus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES));
        this.appointment.endAt(LocalDateTime.now().plus(90, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES));
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        int databaseSizeBeforeCreate = this.appointmentRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().is5xxServerError());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStartAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.appointmentRepository.findAll().size();
        // set the field null
        this.appointment.setStartAt(null);

        // Create the Appointment, which fails.
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isBadRequest());

        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEndAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.appointmentRepository.findAll().size();
        // set the field null
        this.appointment.setEndAt(null);

        // Create the Appointment, which fails.
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isBadRequest());

        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOrganizationIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.appointmentRepository.findAll().size();
        // set the field null
        this.appointment.setOrganizationId(null);

        // Create the Appointment, which fails.
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isBadRequest());

        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEmployeeIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.appointmentRepository.findAll().size();
        // set the field null
        this.appointment.setEmployeeId(null);

        // Create the Appointment, which fails.
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        this.restAppointmentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isBadRequest());

        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAppointments() throws Exception {
        // Initialize the database
        this.appointmentRepository.saveAndFlush(this.appointment);

        // Get all the appointmentList
        this.restAppointmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.appointment.getId().intValue())))
            .andExpect(jsonPath("$.[*].startAt").value(hasItem(DEFAULT_START_AT.toString())))
            .andExpect(jsonPath("$.[*].endAt").value(hasItem(DEFAULT_END_AT.toString())))
            .andExpect(jsonPath("$.[*].organizationId").value(hasItem(DEFAULT_ORGANIZATION_ID.intValue())))
            .andExpect(jsonPath("$.[*].employeeId").value(hasItem(DEFAULT_EMPLOYEE_ID.intValue())));
    }

    @Test
    @Transactional
    void getAppointment() throws Exception {
        // Initialize the database
        this.appointmentRepository.saveAndFlush(this.appointment);

        // Get the appointment
        this.restAppointmentMockMvc
            .perform(get(ENTITY_API_URL_ID, this.appointment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(this.appointment.getId().intValue()))
            .andExpect(jsonPath("$.startAt").value(DEFAULT_START_AT.toString()))
            .andExpect(jsonPath("$.endAt").value(DEFAULT_END_AT.toString()))
            .andExpect(jsonPath("$.organizationId").value(DEFAULT_ORGANIZATION_ID.intValue()))
            .andExpect(jsonPath("$.employeeId").value(DEFAULT_EMPLOYEE_ID.intValue()));
    }

    @Test
    @Transactional
    void getNonExistingAppointment() throws Exception {
        // Get the appointment
        this.restAppointmentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewAppointment() throws Exception {
        // Initialize the database
        this.appointmentRepository.saveAndFlush(this.appointment);

        int databaseSizeBeforeUpdate = this.appointmentRepository.findAll().size();

        // Update the appointment
        Appointment updatedAppointment = this.appointmentRepository.findById(this.appointment.getId()).get();
        // Disconnect from session so that the updates on updatedAppointment are not directly saved in db
        this.em.detach(updatedAppointment);
        updatedAppointment
            .startAt(UPDATED_START_AT)
            .endAt(UPDATED_END_AT)
            .organizationId(UPDATED_ORGANIZATION_ID)
            .employeeId(UPDATED_EMPLOYEE_ID);
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(updatedAppointment);

        this.restAppointmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, appointmentDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isOk());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeUpdate);
        Appointment testAppointment = appointmentList.get(appointmentList.size() - 1);
        assertThat(testAppointment.getStartAt()).isEqualTo(UPDATED_START_AT);
        assertThat(testAppointment.getEndAt()).isEqualTo(UPDATED_END_AT);
        assertThat(testAppointment.getOrganizationId()).isEqualTo(UPDATED_ORGANIZATION_ID);
        assertThat(testAppointment.getEmployeeId()).isEqualTo(UPDATED_EMPLOYEE_ID);
    }

    @Test
    @Transactional
    void putNonExistingAppointment() throws Exception {
        int databaseSizeBeforeUpdate = this.appointmentRepository.findAll().size();
        this.appointment.setId(count.incrementAndGet());

        // Create the Appointment
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restAppointmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, appointmentDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAppointment() throws Exception {
        int databaseSizeBeforeUpdate = this.appointmentRepository.findAll().size();
        this.appointment.setId(count.incrementAndGet());

        // Create the Appointment
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restAppointmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAppointment() throws Exception {
        int databaseSizeBeforeUpdate = this.appointmentRepository.findAll().size();
        this.appointment.setId(count.incrementAndGet());

        // Create the Appointment
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restAppointmentMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(appointmentDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAppointment() throws Exception {
        int databaseSizeBeforeUpdate = this.appointmentRepository.findAll().size();
        this.appointment.setId(count.incrementAndGet());

        // Create the Appointment
        AppointmentDTO appointmentDTO = this.appointmentMapper.toDto(this.appointment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restAppointmentMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(appointmentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Appointment in the database
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAppointment() throws Exception {
        // Initialize the database
        this.appointmentRepository.saveAndFlush(this.appointment);

        int databaseSizeBeforeDelete = this.appointmentRepository.findAll().size();

        // Delete the appointment
        this.restAppointmentMockMvc
            .perform(delete(ENTITY_API_URL_ID, this.appointment.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Appointment> appointmentList = this.appointmentRepository.findAll();
        assertThat(appointmentList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @TestConfiguration
    public static class EventSenderConfig {
        private final Logger log = LoggerFactory.getLogger(EventSenderConfig.class);

        @Bean
        public ApnmtEventSender<AppointmentEventDTO> sender() {
            return (topic, event) -> {
                this.log.info("Event send to topic {}", topic);
            };
        }

    }

}
