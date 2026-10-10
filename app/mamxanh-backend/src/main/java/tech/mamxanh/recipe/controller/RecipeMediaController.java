package tech.mamxanh.recipe.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.mamxanh.common.response.ApiResponse;
import tech.mamxanh.recipe.dto.request.UpdateRecipeMediaRequest;
import tech.mamxanh.recipe.dto.response.RecipeMediaResponse;
import tech.mamxanh.recipe.dto.response.UploadImageResponse;
import tech.mamxanh.recipe.service.RecipeMediaService;

import java.util.List;

/**
 * Controller handling recipe media uploads and management (FR-14).
 */
@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeMediaController {

    private final RecipeMediaService recipeMediaService;

    public RecipeMediaController(RecipeMediaService recipeMediaService) {
        this.recipeMediaService = recipeMediaService;
    }

    /**
     * Upload an image file to storage (UC-14.1, AC-14.1, AC-14.2).
     * Restricted to Chuyên gia (ROLE_EXPERT) and Administrator (ROLE_ADMIN).
     */
    @PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EXPERT', 'ADMIN')")
    public ResponseEntity<ApiResponse<UploadImageResponse>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        UploadImageResponse response = recipeMediaService.uploadImage(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải ảnh lên thành công.", response));
    }

    /**
     * Retrieve media items of a recipe ordered by display order (UC-14.3).
     * Publicly viewable by Guest and Member.
     */
    @GetMapping("/{recipeId}/media")
    public ApiResponse<List<RecipeMediaResponse>> getRecipeMedia(@PathVariable Long recipeId) {
        List<RecipeMediaResponse> media = recipeMediaService.getRecipeMedia(recipeId);
        return ApiResponse.success("Lấy danh sách ảnh công thức thành công.", media);
    }

    /**
     * Update/associate media items for a recipe (UC-14.2, AC-14.2, AC-14.3, BR-19).
     * Enforces max 5 images and exactly 1 cover image when media items exist.
     */
    @PutMapping("/{recipeId}/media")
    @PreAuthorize("hasAnyRole('EXPERT', 'ADMIN')")
    public ApiResponse<List<RecipeMediaResponse>> updateRecipeMedia(
            @PathVariable Long recipeId,
            @Valid @RequestBody UpdateRecipeMediaRequest request) {
        List<RecipeMediaResponse> updated = recipeMediaService.setRecipeMedia(recipeId, request);
        return ApiResponse.success("Cập nhật danh sách ảnh công thức thành công.", updated);
    }

    /**
     * Delete all media items of a recipe and clean up Azure Blob resources.
     */
    @DeleteMapping("/{recipeId}/media")
    @PreAuthorize("hasAnyRole('EXPERT', 'ADMIN')")
    public ApiResponse<Void> deleteRecipeMedia(@PathVariable Long recipeId) {
        recipeMediaService.deleteRecipeMedia(recipeId);
        return ApiResponse.success("Xóa toàn bộ ảnh công thức thành công.", null);
    }
}
