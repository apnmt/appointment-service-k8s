package de.apnmt.appointment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.apnmt.appointment.IntegrationTest;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.AppointmentEventDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnableKafka
@EmbeddedKafka(topics = {TopicConstants.APPOINTMENT_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
public class AppointmentEventSenderIT {

    private static final LocalDateTime DEFAULT_START_AT = LocalDateTime.of(2021, 12, 24, 0, 0, 11, 0);
    private static final LocalDateTime DEFAULT_END_AT = LocalDateTime.of(2021, 12, 25, 0, 0, 11, 0);

    private BlockingQueue<ConsumerRecord<String, Object>> records;

    private KafkaMessageListenerContainer<String, String> container;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private AppointmentEventSender appointmentEventSender;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(this.getConsumerProperties());
        ContainerProperties containerProperties = new ContainerProperties(TopicConstants.APPOINTMENT_CHANGED_TOPIC);
        this.container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        this.records = new LinkedBlockingQueue<>();
        this.container.setupMessageListener((MessageListener<String, Object>) this.records::add);
        this.container.start();
        ContainerTestUtils.waitForAssignment(this.container, this.embeddedKafkaBroker.getPartitionsPerTopic());
    }

    private Map<String, Object> getConsumerProperties() {
        return Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.embeddedKafkaBroker.getBrokersAsString(),
            ConsumerConfig.GROUP_ID_CONFIG, "consumer",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true",
            ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10",
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "60000",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    }

    @AfterEach
    public void tearDown() {
        this.container.stop();
    }

    @Test
    public void appointmentEventSenderTest() throws InterruptedException, JsonProcessingException {
        AppointmentEventDTO appointment = new AppointmentEventDTO();
        appointment.setId(1L);
        appointment.setStartAt(DEFAULT_START_AT);
        appointment.setEndAt(DEFAULT_END_AT);
        appointment.setEmployeeId(2L);
        appointment.setOrganizationId(3L);

        ApnmtEvent<AppointmentEventDTO> event = new ApnmtEvent<AppointmentEventDTO>().timestamp(LocalDateTime.now()).type(ApnmtEventType.appointmentCreated).value(appointment);
        this.appointmentEventSender.send(TopicConstants.APPOINTMENT_CHANGED_TOPIC, event);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<AppointmentEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<AppointmentEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.appointmentCreated);

        AppointmentEventDTO appointmentResult = eventResult.getValue();
        assertThat(appointmentResult.getId()).isEqualTo(appointment.getId());
        assertThat(appointmentResult.getStartAt()).isEqualTo(DEFAULT_START_AT);
        assertThat(appointmentResult.getEndAt()).isEqualTo(DEFAULT_END_AT);
        assertThat(appointmentResult.getEmployeeId()).isEqualTo(2L);
        assertThat(appointmentResult.getOrganizationId()).isEqualTo(3L);
    }

}
