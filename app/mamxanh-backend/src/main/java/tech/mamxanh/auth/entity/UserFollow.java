package tech.mamxanh.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Directed follow relation of {@code USER_FOLLOW} (FR-59, BR-75): {@code follower} follows
 * {@code followed}. Rows are written by {@code UserFollowRepository}; {@code created_at} is UTC.
 */
@Entity
@Table(name = "\"USER_FOLLOW\"")
public class UserFollow {
    @EmbeddedId private UserFollowId id;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    protected UserFollow() { }
}
