package tech.mamxanh.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tech.mamxanh.admin.dto.AdminExpertDetailResponse;
import tech.mamxanh.admin.dto.AdminUserResponse;
import tech.mamxanh.admin.repository.AdminUserQueryRepository;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;

@ExtendWith(MockitoExtension.class)
class AdminExpertServiceTest {
    @Mock private AdminUserQueryRepository users;
    @Mock private ExpertApplicationService applications;
    private AdminExpertService service;

    @BeforeEach
    void setUp() {
        service = new AdminExpertService(users, applications);
    }

    @Test
    void returnsProfileWithLatestApprovedApplication() {
        var profile = new AdminExpertDetailResponse(account(), "https://example.test/avatar.webp",
                "Chia sẻ món chay", null);
        var application = new ExpertApplicationResponse(91L, 42L, "Nguyễn An", "an@example.test",
                "Nhiều năm nấu món chay", "VEGAN", "Món ăn mẫu", null,
                false, false, "APPROVED", null,
                LocalDateTime.of(2026, 10, 20, 8, 0), LocalDateTime.of(2026, 10, 21, 8, 0));
        when(users.findExpertById(42L)).thenReturn(Optional.of(profile));
        when(applications.adminLatestApprovedForUser(42L)).thenReturn(application);

        var result = service.detail(42L);

        assertThat(result.account()).isEqualTo(account());
        assertThat(result.avatarUrl()).isEqualTo("https://example.test/avatar.webp");
        assertThat(result.bio()).isEqualTo("Chia sẻ món chay");
        assertThat(result.approvedApplication()).isSameAs(application);
        verify(applications).adminLatestApprovedForUser(42L);
    }

    @Test
    void missingExpertReturnsDomainNotFoundWithoutLoadingApplications() {
        when(users.findExpertById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(42L))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(error.getCode()).isEqualTo("EXPERT_NOT_FOUND");
                });

        verifyNoInteractions(applications);
    }

    private static AdminUserResponse account() {
        return new AdminUserResponse(42L, "Nguyễn An", "an@example.test", "EXPERT", "ACTIVE",
                LocalDateTime.of(2026, 10, 20, 8, 0), LocalDateTime.of(2026, 10, 21, 8, 0), 3L);
    }
}
