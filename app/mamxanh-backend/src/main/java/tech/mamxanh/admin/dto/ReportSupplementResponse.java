package tech.mamxanh.admin.dto;

import java.time.LocalDateTime;

public record ReportSupplementResponse(long supplementId, String content, LocalDateTime createdAt) { }
