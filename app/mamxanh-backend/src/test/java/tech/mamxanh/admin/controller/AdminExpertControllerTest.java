package tech.mamxanh.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.admin.dto.AdminExpertDetailResponse;
import tech.mamxanh.admin.dto.AdminUserResponse;
import tech.mamxanh.admin.service.AdminExpertService;

@ExtendWith(MockitoExtension.class)
class AdminExpertControllerTest {
    @Mock private AdminExpertService service;
    private AdminExpertController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminExpertController(service);
    }

    @Test
    void delegatesExpertDetailLookup() {
        var response = new AdminExpertDetailResponse(
                new AdminUserResponse(42L, "Nguyễn An", "an@example.test", "EXPERT", "ACTIVE",
                        LocalDateTime.of(2026, 10, 20, 8, 0), null, 3L),
                null, null, null);
        when(service.detail(42L)).thenReturn(response);

        assertThat(controller.detail(42L)).isSameAs(response);
        verify(service).detail(42L);
    }
}
