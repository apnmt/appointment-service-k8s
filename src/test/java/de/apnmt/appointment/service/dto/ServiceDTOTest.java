package de.apnmt.appointment.service.dto;

import de.apnmt.appointment.common.service.dto.ServiceDTO;
import de.apnmt.appointment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ServiceDTO.class);
        ServiceDTO serviceDTO1 = new ServiceDTO();
        serviceDTO1.setId(1L);
        ServiceDTO serviceDTO2 = new ServiceDTO();
        assertThat(serviceDTO1).isNotEqualTo(serviceDTO2);
        serviceDTO2.setId(serviceDTO1.getId());
        assertThat(serviceDTO1).isEqualTo(serviceDTO2);
        serviceDTO2.setId(2L);
        assertThat(serviceDTO1).isNotEqualTo(serviceDTO2);
        serviceDTO1.setId(null);
        assertThat(serviceDTO1).isNotEqualTo(serviceDTO2);
    }
}
