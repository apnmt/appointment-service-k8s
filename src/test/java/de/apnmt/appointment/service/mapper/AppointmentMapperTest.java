package de.apnmt.appointment.service.mapper;

import de.apnmt.appointment.common.service.mapper.AppointmentMapper;
import de.apnmt.appointment.common.service.mapper.AppointmentMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class AppointmentMapperTest {

    private AppointmentMapper appointmentMapper;

    @BeforeEach
    public void setUp() {
        this.appointmentMapper = new AppointmentMapperImpl();
    }
}
