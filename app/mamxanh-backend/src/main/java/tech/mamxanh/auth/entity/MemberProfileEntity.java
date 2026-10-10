package tech.mamxanh.auth.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Profile view of the {@code USER} row (FR-23): the public name, avatar and short bio a member
 * edits, plus the role and creation time needed to show a public profile. Security columns stay
 * on {@link User}; timestamps are UTC.
 */
@Entity
@Table(name = "\"USER\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberProfileEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Nationalized
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Nationalized
    @Column(name = "bio", length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20, insertable = false, updatable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20, insertable = false, updatable = false)
    private AccountStatus accountStatus;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Q58: the name is already trimmed and validated; an empty bio is stored as {@code NULL}. */
    public void updateProfile(String displayName, String bio, LocalDateTime now) {
        this.displayName = displayName;
        this.bio = bio;
        this.updatedAt = now;
    }

    public void changeAvatar(String avatarUrl, LocalDateTime now) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = now;
    }
}
