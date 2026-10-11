package tech.mamxanh.nutrition.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;

/** FR-38 eligibility and FR-35 guard against a real SQL Server with Flyway applied. */
class NutritionEligibilityIntegrationTest extends AbstractIntegrationTest {
    private static final String BASE = "/api/v1/nutrition/profile";
    private static final String DOMAIN = "%@fr38.test";
    private static final String PASSWORD = "MatKhau123";

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE ?", DOMAIN);
    }

    @Test
    void newMemberIsNotConfirmedAndCannotReadOrWriteNutritionProfileUntilEligible() throws Exception {
        String token = memberToken("new@fr38.test");

        mockMvc.perform(get(BASE + "/eligibility").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").doesNotExist());
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED"));
        saveProfile(token, profileBody()).andExpect(status().isForbidden());
        assertThat(eligibilityRow("new@fr38.test")).containsEntry("nutrition_eligibility_status", "NOT_CONFIRMED")
                .containsEntry("nutrition_eligibility_confirmed_at", null)
                .containsEntry("health_data_consent", false);
    }

    @Test
    void confirmsEligibleAndKeepsEligibilityTimestampSeparateFromHealthDataConsent() throws Exception {
        String token = memberToken("eligible@fr38.test");

        updateEligibility(token, "ELIGIBLE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ELIGIBLE"))
                .andExpect(jsonPath("$.confirmedAt").exists());
        saveProfile(token, profileBody()).andExpect(status().isOk());

        Map<String, Object> row = eligibilityRow("eligible@fr38.test");
        assertThat(row.get("nutrition_eligibility_status")).isEqualTo("ELIGIBLE");
        assertThat(row.get("nutrition_eligibility_confirmed_at")).isNotNull();
        assertThat(row.get("health_data_consent")).isEqualTo(true);
        assertThat(row.get("health_data_consent_at")).isNotNull();

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.dateOfBirth").value("1990-01-01"))
                .andExpect(jsonPath("$.results").doesNotExist());
    }

    @Test
    void ineligibleBlocksProfileAndCalculationButPreservesDataAndReconfirmationRestoresReadAccess() throws Exception {
        String token = memberToken("changed@fr38.test");
        updateEligibility(token, "ELIGIBLE").andExpect(status().isOk());
        saveProfile(token, profileBody()).andExpect(status().isOk());
        Map<String, Object> before = profileData("changed@fr38.test");

        updateEligibility(token, "INELIGIBLE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INELIGIBLE"));
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NUTRITION_ELIGIBILITY_INELIGIBLE"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(BASE + "/calculate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pregnant\":false,\"breastfeeding\":false,\"therapeuticDietRequired\":false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NUTRITION_ELIGIBILITY_INELIGIBLE"));
        assertThat(profileData("changed@fr38.test")).isEqualTo(before);

        updateEligibility(token, "ELIGIBLE").andExpect(status().isOk());
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.dateOfBirth").value("1990-01-01"))
                .andExpect(jsonPath("$.results").doesNotExist());
        assertThat(profileData("changed@fr38.test")).isEqualTo(before);
    }

    @Test
    void invalidConfirmationDoesNotChangePreviouslyConfirmedStateOrTimestamp() throws Exception {
        String token = memberToken("invalid@fr38.test");
        updateEligibility(token, "ELIGIBLE").andExpect(status().isOk());
        Map<String, Object> before = eligibilityRow("invalid@fr38.test");

        updateEligibility(token, "NOT_CONFIRMED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(eligibilityRow("invalid@fr38.test")).isEqualTo(before);
    }

    @Test
    void guestCannotReadOrChangeEligibility() throws Exception {
        mockMvc.perform(get(BASE + "/eligibility")).andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE + "/eligibility").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ELIGIBLE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void runtimeOpenApiDocumentsEligibilityRequestAndGuardResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/nutrition/profile/eligibility'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/nutrition/profile/eligibility'].get.responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/NutritionEligibilityResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/nutrition/profile/eligibility'].put.requestBody.content['application/json'].schema.$ref")
                        .value("#/components/schemas/UpdateNutritionEligibilityRequest"))
                .andExpect(jsonPath("$.components.schemas.UpdateNutritionEligibilityRequest.properties.status.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.UpdateNutritionEligibilityRequest.properties.status.enum[0]")
                        .value("ELIGIBLE"))
                .andExpect(jsonPath("$.components.schemas.UpdateNutritionEligibilityRequest.properties.status.enum[1]")
                        .value("INELIGIBLE"))
                .andExpect(jsonPath("$.paths['/api/v1/nutrition/profile/eligibility'].put.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/nutrition/profile/eligibility'].put.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/nutrition/profile/eligibility'].put.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/nutrition/profile'].get.responses['403']").exists());
    }

    private ResultActions updateEligibility(String token, String status) throws Exception {
        return mockMvc.perform(put(BASE + "/eligibility").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private ResultActions saveProfile(String token, String body) throws Exception {
        return mockMvc.perform(put(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String profileBody() {
        return """
                {"dateOfBirth":"1990-01-01","biologicalSex":"FEMALE","heightCm":170,"weightKg":65,
                 "activityLevel":"SEDENTARY","nutritionGoal":"MAINTAIN_WEIGHT","pregnant":false,
                 "breastfeeding":false,"therapeuticDietRequired":false,"consentAccepted":true}
                """;
    }

    private Map<String, Object> eligibilityRow(String email) {
        return jdbcTemplate.queryForMap("""
                SELECT nutrition_eligibility_status, nutrition_eligibility_confirmed_at,
                       health_data_consent, health_data_consent_at
                FROM [USER] WHERE email = ?
                """, email);
    }

    private Map<String, Object> profileData(String email) {
        return jdbcTemplate.queryForMap("""
                SELECT date_of_birth, biological_sex, height_cm, weight_kg, activity_level, nutrition_goal,
                       health_data_consent, health_data_consent_at
                FROM [USER] WHERE email = ?
                """, email);
    }

    private String memberToken(String email) throws Exception {
        User user = User.registerWithPassword(email, passwordEncoder.encode(PASSWORD), "Thành viên FR38", clock.instant().atZone(java.time.ZoneOffset.UTC).toLocalDateTime());
        user.markEmailVerified(clock.instant().atZone(java.time.ZoneOffset.UTC).toLocalDateTime());
        userRepository.saveAndFlush(user);
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.accessToken");
    }

    private static String bearer(String token) { return "Bearer " + token; }
}
