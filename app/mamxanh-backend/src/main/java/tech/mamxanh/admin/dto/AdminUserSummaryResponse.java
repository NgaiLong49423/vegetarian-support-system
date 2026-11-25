package tech.mamxanh.admin.dto;

public record AdminUserSummaryResponse(long totalUsers, long activeUsers, long expertUsers, long lockedUsers,
        long activeExperts, long lockedExperts, long expertPublishedRecipeCount) { }
