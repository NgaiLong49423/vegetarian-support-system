package tech.mamxanh.admin.dto;

import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;

/** Current Expert account and public profile with its most recently approved application, if any. */
public record AdminExpertDetailResponse(AdminUserResponse account, String avatarUrl, String bio,
        ExpertApplicationResponse approvedApplication) { }
