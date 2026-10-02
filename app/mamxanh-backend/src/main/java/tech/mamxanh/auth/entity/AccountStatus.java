package tech.mamxanh.auth.entity;

/**
 * Values of {@code USER.account_status} (CK_USER_account_status). Email verification is a
 * separate flag ({@code email_verified}); there is no UNVERIFIED status.
 */
public enum AccountStatus {
    ACTIVE,
    LOCKED
}
