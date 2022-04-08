package de.apnmt.appointment.kafka;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.ServiceEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceEventSender implements ApnmtEventSender<ServiceEventDTO> {

    private static final Logger log = LoggerFactory.getLogger(ServiceEventSender.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ServiceEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, ApnmtEvent<ServiceEventDTO> event) {
        log.info("Send event {} to topic {}", event, topic);
        this.kafkaTemplate.send(topic, event);
    }

}
