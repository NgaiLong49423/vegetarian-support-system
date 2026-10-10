package tech.mamxanh.recipe.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import tech.mamxanh.recipe.service.RecipePostService;
import tech.mamxanh.recipe.service.RecipePostService.RecipePageResponse;

/** FR-23 public profile: the published Recipe Posts of one member (UC-23.2, AC-23.2, Q56). */
@RestController
@RequestMapping("/api/v1/members")
@Validated
public class MemberRecipeController {

    private final RecipePostService service;

    public MemberRecipeController(RecipePostService service) {
        this.service = service;
    }

    @GetMapping("/{userId}/recipes")
    public RecipePageResponse publishedRecipes(@PathVariable @Min(1) long userId,
            @Parameter(description = "Số trang bắt đầu từ 0.")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Số công thức mỗi trang (từ 1 đến 50).")
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return service.listPublishedByAuthor(userId, page, size);
    }
}
