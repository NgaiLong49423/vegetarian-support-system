package tech.mamxanh.expertapplication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(@NotBlank @Size(min = 10, max = 500) String reason) {
    public ReviewRequest { if (reason != null) reason = reason.trim(); }
}
