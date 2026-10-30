package tech.mamxanh.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeAdminUserRoleRequest(
        @NotBlank @Pattern(regexp = "CUSTOMER|EXPERT") String role,
        @NotBlank @Size(min = 10, max = 500) String reason) { }
