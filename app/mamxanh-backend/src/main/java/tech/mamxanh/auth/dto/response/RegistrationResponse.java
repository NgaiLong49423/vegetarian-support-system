package tech.mamxanh.auth.dto.response;

import tech.mamxanh.auth.entity.AccountStatus;

/** 201 body of {@code POST /api/v1/auth/register} (openapi.yaml RegistrationResponse). */
public record RegistrationResponse(AccountStatus accountStatus, boolean emailVerified, String message) {
}
