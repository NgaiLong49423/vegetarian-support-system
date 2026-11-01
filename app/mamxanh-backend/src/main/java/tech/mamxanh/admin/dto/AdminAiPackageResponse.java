package tech.mamxanh.admin.dto;

import java.time.LocalDateTime;

public record AdminAiPackageResponse(long userId, String oldTier, String newTier,
        LocalDateTime startsAt, LocalDateTime endsAt, long tokenQuota, boolean changed,
        LocalDateTime effectiveAt) { }
