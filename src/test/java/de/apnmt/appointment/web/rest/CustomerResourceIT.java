package de.apnmt.appointment.web.rest;

import de.apnmt.appointment.IntegrationTest;
import de.apnmt.appointment.common.domain.Customer;
import de.apnmt.appointment.common.repository.CustomerRepository;
import de.apnmt.appointment.common.service.dto.CustomerDTO;
import de.apnmt.appointment.common.service.mapper.CustomerMapper;
import de.apnmt.appointment.common.web.rest.CustomerResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link CustomerResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CustomerResourceIT {

    private static final String DEFAULT_LAST_NAME = "AAAAAAAAAA";
    private static final String UPDATED_LAST_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_FIRST_NAME = "AAAAAAAAAA";
    private static final String UPDATED_FIRST_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_MAIL = "AAAAAAAAAA";
    private static final String UPDATED_MAIL = "BBBBBBBBBB";

    private static final String DEFAULT_PHONE = "AAAAAAAAAA";
    private static final String UPDATED_PHONE = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_BIRTHDAY = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_BIRTHDAY = LocalDate.now(ZoneId.systemDefault());

    private static final Long DEFAULT_ORGANIZATION_ID = 1L;
    private static final Long UPDATED_ORGANIZATION_ID = 2L;

    private static final String ENTITY_API_URL = "/api/customers";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCustomerMockMvc;

    private Customer customer;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Customer createEntity(EntityManager em) {
        Customer customer = new Customer()
            .lastName(DEFAULT_LAST_NAME)
            .firstName(DEFAULT_FIRST_NAME)
            .mail(DEFAULT_MAIL)
            .phone(DEFAULT_PHONE)
            .birthday(DEFAULT_BIRTHDAY)
            .organizationId(DEFAULT_ORGANIZATION_ID);
        return customer;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Customer createUpdatedEntity(EntityManager em) {
        Customer customer = new Customer()
            .lastName(UPDATED_LAST_NAME)
            .firstName(UPDATED_FIRST_NAME)
            .mail(UPDATED_MAIL)
            .phone(UPDATED_PHONE)
            .birthday(UPDATED_BIRTHDAY)
            .organizationId(UPDATED_ORGANIZATION_ID);
        return customer;
    }

    @BeforeEach
    public void initTest() {
        this.customer = createEntity(this.em);
    }

    @Test
    @Transactional
    void createCustomer() throws Exception {
        int databaseSizeBeforeCreate = this.customerRepository.findAll().size();
        // Create the Customer
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);
        this.restCustomerMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isCreated());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeCreate + 1);
        Customer testCustomer = customerList.get(customerList.size() - 1);
        assertThat(testCustomer.getLastName()).isEqualTo(DEFAULT_LAST_NAME);
        assertThat(testCustomer.getFirstName()).isEqualTo(DEFAULT_FIRST_NAME);
        assertThat(testCustomer.getMail()).isEqualTo(DEFAULT_MAIL);
        assertThat(testCustomer.getPhone()).isEqualTo(DEFAULT_PHONE);
        assertThat(testCustomer.getBirthday()).isEqualTo(DEFAULT_BIRTHDAY);
        assertThat(testCustomer.getOrganizationId()).isEqualTo(DEFAULT_ORGANIZATION_ID);
    }

    @Test
    @Transactional
    void createCustomerWithExistingId() throws Exception {
        // Create the Customer with an existing ID
        this.customer.setId(1L);
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        int databaseSizeBeforeCreate = this.customerRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restCustomerMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkLastNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.customerRepository.findAll().size();
        // set the field null
        this.customer.setLastName(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        this.restCustomerMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isBadRequest());

        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFirstNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.customerRepository.findAll().size();
        // set the field null
        this.customer.setFirstName(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        this.restCustomerMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isBadRequest());

        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMailIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.customerRepository.findAll().size();
        // set the field null
        this.customer.setMail(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        this.restCustomerMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isBadRequest());

        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPhoneIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.customerRepository.findAll().size();
        // set the field null
        this.customer.setPhone(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        this.restCustomerMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isBadRequest());

        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkBirthdayIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.customerRepository.findAll().size();
        // set the field null
        this.customer.setBirthday(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        this.restCustomerMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isBadRequest());

        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOrganizationIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.customerRepository.findAll().size();
        // set the field null
        this.customer.setOrganizationId(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        this.restCustomerMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isBadRequest());

        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCustomers() throws Exception {
        // Initialize the database
        this.customerRepository.saveAndFlush(this.customer);

        // Get all the customerList
        this.restCustomerMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.customer.getId().intValue())))
            .andExpect(jsonPath("$.[*].lastName").value(hasItem(DEFAULT_LAST_NAME)))
            .andExpect(jsonPath("$.[*].firstName").value(hasItem(DEFAULT_FIRST_NAME)))
            .andExpect(jsonPath("$.[*].mail").value(hasItem(DEFAULT_MAIL)))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE)))
            .andExpect(jsonPath("$.[*].birthday").value(hasItem(DEFAULT_BIRTHDAY.toString())))
            .andExpect(jsonPath("$.[*].organizationId").value(hasItem(DEFAULT_ORGANIZATION_ID.intValue())));
    }

    @Test
    @Transactional
    void getCustomer() throws Exception {
        // Initialize the database
        this.customerRepository.saveAndFlush(this.customer);

        // Get the customer
        this.restCustomerMockMvc
            .perform(get(ENTITY_API_URL_ID, this.customer.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(this.customer.getId().intValue()))
            .andExpect(jsonPath("$.lastName").value(DEFAULT_LAST_NAME))
            .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRST_NAME))
            .andExpect(jsonPath("$.mail").value(DEFAULT_MAIL))
            .andExpect(jsonPath("$.phone").value(DEFAULT_PHONE))
            .andExpect(jsonPath("$.birthday").value(DEFAULT_BIRTHDAY.toString()))
            .andExpect(jsonPath("$.organizationId").value(DEFAULT_ORGANIZATION_ID.intValue()));
    }

    @Test
    @Transactional
    void getNonExistingCustomer() throws Exception {
        // Get the customer
        this.restCustomerMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewCustomer() throws Exception {
        // Initialize the database
        this.customerRepository.saveAndFlush(this.customer);

        int databaseSizeBeforeUpdate = this.customerRepository.findAll().size();

        // Update the customer
        Customer updatedCustomer = this.customerRepository.findById(this.customer.getId()).get();
        // Disconnect from session so that the updates on updatedCustomer are not directly saved in db
        this.em.detach(updatedCustomer);
        updatedCustomer
            .lastName(UPDATED_LAST_NAME)
            .firstName(UPDATED_FIRST_NAME)
            .mail(UPDATED_MAIL)
            .phone(UPDATED_PHONE)
            .birthday(UPDATED_BIRTHDAY)
            .organizationId(UPDATED_ORGANIZATION_ID);
        CustomerDTO customerDTO = this.customerMapper.toDto(updatedCustomer);

        this.restCustomerMockMvc
            .perform(
                put(ENTITY_API_URL_ID, customerDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(customerDTO))
            )
            .andExpect(status().isOk());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
        Customer testCustomer = customerList.get(customerList.size() - 1);
        assertThat(testCustomer.getLastName()).isEqualTo(UPDATED_LAST_NAME);
        assertThat(testCustomer.getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
        assertThat(testCustomer.getMail()).isEqualTo(UPDATED_MAIL);
        assertThat(testCustomer.getPhone()).isEqualTo(UPDATED_PHONE);
        assertThat(testCustomer.getBirthday()).isEqualTo(UPDATED_BIRTHDAY);
        assertThat(testCustomer.getOrganizationId()).isEqualTo(UPDATED_ORGANIZATION_ID);
    }

    @Test
    @Transactional
    void putNonExistingCustomer() throws Exception {
        int databaseSizeBeforeUpdate = this.customerRepository.findAll().size();
        this.customer.setId(count.incrementAndGet());

        // Create the Customer
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restCustomerMockMvc
            .perform(
                put(ENTITY_API_URL_ID, customerDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(customerDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCustomer() throws Exception {
        int databaseSizeBeforeUpdate = this.customerRepository.findAll().size();
        this.customer.setId(count.incrementAndGet());

        // Create the Customer
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restCustomerMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(customerDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCustomer() throws Exception {
        int databaseSizeBeforeUpdate = this.customerRepository.findAll().size();
        this.customer.setId(count.incrementAndGet());

        // Create the Customer
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restCustomerMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCustomerWithPatch() throws Exception {
        // Initialize the database
        this.customerRepository.saveAndFlush(this.customer);

        int databaseSizeBeforeUpdate = this.customerRepository.findAll().size();

        // Update the customer using partial update
        Customer partialUpdatedCustomer = new Customer();
        partialUpdatedCustomer.setId(this.customer.getId());

        partialUpdatedCustomer.lastName(UPDATED_LAST_NAME).birthday(UPDATED_BIRTHDAY);

        this.restCustomerMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCustomer.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedCustomer))
            )
            .andExpect(status().isOk());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
        Customer testCustomer = customerList.get(customerList.size() - 1);
        assertThat(testCustomer.getLastName()).isEqualTo(UPDATED_LAST_NAME);
        assertThat(testCustomer.getFirstName()).isEqualTo(DEFAULT_FIRST_NAME);
        assertThat(testCustomer.getMail()).isEqualTo(DEFAULT_MAIL);
        assertThat(testCustomer.getPhone()).isEqualTo(DEFAULT_PHONE);
        assertThat(testCustomer.getBirthday()).isEqualTo(UPDATED_BIRTHDAY);
        assertThat(testCustomer.getOrganizationId()).isEqualTo(DEFAULT_ORGANIZATION_ID);
    }

    @Test
    @Transactional
    void fullUpdateCustomerWithPatch() throws Exception {
        // Initialize the database
        this.customerRepository.saveAndFlush(this.customer);

        int databaseSizeBeforeUpdate = this.customerRepository.findAll().size();

        // Update the customer using partial update
        Customer partialUpdatedCustomer = new Customer();
        partialUpdatedCustomer.setId(this.customer.getId());

        partialUpdatedCustomer
            .lastName(UPDATED_LAST_NAME)
            .firstName(UPDATED_FIRST_NAME)
            .mail(UPDATED_MAIL)
            .phone(UPDATED_PHONE)
            .birthday(UPDATED_BIRTHDAY)
            .organizationId(UPDATED_ORGANIZATION_ID);

        this.restCustomerMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCustomer.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedCustomer))
            )
            .andExpect(status().isOk());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
        Customer testCustomer = customerList.get(customerList.size() - 1);
        assertThat(testCustomer.getLastName()).isEqualTo(UPDATED_LAST_NAME);
        assertThat(testCustomer.getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
        assertThat(testCustomer.getMail()).isEqualTo(UPDATED_MAIL);
        assertThat(testCustomer.getPhone()).isEqualTo(UPDATED_PHONE);
        assertThat(testCustomer.getBirthday()).isEqualTo(UPDATED_BIRTHDAY);
        assertThat(testCustomer.getOrganizationId()).isEqualTo(UPDATED_ORGANIZATION_ID);
    }

    @Test
    @Transactional
    void patchNonExistingCustomer() throws Exception {
        int databaseSizeBeforeUpdate = this.customerRepository.findAll().size();
        this.customer.setId(count.incrementAndGet());

        // Create the Customer
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restCustomerMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, customerDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(customerDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCustomer() throws Exception {
        int databaseSizeBeforeUpdate = this.customerRepository.findAll().size();
        this.customer.setId(count.incrementAndGet());

        // Create the Customer
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restCustomerMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(customerDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCustomer() throws Exception {
        int databaseSizeBeforeUpdate = this.customerRepository.findAll().size();
        this.customer.setId(count.incrementAndGet());

        // Create the Customer
        CustomerDTO customerDTO = this.customerMapper.toDto(this.customer);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restCustomerMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(customerDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Customer in the database
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCustomer() throws Exception {
        // Initialize the database
        this.customerRepository.saveAndFlush(this.customer);

        int databaseSizeBeforeDelete = this.customerRepository.findAll().size();

        // Delete the customer
        this.restCustomerMockMvc
            .perform(delete(ENTITY_API_URL_ID, this.customer.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Customer> customerList = this.customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
