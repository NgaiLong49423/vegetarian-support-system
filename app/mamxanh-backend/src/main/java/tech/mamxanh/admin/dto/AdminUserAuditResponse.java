package tech.mamxanh.admin.dto;

import java.time.LocalDateTime;

/** One account activity, including administration audits and AI Credit ledger grants. */
public record AdminUserAuditResponse(String eventId, String action, String oldValue, String newValue,
        String reason, Long administratorId, String administratorName, LocalDateTime occurredAt,
        LocalDateTime effectiveUntil) { }
