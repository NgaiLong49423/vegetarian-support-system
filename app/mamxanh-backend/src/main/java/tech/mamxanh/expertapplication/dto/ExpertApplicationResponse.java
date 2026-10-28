package tech.mamxanh.expertapplication.dto;

import java.time.LocalDateTime;

public record ExpertApplicationResponse(long id, long userId, String displayName, String email,
        String experience, String vegetarianType, String sampleRecipeSummary, String portfolioUrl,
        boolean certificateAvailable, boolean applicationAvatarAvailable,
        String status, String adminNote, LocalDateTime submittedAt, LocalDateTime reviewedAt) {}
