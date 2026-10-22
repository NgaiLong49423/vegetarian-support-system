package tech.mamxanh.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.service.MemberFollowService;

/** FR-59 member following (AC-59.1–AC-59.7, BR-75) against SQL Server. */
class MemberFollowIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL_PREFIX = "issue69-follow-";

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MemberFollowService followService;

    private long memberA;
    private long memberB;
    private long expertC;
    private long lockedD;
    private long adminE;

    @BeforeEach
    void prepareFixtures() {
        removeFixtures();
        memberA = insertUser("a", "CUSTOMER", "ACTIVE");
        memberB = insertUser("b", "CUSTOMER", "ACTIVE");
        expertC = insertUser("c", "EXPERT", "ACTIVE");
        lockedD = insertUser("d", "CUSTOMER", "LOCKED");
        adminE = insertUser("e", "ADMIN", "ACTIVE");
    }

    /** PasswordLoginIntegrationTest deletes every USER row, so no USER_FOLLOW row may outlive a test. */
    @AfterEach
    void removeFixtures() {
        jdbcTemplate.update("""
                DELETE FROM [USER_FOLLOW]
                WHERE follower_user_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)
                   OR followed_user_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)
                """, EMAIL_PREFIX + "%", EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE ?", EMAIL_PREFIX + "%");
    }

    @Test
    void followCreatesOneDirectedRelationAndIncrementsTheFollowerCount() throws Exception {
        mockMvc.perform(put("/api/v1/members/{userId}/follow", memberB).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(memberB))
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(0));
        assertThat(followRows(memberA, memberB)).isEqualTo(1);

        // AC-59.2: B does not follow A back automatically.
        assertThat(followRows(memberB, memberA)).isZero();
        mockMvc.perform(get("/api/v1/members/{userId}/follow", memberA).with(principal(memberB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(0))
                .andExpect(jsonPath("$.followingCount").value(1));
    }

    @Test
    void repeatedFollowKeepsOneRelationAndTheSameCount() throws Exception {
        mockMvc.perform(put("/api/v1/members/{userId}/follow", memberB).with(principal(memberA)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/members/{userId}/follow", memberB).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(1));
        assertThat(followRows(memberA, memberB)).isEqualTo(1);
    }

    @Test
    void parallelFollowRequestsForOnePairAllSucceedWithOneRelation() throws Exception {
        int requests = 8;
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(requests)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                results.add(pool.submit(() -> followAs(start, memberA, memberB)));
            }
            start.countDown();
            for (Future<Boolean> result : results) {
                assertThat(result.get(30, TimeUnit.SECONDS)).isTrue();
            }
        }
        assertThat(followRows(memberA, memberB)).isEqualTo(1);
    }

    @Test
    void unfollowIsIdempotentAndNeverDropsCountsBelowTheStoredRelations() throws Exception {
        followFixture(memberA, memberB, "2026-10-01T08:00:00");
        followFixture(expertC, memberB, "2026-10-02T08:00:00");

        mockMvc.perform(delete("/api/v1/members/{userId}/follow", memberB).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(1));
        mockMvc.perform(delete("/api/v1/members/{userId}/follow", memberB).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(1));
        assertThat(followRows(memberA, memberB)).isZero();
        assertThat(followRows(expertC, memberB)).isEqualTo(1);
    }

    @Test
    void selfFollowAndSelfUnfollowAreForbiddenWithoutWritingAnything() throws Exception {
        mockMvc.perform(put("/api/v1/members/{userId}/follow", memberA).with(principal(memberA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FOLLOW_SELF_NOT_ALLOWED"));
        mockMvc.perform(delete("/api/v1/members/{userId}/follow", memberA).with(principal(memberA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FOLLOW_SELF_NOT_ALLOWED"));
        assertThat(followRows(memberA, memberA)).isZero();
    }

    @Test
    void onlyAnAuthenticatedActiveMemberMayFollowOrUnfollow() throws Exception {
        mockMvc.perform(put("/api/v1/members/{userId}/follow", memberB))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/members/{userId}/follow", memberB))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/members/{userId}/follow", memberB).with(principal(adminE, "ROLE_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_ACCESS_REQUIRED"));
        mockMvc.perform(delete("/api/v1/members/{userId}/follow", memberB).with(principal(adminE, "ROLE_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_ACCESS_REQUIRED"));
        mockMvc.perform(put("/api/v1/members/{userId}/follow", memberB).with(principal(lockedD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_ACCESS_REQUIRED"));
        assertThat(followRows(adminE, memberB)).isZero();
        assertThat(followRows(lockedD, memberB)).isZero();
    }

    @Test
    void anExpertCanFollowAndBeFollowed() throws Exception {
        mockMvc.perform(put("/api/v1/members/{userId}/follow", memberA).with(principal(expertC, "ROLE_EXPERT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true));
        mockMvc.perform(put("/api/v1/members/{userId}/follow", expertC).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(1));
    }

    @Test
    void unknownOrAdministratorTargetsHaveNoProfileToFollow() throws Exception {
        mockMvc.perform(put("/api/v1/members/{userId}/follow", Long.MAX_VALUE).with(principal(memberA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_PROFILE_NOT_FOUND"));
        mockMvc.perform(put("/api/v1/members/{userId}/follow", adminE).with(principal(memberA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_PROFILE_NOT_FOUND"));
        mockMvc.perform(delete("/api/v1/members/{userId}/follow", adminE).with(principal(memberA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_PROFILE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/members/{userId}/follow", adminE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_PROFILE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/members/{userId}/followers", Long.MAX_VALUE).with(principal(memberA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_PROFILE_NOT_FOUND"));
        mockMvc.perform(put("/api/v1/members/{userId}/follow", 0).with(principal(memberA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(followRows(memberA, adminE)).isZero();
    }

    @Test
    void aLockedMemberReceivesNoNewFollowerButCanStillBeUnfollowed() throws Exception {
        mockMvc.perform(put("/api/v1/members/{userId}/follow", lockedD).with(principal(memberA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FOLLOW_TARGET_INACTIVE"));
        assertThat(followRows(memberA, lockedD)).isZero();

        followFixture(memberB, lockedD, "2026-10-01T08:00:00");
        mockMvc.perform(delete("/api/v1/members/{userId}/follow", lockedD).with(principal(memberB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false));
        assertThat(followRows(memberB, lockedD)).isZero();
    }

    @Test
    void publicFollowSummaryCountsOnlyActiveAccountsAndShowsTheViewersState() throws Exception {
        followFixture(memberA, memberB, "2026-10-01T08:00:00");
        followFixture(expertC, memberB, "2026-10-02T08:00:00");
        followFixture(lockedD, memberB, "2026-10-03T08:00:00");
        followFixture(memberB, expertC, "2026-10-04T08:00:00");
        followFixture(memberB, lockedD, "2026-10-05T08:00:00");

        mockMvc.perform(get("/api/v1/members/{userId}/follow", memberB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(memberB))
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(2))
                .andExpect(jsonPath("$.followingCount").value(1));
        mockMvc.perform(get("/api/v1/members/{userId}/follow", memberB).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true));

        // Q55: a LOCKED member keeps a public profile, so its summary is readable.
        mockMvc.perform(get("/api/v1/members/{userId}/follow", lockedD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(1));
    }

    @Test
    void followerAndFollowingListsArePaginatedNewestFirstWithPublicFieldsOnly() throws Exception {
        jdbcTemplate.update("UPDATE [USER] SET avatar_url = ? WHERE user_id = ?", "https://img.test/c.png", expertC);
        followFixture(memberA, memberB, "2026-10-01T08:00:00");
        followFixture(expertC, memberB, "2026-10-02T08:00:00");
        followFixture(lockedD, memberB, "2026-10-03T08:00:00");
        followFixture(memberB, expertC, "2026-10-04T08:00:00");
        followFixture(memberB, lockedD, "2026-10-05T08:00:00");

        mockMvc.perform(get("/api/v1/members/{userId}/followers", memberB).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].userId").value(expertC))
                .andExpect(jsonPath("$.content[0].displayName").value("Issue 69 c"))
                .andExpect(jsonPath("$.content[0].avatarUrl").value("https://img.test/c.png"))
                .andExpect(jsonPath("$.content[0].length()").value(3))
                .andExpect(jsonPath("$.content[1].userId").value(memberA));
        mockMvc.perform(get("/api/v1/members/{userId}/followers", memberB).param("page", "1").param("size", "1")
                        .with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(memberA));
        mockMvc.perform(get("/api/v1/members/{userId}/following", memberB).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(expertC));
        mockMvc.perform(get("/api/v1/members/{userId}/followers", lockedD).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(memberB));
    }

    @Test
    void followListsRequireAuthenticationAndBoundedPaging() throws Exception {
        mockMvc.perform(get("/api/v1/members/{userId}/followers", memberB))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/members/{userId}/following", memberB))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/members/{userId}/followers", memberB).param("page", "-1")
                        .with(principal(memberA)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/members/{userId}/following", memberB).param("size", "0")
                        .with(principal(memberA)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/members/{userId}/following", memberB).param("size", "51")
                        .with(principal(memberA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void publicExpertDirectoryIncludesActiveExpertsAndViewerFollowStateOnly() throws Exception {
        followFixture(memberA, expertC, "2026-10-06T08:00:00");

        mockMvc.perform(get("/api/v1/experts").param("query", "Issue 69 c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(expertC))
                .andExpect(jsonPath("$.content[0].displayName").value("Issue 69 c"))
                .andExpect(jsonPath("$.content[0].followerCount").value(1))
                .andExpect(jsonPath("$.content[0].following").value(false))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].role").doesNotExist());

        mockMvc.perform(get("/api/v1/experts").param("query", "Issue 69 c").with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].following").value(true));
    }

    @Test
    void generatedOpenApiMarksOnlyTheFollowSummaryAsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/members/{userId}/follow'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/members/{userId}/follow'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/members/{userId}/follow'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/members/{userId}/follow'].delete.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/members/{userId}/followers'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/members/{userId}/following'].get.security[0].bearerAuth").isArray());
    }

    private boolean followAs(CountDownLatch start, long followerId, long followedId) throws InterruptedException {
        start.await();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(Long.toString(followerId), null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        SecurityContextHolder.setContext(context);
        try {
            return followService.follow(followedId).following();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private RequestPostProcessor principal(long userId) {
        return principal(userId, "ROLE_CUSTOMER");
    }

    private RequestPostProcessor principal(long userId, String authority) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(authority));
    }

    private long insertUser(String name, String role, String accountStatus) {
        String email = EMAIL_PREFIX + name + "@test.local";
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, ?, 1)",
                email, "Issue 69 " + name, role, accountStatus);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private void followFixture(long followerId, long followedId, String createdAt) {
        jdbcTemplate.update("INSERT INTO [USER_FOLLOW] (follower_user_id, followed_user_id, created_at) VALUES (?, ?, ?)",
                followerId, followedId, Timestamp.valueOf(createdAt.replace('T', ' ')));
    }

    private int followRows(long followerId, long followedId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [USER_FOLLOW] WHERE follower_user_id = ? AND followed_user_id = ?",
                Integer.class, followerId, followedId);
    }
}
