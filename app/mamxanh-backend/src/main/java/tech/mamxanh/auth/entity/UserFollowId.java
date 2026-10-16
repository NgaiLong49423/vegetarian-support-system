package tech.mamxanh.auth.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Composite key {@code PK_USER_FOLLOW (follower_user_id, followed_user_id)}; one row per directed pair. */
@Embeddable
public class UserFollowId implements Serializable {
    @Column(name = "follower_user_id") private Long followerUserId;
    @Column(name = "followed_user_id") private Long followedUserId;

    protected UserFollowId() { }

    public UserFollowId(Long followerUserId, Long followedUserId) {
        this.followerUserId = followerUserId;
        this.followedUserId = followedUserId;
    }

    @Override public boolean equals(Object value) {
        return value instanceof UserFollowId other && Objects.equals(followerUserId, other.followerUserId)
                && Objects.equals(followedUserId, other.followedUserId);
    }

    @Override public int hashCode() { return Objects.hash(followerUserId, followedUserId); }
}
