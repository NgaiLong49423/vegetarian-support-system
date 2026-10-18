package tech.mamxanh.recipe.dto.request;

import jakarta.validation.constraints.Pattern;

/** Anonymous browser identifier only; account identity and timestamps are server-owned. */
public record RecipeViewRequest(
        @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                message = "Định danh phiên khách phải là UUID hợp lệ.") String anonymousViewerId) { }
