package de.apnmt.appointment.config;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.ServiceEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncEventSenderConfiguration {

    private final Logger log = LoggerFactory.getLogger(AsyncEventSenderConfiguration.class);

    @Bean
    public ApnmtEventSender<ServiceEventDTO> serviceEventSender() {
        // TODO replace with real implementation
        return new ApnmtEventSender<ServiceEventDTO>() {
            @Override
            public void send(String topic, ApnmtEvent<ServiceEventDTO> event) {
                AsyncEventSenderConfiguration.this.log.info("Event send to topic {} with message {}", topic, event);
            }
        };
    }

}
