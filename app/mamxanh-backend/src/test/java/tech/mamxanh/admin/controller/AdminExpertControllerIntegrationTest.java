package tech.mamxanh.admin.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.admin.repository.AdminUserQueryRepository;

class AdminExpertControllerIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL_PREFIX = "admin-expert-detail-";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private AdminUserQueryRepository users;
    private String emailSuffix;
    private long adminId;
    private long expertId;
    private long customerId;

    @BeforeEach
    void createAccounts() {
        emailSuffix = UUID.randomUUID() + "@test.local";
        adminId = insertUser("admin", "Quản trị viên", "ADMIN");
        expertId = insertUser("expert", "Nguyễn An", "EXPERT");
        customerId = insertUser("customer", "Thành viên Bình", "CUSTOMER");
    }

    @AfterEach
    void removeRecords() {
        jdbc.update("DELETE FROM [EXPERT_APPLICATION] WHERE user_id IN (?, ?, ?)", adminId, expertId, customerId);
        jdbc.update("DELETE FROM [USER] WHERE user_id IN (?, ?, ?)", adminId, expertId, customerId);
    }

    @Test
    void repositoryReturnsOnlyExpertAccounts() {
        var expert = users.findExpertById(expertId).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(expert.account().id()).isEqualTo(expertId);
        org.assertj.core.api.Assertions.assertThat(expert.account().displayName()).isEqualTo("Nguyễn An");
        org.assertj.core.api.Assertions.assertThat(expert.account().role()).isEqualTo("EXPERT");
        org.assertj.core.api.Assertions.assertThat(expert.approvedApplication()).isNull();
        org.assertj.core.api.Assertions.assertThat(users.findExpertById(customerId)).isEmpty();
        org.assertj.core.api.Assertions.assertThat(users.findExpertById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void administratorsReadExpertDetailAndMissingExpertGetsDomain404() throws Exception {
        asAdmin(get("/api/v1/admin/experts/{userId}", expertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.id").value(expertId))
                .andExpect(jsonPath("$.account.displayName").value("Nguyễn An"))
                .andExpect(jsonPath("$.account.role").value("EXPERT"))
                .andExpect(jsonPath("$.approvedApplication").value(nullValue()));

        asAdmin(get("/api/v1/admin/experts/{userId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXPERT_NOT_FOUND"));
    }

    @Test
    void expertDetailRequiresAdministratorRoleAndPositiveId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/experts/{userId}", expertId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mockMvc.perform(get("/api/v1/admin/experts/{userId}", expertId).with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        asAdmin(get("/api/v1/admin/experts/{userId}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private org.springframework.test.web.servlet.ResultActions asAdmin(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(principal(adminId, "ROLE_ADMIN")));
    }

    private RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private long insertUser(String prefix, String name, String role) {
        return jdbc.queryForObject("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
                OUTPUT INSERTED.user_id VALUES (?, ?, ?, 'ACTIVE', 1)
                """, Long.class, EMAIL_PREFIX + prefix + "-" + emailSuffix, name, role);
    }
}
