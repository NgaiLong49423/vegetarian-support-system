package tech.mamxanh.nutrition.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.nutrition.service.DietaryRequirement;

/**
 * FR-31 (#36): dietary preferences, Onboarding and the personalized-AI gate against a real SQL
 * Server (Flyway V1→V7). Accounts use the {@code @fr31.test} domain so cleanup never touches data
 * created by other test classes.
 */
class DietaryPreferenceIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/nutrition/dietary-preferences";
    private static final String PASSWORD = "MatKhau123";
    private static final String PEANUT = "Đậu phộng";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DietaryPreferenceService dietaryPreferenceService;

    private Long insertedPeanutId;

    @BeforeEach
    void prepare() {
        cleanUp();
        insertedPeanutId = null;
        Long existing = jdbcTemplate.query("SELECT ingredient_id FROM INGREDIENT WHERE name = ?",
                rs -> rs.next() ? rs.getLong(1) : null, PEANUT);
        if (existing == null) {
            jdbcTemplate.update("INSERT INTO INGREDIENT (name, source_name, reference_date) VALUES (?, N'Dữ liệu kiểm thử', '2026-10-05')",
                    PEANUT);
            insertedPeanutId = jdbcTemplate.queryForObject("SELECT ingredient_id FROM INGREDIENT WHERE name = ?", Long.class, PEANUT);
        }
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE p FROM USER_INGREDIENT_PREFERENCE p JOIN [USER] u ON u.user_id = p.user_id WHERE u.email LIKE '%@fr31.test'");
        jdbcTemplate.update("DELETE m FROM MEAL_PLAN m JOIN [USER] u ON u.user_id = m.user_id WHERE u.email LIKE '%@fr31.test'");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE '%@fr31.test'");
        if (insertedPeanutId != null) {
            jdbcTemplate.update("DELETE FROM INGREDIENT WHERE ingredient_id = ?", insertedPeanutId);
            insertedPeanutId = null;
        }
    }

    // ---------------------------------------------------------------- read / initial state

    @Test
    void aNewMemberHasNotStartedOnboardingAndMissesAllThreeRequirements() throws Exception {
        String token = memberToken("new@fr31.test");

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingStatus").value("NOT_STARTED"))
                .andExpect(jsonPath("$.vegetarianType").doesNotExist())
                .andExpect(jsonPath("$.avoid.noneConfirmed").value(false))
                .andExpect(jsonPath("$.avoid.items").isEmpty())
                .andExpect(jsonPath("$.dislike.noneConfirmed").value(false))
                .andExpect(jsonPath("$.aiPersonalization.eligible").value(false))
                .andExpect(jsonPath("$.aiPersonalization.missing[0]").value("VEGETARIAN_TYPE"))
                .andExpect(jsonPath("$.aiPersonalization.missing[1]").value("AVOID_INGREDIENTS"))
                .andExpect(jsonPath("$.aiPersonalization.missing[2]").value("DISLIKED_INGREDIENTS"));
    }

    // ---------------------------------------------------------------- AC-31.1, AC-31.2

    @Test
    void completingOnboardingWithSpecificIngredientsStoresTheProfile() throws Exception {
        String token = memberToken("an@fr31.test");
        long peanutId = jdbcTemplate.queryForObject("SELECT ingredient_id FROM INGREDIENT WHERE name = ?", Long.class, PEANUT);

        save(token, """
                {"vegetarianType":"VEGAN",
                 "avoid":{"noneConfirmed":false,"items":["đậu phộng"]},
                 "dislike":{"noneConfirmed":false,"items":["Mướp đắng"]}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vegetarianType").value("VEGAN"))
                .andExpect(jsonPath("$.onboardingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.avoid.items[0].name").value(PEANUT))
                .andExpect(jsonPath("$.avoid.items[0].ingredientId").value(peanutId))
                .andExpect(jsonPath("$.dislike.items[0].name").value("Mướp đắng"))
                .andExpect(jsonPath("$.dislike.items[0].ingredientId").doesNotExist())
                .andExpect(jsonPath("$.aiPersonalization.eligible").value(true))
                .andExpect(jsonPath("$.aiPersonalization.missing").isEmpty());

        assertThat(userRow("an@fr31.test")).containsEntry("vegetarian_type", "VEGAN")
                .containsEntry("onboarding_status", "COMPLETED")
                .containsEntry("avoid_none_confirmed", false)
                .containsEntry("dislike_none_confirmed", false);
        assertThat(preferenceRows("an@fr31.test")).containsExactly(
                "AVOID|" + peanutId + "|" + PEANUT, "DISLIKE|null|Mướp đắng");
    }

    @Test
    void confirmingNoneForBothListsMakesTheProfileEligible() throws Exception {
        String token = memberToken("binh@fr31.test");

        save(token, """
                {"vegetarianType":"LACTO_OVO",
                 "avoid":{"noneConfirmed":true,"items":[]},
                 "dislike":{"noneConfirmed":true}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avoid.noneConfirmed").value(true))
                .andExpect(jsonPath("$.dislike.noneConfirmed").value(true))
                .andExpect(jsonPath("$.onboardingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.aiPersonalization.eligible").value(true));

        assertThat(userRow("binh@fr31.test")).containsEntry("avoid_none_confirmed", true)
                .containsEntry("dislike_none_confirmed", true);
        assertThat(preferenceRows("binh@fr31.test")).isEmpty();
    }

    @Test
    void optionalPreferencesAreStoredAndReturned() throws Exception {
        String token = memberToken("chi@fr31.test");

        save(token, """
                {"vegetarianType":"OVO","avoid":{"noneConfirmed":true},"dislike":{"noneConfirmed":true},
                 "cuisinePreference":"Món chay truyền thống Việt Nam","maxCookingTimeMinutes":30,
                 "preferredDifficulty":"EASY"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuisinePreference").value("Món chay truyền thống Việt Nam"))
                .andExpect(jsonPath("$.maxCookingTimeMinutes").value(30))
                .andExpect(jsonPath("$.preferredDifficulty").value("EASY"));

        assertThat(userRow("chi@fr31.test")).containsEntry("cuisine_preference", "Món chay truyền thống Việt Nam")
                .containsEntry("max_cooking_time_min", 30)
                .containsEntry("preferred_difficulty", "EASY");
    }

    // ---------------------------------------------------------------- AC-31.3, AC-31.10 (Skip)

    @Test
    void skippingRecordsSkippedOnceAndLeavesTheProfileIncomplete() throws Exception {
        String token = memberToken("dung@fr31.test");

        mockMvc.perform(post(BASE + "/onboarding/skip").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(BASE + "/onboarding/skip").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingStatus").value("SKIPPED"))
                .andExpect(jsonPath("$.aiPersonalization.eligible").value(false));
        assertThat(userRow("dung@fr31.test")).containsEntry("onboarding_status", "SKIPPED");
    }

    @Test
    void skippingAfterCompletingKeepsTheCompletedStatus() throws Exception {
        String token = memberToken("em@fr31.test");
        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/onboarding/skip").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(userRow("em@fr31.test")).containsEntry("onboarding_status", "COMPLETED");
    }

    @Test
    void theOnboardingMigrationStopsInvitingExistingAccountsOnly() throws Exception {
        createMember("cu@fr31.test", "CUSTOMER");
        String done = memberToken("xong@fr31.test");
        save(done, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk());
        String migration = new ClassPathResource("db/migration/V7__onboarding_existing_accounts.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        jdbcTemplate.execute(migration);

        assertThat(userRow("cu@fr31.test")).containsEntry("onboarding_status", "SKIPPED");
        assertThat(userRow("xong@fr31.test")).containsEntry("onboarding_status", "COMPLETED");
        createMember("moi@fr31.test", "CUSTOMER");
        assertThat(userRow("moi@fr31.test")).containsEntry("onboarding_status", "NOT_STARTED");
    }

    // ---------------------------------------------------------------- AC-31.7 and validation

    @Test
    void anEmptyAvoidListWithoutConfirmationIsRejectedAndNothingChanges() throws Exception {
        String token = memberToken("giang@fr31.test");
        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":false,\"items\":[\"Nấm\"]},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk());

        save(token, "{\"vegetarianType\":\"LACTO\",\"avoid\":{\"noneConfirmed\":false,\"items\":[]},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("avoid"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Chọn ít nhất một nguyên liệu cần tránh hoặc xác nhận không có dị ứng/kiêng cử."));

        assertThat(userRow("giang@fr31.test")).containsEntry("vegetarian_type", "VEGAN");
        assertThat(preferenceRows("giang@fr31.test")).containsExactly("AVOID|null|Nấm");
    }

    @Test
    void anEmptyDislikeListWithoutConfirmationIsRejected() throws Exception {
        String token = memberToken("ha@fr31.test");

        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":false}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("dislike"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Chọn ít nhất một món hoặc nguyên liệu không thích hoặc xác nhận không có."));
    }

    @Test
    void theVegetarianTypeAndBothListsAreRequired() throws Exception {
        String token = memberToken("hai@fr31.test");

        save(token, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='vegetarianType')].message").value("Chọn một loại ăn chay."))
                .andExpect(jsonPath("$.errors[?(@.field=='avoid')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='dislike')]").exists());
    }

    @Test
    void anUnknownVegetarianTypeIsRejected() throws Exception {
        String token = memberToken("hoa@fr31.test");

        save(token, "{\"vegetarianType\":\"PESCATARIAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void optionalFieldsAndListItemsAreBounded() throws Exception {
        String token = memberToken("khanh@fr31.test");
        String longName = "x".repeat(201);
        String tooMany = String.join(",", java.util.stream.IntStream.rangeClosed(1, 31).mapToObj(i -> "\"Món " + i + "\"").toList());

        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true},\"maxCookingTimeMinutes\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("maxCookingTimeMinutes"));
        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true},\"maxCookingTimeMinutes\":1441}")
                .andExpect(status().isBadRequest());
        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true},\"cuisinePreference\":\"" + longName + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cuisinePreference"));
        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"items\":[\"" + longName + "\"]},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isBadRequest());
        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"items\":[\"   \"]},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isBadRequest());
        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"items\":[" + tooMany + "]},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void aNameMadeOnlyOfNonBreakingSpacesIsRejectedAsBlank() throws Exception {
        String token = memberToken("nbsp@fr31.test");

        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"items\":[\"\u00a0\u00a0\"]},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(preferenceRows("nbsp@fr31.test")).isEmpty();
        assertThat(userRow("nbsp@fr31.test")).containsEntry("onboarding_status", "NOT_STARTED");
    }

    @Test
    void theSameIngredientCannotBeAvoidedAndDisliked() throws Exception {
        String token = memberToken("lan@fr31.test");

        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"items\":[\"Nấm\"]},\"dislike\":{\"items\":[\" nấm \"]}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INGREDIENT_PREFERENCE_CONFLICT"));
        assertThat(preferenceRows("lan@fr31.test")).isEmpty();
    }

    @Test
    void duplicatesInsideOneListAreMergedAndAddingItemsClearsTheNoneConfirmation() throws Exception {
        String token = memberToken("long@fr31.test");
        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk());

        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true,\"items\":[\"Nấm\",\"  nấm  \",\"NẤM\",\"Đậu   nành\"]},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avoid.noneConfirmed").value(false))
                .andExpect(jsonPath("$.avoid.items.length()").value(2));

        assertThat(userRow("long@fr31.test")).containsEntry("avoid_none_confirmed", false);
        assertThat(preferenceRows("long@fr31.test")).containsExactly("AVOID|null|Nấm", "AVOID|null|Đậu nành");

        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk());
        assertThat(preferenceRows("long@fr31.test")).isEmpty();
    }

    // ---------------------------------------------------------------- AC-31.4–AC-31.6 (AI gate)

    @Test
    void thePersonalizedAiGateNamesEveryMissingRequirement() throws Exception {
        String token = memberToken("minh@fr31.test");
        long userId = userId("minh@fr31.test");

        assertThat(missingFor(userId)).containsExactly(DietaryRequirement.VEGETARIAN_TYPE,
                DietaryRequirement.AVOID_INGREDIENTS, DietaryRequirement.DISLIKED_INGREDIENTS);

        jdbcTemplate.update("UPDATE [USER] SET vegetarian_type = 'VEGAN' WHERE user_id = ?", userId);
        assertThat(missingFor(userId)).containsExactly(DietaryRequirement.AVOID_INGREDIENTS,
                DietaryRequirement.DISLIKED_INGREDIENTS);

        jdbcTemplate.update("UPDATE [USER] SET avoid_none_confirmed = 1 WHERE user_id = ?", userId);
        assertThat(missingFor(userId)).containsExactly(DietaryRequirement.DISLIKED_INGREDIENTS);

        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"items\":[\"Rau mùi\"]}}")
                .andExpect(status().isOk());
        assertThatCode(() -> dietaryPreferenceService.requirePersonalizedAiEligible(userId)).doesNotThrowAnyException();
    }

    @Test
    void theGateBlocksWithAConflictThatListsTheMissingInformation() throws Exception {
        memberToken("my@fr31.test");

        AppException blocked = catchThrowableOfType(AppException.class,
                () -> dietaryPreferenceService.requirePersonalizedAiEligible(userId("my@fr31.test")));

        assertThat(blocked.errorCode()).isEqualTo(ErrorCode.DIETARY_PROFILE_INCOMPLETE);
        assertThat(blocked.errorCode().status().value()).isEqualTo(409);
        assertThat(blocked.properties()).containsEntry("missing",
                List.of("VEGETARIAN_TYPE", "AVOID_INGREDIENTS", "DISLIKED_INGREDIENTS"));
    }

    // ---------------------------------------------------------------- AC-31.8

    @Test
    void changingTheProfileDoesNotModifyAnExistingMealPlan() throws Exception {
        String token = memberToken("nam@fr31.test");
        save(token, "{\"vegetarianType\":\"LACTO\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk());
        long userId = userId("nam@fr31.test");
        jdbcTemplate.update("INSERT INTO MEAL_PLAN (user_id, week_start_date, created_at, updated_at) VALUES (?, ?, ?, ?)",
                userId, LocalDate.of(2026, 9, 28), LocalDateTime.of(2026, 9, 28, 1, 0), LocalDateTime.of(2026, 9, 28, 1, 0));

        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForList("SELECT week_start_date, updated_at FROM MEAL_PLAN WHERE user_id = ?", userId))
                .singleElement()
                .satisfies(row -> assertThat(row.get("updated_at").toString()).startsWith("2026-09-28 01:00"));
        assertThat(userRow("nam@fr31.test")).containsEntry("vegetarian_type", "VEGAN");
    }

    // ---------------------------------------------------------------- access and privacy (AC-31.9)

    @Test
    void guestsCannotReadOrChangePreferences() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/onboarding/skip")).andExpect(status().isUnauthorized());
    }

    @Test
    void administratorsAreNotMembersAndCannotUseThePreferenceProfile() throws Exception {
        createMember("admin@fr31.test", "ADMIN");
        String token = accessToken("admin@fr31.test");

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_ACCESS_REQUIRED"));
    }

    @Test
    void expertsCanUseTheirOwnPreferenceProfile() throws Exception {
        createMember("expert@fr31.test", "EXPERT");
        String token = accessToken("expert@fr31.test");

        save(token, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"noneConfirmed\":true},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingStatus").value("COMPLETED"));
    }

    @Test
    void eachMemberOnlySeesTheirOwnPreferences() throws Exception {
        String ownerToken = memberToken("owner@fr31.test");
        save(ownerToken, "{\"vegetarianType\":\"VEGAN\",\"avoid\":{\"items\":[\"Nấm\"]},\"dislike\":{\"noneConfirmed\":true}}")
                .andExpect(status().isOk());
        String otherToken = memberToken("other@fr31.test");

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vegetarianType").doesNotExist())
                .andExpect(jsonPath("$.avoid.items").isEmpty());
    }

    @Test
    void ingredientSuggestionsComeFromTheActiveStandardCatalog() throws Exception {
        String token = memberToken("goi-y@fr31.test");

        mockMvc.perform(get(BASE + "/ingredient-suggestions").param("query", "phộng")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(PEANUT))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(10)));
        mockMvc.perform(get(BASE + "/ingredient-suggestions").param("query", "   ")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions save(String token, String body) throws Exception {
        return mockMvc.perform(put(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<DietaryRequirement> missingFor(long userId) {
        AppException blocked = catchThrowableOfType(AppException.class,
                () -> dietaryPreferenceService.requirePersonalizedAiEligible(userId));
        assertThat(blocked).isNotNull();
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) blocked.properties().get("missing");
        return missing.stream().map(DietaryRequirement::valueOf).toList();
    }

    private String memberToken(String email) throws Exception {
        createMember(email, "CUSTOMER");
        return accessToken(email);
    }

    private void createMember(String email, String role) {
        User user = User.registerWithPassword(email, passwordEncoder.encode(PASSWORD), "Thành viên FR31",
                LocalDateTime.now(clock));
        user.markEmailVerified(LocalDateTime.now(clock));
        userRepository.saveAndFlush(user);
        jdbcTemplate.update("UPDATE [USER] SET role = ? WHERE email = ?", role, email);
    }

    private String accessToken(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.accessToken");
    }

    private long userId(String email) {
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private Map<String, Object> userRow(String email) {
        return jdbcTemplate.queryForMap("""
                SELECT vegetarian_type, cuisine_preference, preferred_difficulty, max_cooking_time_min,
                       onboarding_status, avoid_none_confirmed, dislike_none_confirmed
                FROM [USER] WHERE email = ?""", email);
    }

    private List<String> preferenceRows(String email) {
        return jdbcTemplate.queryForList("""
                SELECT CONCAT(p.preference_type, '|', COALESCE(CAST(p.ingredient_id AS VARCHAR(20)), 'null'), '|',
                              p.custom_ingredient_name)
                FROM USER_INGREDIENT_PREFERENCE p JOIN [USER] u ON u.user_id = p.user_id
                WHERE u.email = ? ORDER BY p.preference_id""", String.class, email);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
