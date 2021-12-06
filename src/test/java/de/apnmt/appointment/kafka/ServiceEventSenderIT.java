package de.apnmt.appointment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.appointment.IntegrationTest;
import de.apnmt.common.ApnmtTestUtil;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.ServiceEventDTO;
import de.apnmt.k8s.common.test.AbstractEventSenderIT;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.SERVICE_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
@DirtiesContext
public class ServiceEventSenderIT extends AbstractEventSenderIT {

    @Autowired
    private ServiceEventSender serviceEventSender;

    @Override
    public String getTopic() {
        return TopicConstants.SERVICE_CHANGED_TOPIC;
    }

    @Test
    public void appointmentEventSenderTest() throws InterruptedException, JsonProcessingException {

        ApnmtEvent<ServiceEventDTO> event = ApnmtTestUtil.createServiceEvent(ApnmtEventType.serviceCreated);
        this.serviceEventSender.send(TopicConstants.SERVICE_CHANGED_TOPIC, event);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ServiceEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ServiceEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult).isEqualTo(event);
    }

}
