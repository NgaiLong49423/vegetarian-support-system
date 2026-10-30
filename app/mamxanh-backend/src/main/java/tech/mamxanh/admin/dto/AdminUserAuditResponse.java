package tech.mamxanh.admin.dto;

import java.time.LocalDateTime;

public record AdminUserAuditResponse(String eventId, String action, String oldValue, String newValue,
        String reason, Long administratorId, String administratorName, LocalDateTime occurredAt) { }
