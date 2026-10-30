package tech.mamxanh.admin.dto;

public record AdminUserSummaryResponse(long totalUsers, long activeUsers, long expertUsers, long lockedUsers) { }
