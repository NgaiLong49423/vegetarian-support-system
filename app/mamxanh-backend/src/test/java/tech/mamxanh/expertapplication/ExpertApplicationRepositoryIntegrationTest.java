package tech.mamxanh.expertapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.repository.ExpertApplicationRepository;

class ExpertApplicationRepositoryIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ExpertApplicationRepository repository;
    private long applicant;
    private long otherApplicant;
    private long reviewer;

    @BeforeEach
    void createAccounts() {
        String token = UUID.randomUUID().toString();
        applicant = user("first-" + token + "@example.org", "CUSTOMER");
        otherApplicant = user("other-" + token + "@example.org", "CUSTOMER");
        reviewer = user("admin-" + token + "@example.org", "ADMIN");
    }

    @AfterEach
    void removeRecords() {
        jdbc.update("DELETE FROM [EXPERT_APPLICATION] WHERE user_id IN (?,?)", applicant, otherApplicant);
        jdbc.update("DELETE FROM [USER] WHERE user_id IN (?,?,?)", applicant, otherApplicant, reviewer);
    }

    @Test
    void insertTrimsTextAndConvertsBlankPortfolioToNull() {
        long id = repository.insert(applicant, request("   "), LocalDateTime.of(2026, 10, 4, 12, 0));

        var result = repository.findById(id);
        assertThat(result.experience()).isEqualTo("Experienced vegetarian cook");
        assertThat(result.sampleRecipeSummary()).isEqualTo("A vegetable dish with a detailed preparation summary.");
        assertThat(result.portfolioUrl()).isNull();
        assertThat(result.status()).isEqualTo("PENDING");
    }

    @Test
    void pagesAndCountsCanBeScopedByOwnerAndStatus() {
        long ownId = repository.insert(applicant, request(null), LocalDateTime.of(2026, 10, 4, 10, 0));
        long otherId = repository.insert(otherApplicant, request(null), LocalDateTime.of(2026, 10, 4, 11, 0));
        repository.transitionPending(otherId, "REJECTED", "Please provide more experience.", reviewer,
                LocalDateTime.of(2026, 10, 4, 12, 0));

        assertThat(repository.page(applicant, null, 0, 10)).extracting("id").containsExactly(ownId);
        assertThat(repository.page(null, "REJECTED", 0, 10)).extracting("id").containsExactly(otherId);
        assertThat(repository.count(applicant, null)).isEqualTo(1);
        assertThat(repository.count(null, "REJECTED")).isEqualTo(1);
    }

    @Test
    void onlyPendingApplicationCanTransitionAndMissingDetailReturnsNotFound() {
        long id = repository.insert(applicant, request(null), LocalDateTime.of(2026, 10, 4, 10, 0));
        var reviewedAt = LocalDateTime.of(2026, 10, 4, 12, 0);

        assertThat(repository.transitionPending(id, "APPROVED", null, reviewer, reviewedAt)).isEqualTo(1);
        assertThat(repository.transitionPending(id, "REJECTED", "Too late to change.", reviewer, reviewedAt)).isZero();
        assertThat(repository.findById(id).reviewedAt()).isEqualTo(reviewedAt);
        assertThatThrownBy(() -> repository.findById(Long.MAX_VALUE)).isInstanceOf(ApiException.class);
    }

    private long user(String email, String role) {
        return jdbc.queryForObject("INSERT INTO [USER](email,display_name,role,account_status,email_verified,created_at,updated_at) OUTPUT INSERTED.user_id VALUES(?,?,?,'ACTIVE',1,SYSUTCDATETIME(),SYSUTCDATETIME())",
                Long.class, email, "Issue 68 Repository Test", role);
    }

    private static ExpertApplicationRequest request(String portfolio) {
        return new ExpertApplicationRequest("  Experienced vegetarian cook  ", "VEGAN",
                "  A vegetable dish with a detailed preparation summary.  ", portfolio);
    }
}
