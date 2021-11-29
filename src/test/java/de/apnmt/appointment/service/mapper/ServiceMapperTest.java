package de.apnmt.appointment.service.mapper;

import de.apnmt.appointment.common.service.mapper.ServiceMapper;
import de.apnmt.appointment.common.service.mapper.ServiceMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class ServiceMapperTest {

    private ServiceMapper serviceMapper;

    @BeforeEach
    public void setUp() {
        this.serviceMapper = new ServiceMapperImpl();
    }
}
