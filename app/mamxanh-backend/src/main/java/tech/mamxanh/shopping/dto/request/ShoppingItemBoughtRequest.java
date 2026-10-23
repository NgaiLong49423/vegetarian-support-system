package tech.mamxanh.shopping.dto.request;

import jakarta.validation.constraints.NotNull;

public record ShoppingItemBoughtRequest(@NotNull Boolean bought) { }
