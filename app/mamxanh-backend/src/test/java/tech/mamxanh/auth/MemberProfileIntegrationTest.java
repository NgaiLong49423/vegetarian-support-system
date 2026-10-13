package tech.mamxanh.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.unit.DataSize;
import tech.mamxanh.AbstractIntegrationTest;

/** FR-23 (Issue #29), decisions Q54–Q58: public member profile, own profile settings and avatar. */
class MemberProfileIntegrationTest extends AbstractIntegrationTest {
    private static final String PREFIX = "issue29-";

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MultipartProperties multipartProperties;

    private long authorId;
    private long lockedMemberId;
    private long customerId;
    private long adminId;
    private long olderRecipeId;
    private long newerRecipeId;

    @BeforeEach
    void prepareDatabaseFixtures() {
        deleteFixtures();

        // 2026-08-31 18:30 UTC is 2026-09-01 01:30 in Vietnam: the join month follows Vietnam time.
        authorId = insertUser("author", "Tác giả 29", "EXPERT", "ACTIVE", "Thích nấu món chay miền Tây.", "2026-08-31T18:30:00");
        lockedMemberId = insertUser("locked", "Thành viên bị khóa", "EXPERT", "LOCKED", null, "2026-07-10T03:00:00");
        customerId = insertUser("customer", "Khách 29", "CUSTOMER", "ACTIVE", null, "2026-10-02T03:00:00");
        adminId = insertUser("admin", "Quản trị 29", "ADMIN", "ACTIVE", null, "2026-06-01T03:00:00");
        olderRecipeId = insertRecipe(authorId, "Canh chua chay", "PUBLISHED", "2026-09-05T03:00:00");
        newerRecipeId = insertRecipe(authorId, "Đậu hũ kho tiêu", "PUBLISHED", "2026-09-20T03:00:00");
        insertRecipe(authorId, "Món đang bị ẩn", "HIDDEN", "2026-09-25T03:00:00");
        insertRecipe(authorId, "Món đã xóa", "DELETED", "2026-09-26T03:00:00");
        insertRecipe(lockedMemberId, "Bún chay của thành viên bị khóa", "PUBLISHED", "2026-08-01T03:00:00");
    }

    /** Other integration classes delete every user, so no recipe may outlive this class (see BUG-014). */
    @AfterEach
    void deleteFixtures() {
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue29-%@test.local')");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE 'issue29-%@test.local'");
    }

    @Test
    void publicProfileShowsOnlyPublicFieldsAndTheJoinMonthInVietnamTime() throws Exception {
        mockMvc.perform(get("/api/v1/members/{userId}", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(authorId))
                .andExpect(jsonPath("$.displayName").value("Tác giả 29"))
                .andExpect(jsonPath("$.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.bio").value("Thích nấu món chay miền Tây."))
                .andExpect(jsonPath("$.joinedMonth").value("2026-09"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.accountStatus").doesNotExist());
    }

    @Test
    void lockedMembersKeepTheirPublicProfileButAdministratorsAndUnknownIdsAreNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/members/{userId}", lockedMemberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Thành viên bị khóa"));
        mockMvc.perform(get("/api/v1/members/{userId}/recipes", lockedMemberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        mockMvc.perform(get("/api/v1/members/{userId}", adminId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_PROFILE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/members/{userId}", adminId + 1_000_000))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_PROFILE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/members/{userId}/recipes", adminId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_PROFILE_NOT_FOUND"));
    }

    @Test
    void memberRecipeListShowsOnlyPublishedPostsNewestFirstWithBoundedPages() throws Exception {
        mockMvc.perform(get("/api/v1/members/{userId}/recipes", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(newerRecipeId))
                .andExpect(jsonPath("$.items[1].id").value(olderRecipeId))
                .andExpect(jsonPath("$.items[0].authorName").value("Tác giả 29"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/members/{userId}/recipes", authorId).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(olderRecipeId))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/v1/members/{userId}/recipes", authorId).param("size", "51"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/members/{userId}/recipes", authorId).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/members/{userId}/recipes", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void ownProfileRequiresAnActiveMemberSession() throws Exception {
        mockMvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/profile").with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(customerId))
                .andExpect(jsonPath("$.displayName").value("Khách 29"))
                .andExpect(jsonPath("$.joinedMonth").value("2026-10"));
        mockMvc.perform(get("/api/v1/me/profile").with(principal(adminId, "ROLE_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_ACCESS_REQUIRED"));
    }

    @Test
    void ownerUpdatesTrimmedNameAndBioAndEveryPublicViewShowsThemImmediately() throws Exception {
        mockMvc.perform(put("/api/v1/me/profile").with(principal(authorId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"  Tuệ Tâm Bếp Chay  \",\"bio\":\"  Nấu chay mỗi ngày.  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Tuệ Tâm Bếp Chay"))
                .andExpect(jsonPath("$.bio").value("Nấu chay mỗi ngày."));
        assertThat(column(authorId, "display_name")).isEqualTo("Tuệ Tâm Bếp Chay");
        assertThat(column(authorId, "bio")).isEqualTo("Nấu chay mỗi ngày.");

        mockMvc.perform(get("/api/v1/members/{userId}", authorId))
                .andExpect(jsonPath("$.displayName").value("Tuệ Tâm Bếp Chay"));
        mockMvc.perform(get("/api/v1/recipes/{recipeId}", newerRecipeId))
                .andExpect(jsonPath("$.author.displayName").value("Tuệ Tâm Bếp Chay"));

        mockMvc.perform(put("/api/v1/me/profile").with(principal(authorId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Tuệ Tâm Bếp Chay\",\"bio\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").doesNotExist());
        assertThat(column(authorId, "bio")).isNull();
    }

    @Test
    void invalidNamesAndBiosAreRejectedWithoutChangingTheProfile() throws Exception {
        String longBio = "a".repeat(501);
        for (String body : new String[] {
                "{\"displayName\":\"  ab  \",\"bio\":null}",
                "{\"displayName\":\"" + "x".repeat(51) + "\",\"bio\":null}",
                "{\"displayName\":\"Tên\\u0007lạ\",\"bio\":null}",
                "{\"displayName\":null,\"bio\":null}",
                "{\"displayName\":\"Tên hợp lệ\",\"bio\":\"" + longBio + "\"}"}) {
            mockMvc.perform(put("/api/v1/me/profile").with(principal(authorId, "ROLE_EXPERT"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        mockMvc.perform(put("/api/v1/me/profile").with(principal(authorId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"ab\",\"bio\":null}"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("displayName")))
                .andExpect(jsonPath("$.errors[0].message", containsString("3 đến 50")));
        assertThat(column(authorId, "display_name")).isEqualTo("Tác giả 29");
        assertThat(column(authorId, "bio")).isEqualTo("Thích nấu món chay miền Tây.");

        mockMvc.perform(put("/api/v1/me/profile").with(principal(adminId, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Quản trị mới\",\"bio\":null}"))
                .andExpect(status().isForbidden());
        assertThat(column(adminId, "display_name")).isEqualTo("Quản trị 29");
    }

    @Test
    void ownerUploadsAValidAvatarUpToTwoMegabytes() throws Exception {
        byte[] png = withSignature(new byte[1_500_000], 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
        mockMvc.perform(multipart("/api/v1/me/profile/avatar").file(new MockMultipartFile("file", "me.png", "image/png", png))
                        .with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", containsString("/avatars/")));
        assertThat(column(customerId, "avatar_url")).contains("/avatars/").endsWith(".png");

        byte[] webp = withSignature(new byte[2 * 1024 * 1024], 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P');
        mockMvc.perform(multipart("/api/v1/me/profile/avatar").file(new MockMultipartFile("file", "me.webp", "image/webp", webp))
                        .with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", containsString(".webp")));
        mockMvc.perform(get("/api/v1/members/{userId}", customerId))
                .andExpect(jsonPath("$.avatarUrl", containsString(".webp")));
    }

    @Test
    void invalidAvatarsAreRejectedAndTheOldAvatarStays() throws Exception {
        jdbcTemplate.update("UPDATE [USER] SET avatar_url = 'https://cdn.test/old.png' WHERE user_id = ?", customerId);
        byte[] tooLarge = withSignature(new byte[2 * 1024 * 1024 + 1], 0xFF, 0xD8, 0xFF);
        mockMvc.perform(multipart("/api/v1/me/profile/avatar").file(new MockMultipartFile("file", "big.jpg", "image/jpeg", tooLarge))
                        .with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
        MockMultipartFile[] rejected = {
                new MockMultipartFile("file", "fake.png", "image/png", "not really an image".getBytes()),
                new MockMultipartFile("file", "anim.gif", "image/gif", withSignature(new byte[64], 'G', 'I', 'F', '8', '9', 'a')),
                new MockMultipartFile("file", "photo.png", "image/png", withSignature(new byte[64], 0xFF, 0xD8, 0xFF)),
        };
        for (MockMultipartFile file : rejected) {
            mockMvc.perform(multipart("/api/v1/me/profile/avatar").file(file).with(principal(customerId, "ROLE_CUSTOMER")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE_TYPE"));
        }
        mockMvc.perform(multipart("/api/v1/me/profile/avatar").file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))
                        .with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/v1/me/profile/avatar").file(new MockMultipartFile("file", "a.png", "image/png",
                        withSignature(new byte[64], 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/v1/me/profile/avatar").file(new MockMultipartFile("file", "a.png", "image/png",
                        withSignature(new byte[64], 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)))
                        .with(principal(adminId, "ROLE_ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(column(customerId, "avatar_url")).isEqualTo("https://cdn.test/old.png");
    }

    @Test
    void servletMultipartLimitsLetTheFiveMegabyteRecipeImageAndTwoMegabyteAvatarThrough() {
        assertThat(multipartProperties.getMaxFileSize()).isGreaterThanOrEqualTo(DataSize.ofMegabytes(5));
        assertThat(multipartProperties.getMaxRequestSize()).isGreaterThanOrEqualTo(DataSize.ofMegabytes(5));
    }

    @Test
    void generatedOpenApiSeparatesPublicProfileReadsFromAuthenticatedProfileChanges() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/members/{userId}'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/members/{userId}/recipes'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/me/profile'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/me/profile'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/me/profile/avatar'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.components.schemas.MemberProfileResponse.properties.email").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.MemberProfileResponse.properties.role").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UpdateProfileRequest.properties.displayName.maxLength").value(50))
                .andExpect(jsonPath("$.components.schemas.UpdateProfileRequest.properties.bio.maxLength").value(500));
    }

    private static byte[] withSignature(byte[] content, int... signature) {
        for (int index = 0; index < signature.length; index++) {
            content[index] = (byte) signature[index];
        }
        return content;
    }

    private static RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private String column(long userId, String name) {
        return jdbcTemplate.queryForObject("SELECT " + name + " FROM [USER] WHERE user_id = ?", String.class, userId);
    }

    private long insertUser(String key, String displayName, String role, String accountStatus, String bio, String createdAt) {
        String email = PREFIX + key + "@test.local";
        jdbcTemplate.update("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified, bio, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, ?, ?, ?)
                """, email, displayName, role, accountStatus, bio, createdAt, createdAt);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private long insertRecipe(long recipeAuthorId, String title, String recipeStatus, String publishedAt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, ?, ?)
                """, Long.class, recipeAuthorId, title, recipeStatus, publishedAt);
    }
}
