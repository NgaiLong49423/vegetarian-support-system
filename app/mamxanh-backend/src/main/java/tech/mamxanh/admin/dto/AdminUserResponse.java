package tech.mamxanh.admin.dto;

import java.time.LocalDateTime;

/** Minimal account data for the Administrator user-management screen. */
public record AdminUserResponse(long id, String displayName, String email, String role,
        String accountStatus, LocalDateTime joinedAt, long publishedRecipeCount) { }
