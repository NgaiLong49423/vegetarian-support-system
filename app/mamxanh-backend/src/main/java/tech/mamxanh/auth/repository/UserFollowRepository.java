package tech.mamxanh.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.mamxanh.auth.entity.UserFollow;
import tech.mamxanh.auth.entity.UserFollowId;

/** FR-59 queries. Counts and lists include only ACTIVE counterpart accounts (AC-59.7). */
public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollowId> {

    /** Idempotent follow (AC-59.4); the range lock keeps parallel requests from inserting the pair twice. */
    @Modifying
    @Query(value = """
            INSERT INTO USER_FOLLOW (follower_user_id, followed_user_id, created_at)
            SELECT :followerId, :followedId, SYSUTCDATETIME()
            WHERE NOT EXISTS (
                SELECT 1 FROM USER_FOLLOW WITH (UPDLOCK, HOLDLOCK)
                WHERE follower_user_id = :followerId AND followed_user_id = :followedId
            )
            """, nativeQuery = true)
    int insertIfAbsent(@Param("followerId") long followerId, @Param("followedId") long followedId);

    @Modifying
    @Query("delete from UserFollow f where f.id.followerUserId = :followerId and f.id.followedUserId = :followedId")
    int deleteFollow(@Param("followerId") long followerId, @Param("followedId") long followedId);

    @Query("""
            select count(f) from UserFollow f, User u
            where u.id = f.id.followerUserId and f.id.followedUserId = :userId
              and u.accountStatus = tech.mamxanh.auth.entity.AccountStatus.ACTIVE
            """)
    long countActiveFollowers(@Param("userId") long userId);

    @Query("""
            select count(f) from UserFollow f, User u
            where u.id = f.id.followedUserId and f.id.followerUserId = :userId
              and u.accountStatus = tech.mamxanh.auth.entity.AccountStatus.ACTIVE
            """)
    long countActiveFollowing(@Param("userId") long userId);

    @Query(value = """
            select u.id as userId, u.displayName as displayName, u.avatarUrl as avatarUrl
            from UserFollow f, User u
            where u.id = f.id.followerUserId and f.id.followedUserId = :userId
              and u.accountStatus = tech.mamxanh.auth.entity.AccountStatus.ACTIVE
            order by f.createdAt desc, u.id desc
            """, countQuery = """
            select count(f) from UserFollow f, User u
            where u.id = f.id.followerUserId and f.id.followedUserId = :userId
              and u.accountStatus = tech.mamxanh.auth.entity.AccountStatus.ACTIVE
            """)
    Page<FollowMemberView> findActiveFollowers(@Param("userId") long userId, Pageable pageable);

    @Query(value = """
            select u.id as userId, u.displayName as displayName, u.avatarUrl as avatarUrl
            from UserFollow f, User u
            where u.id = f.id.followedUserId and f.id.followerUserId = :userId
              and u.accountStatus = tech.mamxanh.auth.entity.AccountStatus.ACTIVE
            order by f.createdAt desc, u.id desc
            """, countQuery = """
            select count(f) from UserFollow f, User u
            where u.id = f.id.followedUserId and f.id.followerUserId = :userId
              and u.accountStatus = tech.mamxanh.auth.entity.AccountStatus.ACTIVE
            """)
    Page<FollowMemberView> findActiveFollowing(@Param("userId") long userId, Pageable pageable);

    /** Public profile fields only (BR-18, AC-59.6). */
    interface FollowMemberView {
        Long getUserId();
        String getDisplayName();
        String getAvatarUrl();
    }
}
