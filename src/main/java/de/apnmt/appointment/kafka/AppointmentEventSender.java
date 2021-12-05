package de.apnmt.appointment.kafka;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.AppointmentEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppointmentEventSender implements ApnmtEventSender<AppointmentEventDTO> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AppointmentEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, ApnmtEvent<AppointmentEventDTO> event) {
        this.kafkaTemplate.send(topic, event);
    }

}
