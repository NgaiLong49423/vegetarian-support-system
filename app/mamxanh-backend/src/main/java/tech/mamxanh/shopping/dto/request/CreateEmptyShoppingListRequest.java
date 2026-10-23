package tech.mamxanh.shopping.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmptyShoppingListRequest(
        @NotBlank @Size(max = 200) String name) { }
