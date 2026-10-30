package tech.mamxanh.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeAdminUserStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|LOCKED") String accountStatus,
        @NotBlank @Size(min = 10, max = 500) String reason) { }
