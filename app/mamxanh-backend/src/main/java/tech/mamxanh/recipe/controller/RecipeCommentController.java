package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.request.CommentCreateRequest;
import tech.mamxanh.recipe.dto.request.CommentUpdateRequest;
import tech.mamxanh.recipe.dto.response.CommentResponse;
import tech.mamxanh.recipe.dto.response.RecipeCommentPageResponse;
import tech.mamxanh.recipe.service.RecipeCommentService;

/** FR-46 comments and replies of up to five levels on published recipes (UC-46.1–UC-46.5). */
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class RecipeCommentController {
    private final RecipeCommentService service;

    public RecipeCommentController(RecipeCommentService service) {
        this.service = service;
    }

    @GetMapping("/recipes/{recipeId}/comments")
    @Operation(summary = "List the comment threads of a published recipe",
            description = "Public. Root threads newest first, each with its visible replies oldest first. "
                    + "A deleted comment that still has replies is returned with `deleted = true` and no content or author.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of comment threads."),
            @ApiResponse(responseCode = "400", description = "Page or size is outside its supported range."),
            @ApiResponse(responseCode = "404", description = "RECIPE_NOT_FOUND: the recipe is not published.")
    })
    public RecipeCommentPageResponse list(@PathVariable @Min(1) long recipeId,
            @Parameter(description = "Zero-based page index.") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Root threads per page, from 1 through 50.") @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return service.list(recipeId, page, size);
    }

    @PostMapping(path = "/recipes/{recipeId}/comments", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Comment on a published recipe or reply to a comment",
            description = "Published immediately. A reply to a level-5 comment is attached to its level-4 parent.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The new comment."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: content must have 1–1,000 characters."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "403", description = "MEMBER_ACCESS_REQUIRED: only an active Customer or Expert comments."),
            @ApiResponse(responseCode = "404", description = "RECIPE_NOT_FOUND, or COMMENT_NOT_FOUND for a reply target that is gone or on another recipe.")
    })
    public ResponseEntity<CommentResponse> create(@PathVariable @Min(1) long recipeId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(recipeId, request));
    }

    @PutMapping(path = "/comments/{commentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Edit the caller's own comment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The edited comment, marked as edited."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: content must have 1–1,000 characters."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "403", description = "COMMENT_EDIT_NOT_ALLOWED for someone else's comment; MEMBER_ACCESS_REQUIRED for a non-member."),
            @ApiResponse(responseCode = "404", description = "COMMENT_NOT_FOUND: the comment is deleted or its recipe is not published.")
    })
    public CommentResponse update(@PathVariable @Min(1) long commentId, @Valid @RequestBody CommentUpdateRequest request) {
        return service.update(commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Delete the caller's own comment",
            description = "Idempotent. Replies stay visible under a tombstone.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The comment is deleted."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "403", description = "COMMENT_EDIT_NOT_ALLOWED for someone else's comment; MEMBER_ACCESS_REQUIRED for a non-member."),
            @ApiResponse(responseCode = "404", description = "COMMENT_NOT_FOUND: no comment on a published recipe has this id.")
    })
    public ResponseEntity<Void> delete(@PathVariable @Min(1) long commentId) {
        service.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
