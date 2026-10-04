package tech.mamxanh.expertapplication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;

/** Exercises the conditional PENDING transition with two independent real SQL Server transactions. */
class ExpertApplicationConcurrencyIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ExpertApplicationService service;

    @Test void onlyOneConcurrentAdminDecisionWinsAndWritesOneNotification() throws Exception {
        String suffix = UUID.randomUUID().toString();
        long applicant = user("applicant-" + suffix + "@example.org", "CUSTOMER");
        long admin = user("admin-" + suffix + "@example.org", "ADMIN");
        long application = jdbc.queryForObject("INSERT INTO [EXPERT_APPLICATION](user_id,bio_experience,vegetarian_type,sample_recipe_summary) OUTPUT INSERTED.application_id VALUES(?,?,?,?)",
                Long.class, applicant, "Kinh nghiệm nấu các món chay nhiều năm.", "VEGAN", "Công thức rau củ hấp với hướng dẫn đầy đủ.");
        var start = new CountDownLatch(1);
        try {
            try (var pool = Executors.newFixedThreadPool(2)) {
                var first = pool.submit(() -> decide(start, admin, application));
                var second = pool.submit(() -> decide(start, admin, application));
                start.countDown();
                List<String> results = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
                assertThat(results).containsExactlyInAnyOrder("won", "conflict");
            }
            assertThat(jdbc.queryForObject("SELECT role FROM [USER] WHERE user_id=?", String.class, applicant)).isEqualTo("EXPERT");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [NOTIFICATION] WHERE user_id=?", Integer.class, applicant)).isEqualTo(1);
        } finally {
            SecurityContextHolder.clearContext();
            jdbc.update("DELETE FROM [NOTIFICATION] WHERE user_id=?", applicant);
            jdbc.update("DELETE FROM [EXPERT_APPLICATION] WHERE application_id=?", application);
            jdbc.update("DELETE FROM [USER] WHERE user_id IN (?,?)", applicant, admin);
        }
    }

    private String decide(CountDownLatch start, long admin, long application) {
        try {
            start.await();
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(admin + "", "test", List.of()));
            SecurityContextHolder.setContext(context);
            service.approve(application, null);
            return "won";
        } catch (ApiException exception) {
            if (exception.getStatus() == org.springframework.http.HttpStatus.CONFLICT) return "conflict";
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); throw new RuntimeException(exception);
        } finally { SecurityContextHolder.clearContext(); }
    }

    private long user(String email, String role) {
        return jdbc.queryForObject("INSERT INTO [USER](email,display_name,role,account_status,email_verified,created_at,updated_at) OUTPUT INSERTED.user_id VALUES(?,?,?,'ACTIVE',1,SYSUTCDATETIME(),SYSUTCDATETIME())",
                Long.class, email, "Issue 68 Test", role);
    }
}
