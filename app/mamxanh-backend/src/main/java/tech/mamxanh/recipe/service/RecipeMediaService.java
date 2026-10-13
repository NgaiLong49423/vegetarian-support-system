package tech.mamxanh.recipe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.storage.StorageClient;
import tech.mamxanh.recipe.dto.request.RecipeMediaItemRequest;
import tech.mamxanh.recipe.dto.request.UpdateRecipeMediaRequest;
import tech.mamxanh.recipe.dto.response.RecipeMediaResponse;
import tech.mamxanh.recipe.dto.response.UploadImageResponse;
import tech.mamxanh.recipe.entity.RecipeMediaEntity;
import tech.mamxanh.recipe.repository.RecipeMediaRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service managing recipe images and Azure Blob Storage lifecycle (FR-14).
 * Enforces image format, 5MB size limit, max 5 images, and mandatory single cover constraint.
 */
@Service
public class RecipeMediaService {

    private static final Logger log = LoggerFactory.getLogger(RecipeMediaService.class);

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB (NFR-10, AC-14.1)
    public static final int MAX_MEDIA_ITEMS = 5; // (BR-19, AC-14.1)

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final StorageClient storageClient;
    private final RecipeMediaRepository recipeMediaRepository;
    private final RecipePostService recipePostService;

    public RecipeMediaService(StorageClient storageClient, RecipeMediaRepository recipeMediaRepository,
            RecipePostService recipePostService) {
        this.storageClient = storageClient;
        this.recipeMediaRepository = recipeMediaRepository;
        this.recipePostService = recipePostService;
    }

    /**
     * Upload an image file to Azure Blob Storage (UC-14.1, AC-14.1, AC-14.2).
     */
    public UploadImageResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Tệp tin hình ảnh không được để trống.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE, "Dung lượng ảnh vượt quá 5 MB cho phép.");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(ErrorCode.UNSUPPORTED_IMAGE_TYPE,
                    "Chỉ hỗ trợ tệp hình ảnh định dạng JPEG, PNG hoặc WebP.");
        }

        try (InputStream is = file.getInputStream()) {
            String blobUrl = storageClient.uploadImage(is, file.getSize(), contentType, file.getOriginalFilename());
            return new UploadImageResponse(blobUrl, contentType, file.getSize());
        } catch (IOException e) {
            log.error("Failed to read image stream for upload: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể đọc dữ liệu ảnh để tải lên.");
        }
    }

    /**
     * Retrieve all media for a recipe ordered by displayOrder (UC-14.3).
     */
    @Transactional(readOnly = true)
    public List<RecipeMediaResponse> getRecipeMedia(Long recipeId) {
        return recipeMediaRepository.findByRecipeIdOrderByDisplayOrderAsc(recipeId)
                .stream()
                .map(RecipeMediaResponse::fromEntity)
                .toList();
    }

    /**
     * Associate or update the recipe's media library (UC-14.2, AC-14.2, AC-14.3, BR-19).
     * Only the active Expert author of a published recipe may change it (BR-64, AC-04.4, AC-04.7).
     */
    @Transactional
    public List<RecipeMediaResponse> setRecipeMedia(Long recipeId, UpdateRecipeMediaRequest request) {
        recipePostService.lockOwnEditableRecipe(recipeId);
        List<RecipeMediaItemRequest> items = request.mediaItems();

        if (items.size() > MAX_MEDIA_ITEMS) {
            throw new AppException(ErrorCode.MAX_RECIPE_MEDIA_EXCEEDED,
                    "Mỗi bài công thức chỉ được có tối đa 5 ảnh minh họa.");
        }

        // AC-14.3 & BR-19: When there are 1..5 images, exactly one must be marked as cover
        if (!items.isEmpty()) {
            long coverCount = items.stream().filter(RecipeMediaItemRequest::isCover).count();
            if (coverCount != 1) {
                throw new AppException(ErrorCode.INVALID_COVER_IMAGE_CONFIGURATION,
                        "Bài công thức có ảnh bắt buộc phải có đúng một ảnh được chọn làm ảnh bìa.");
            }

            // Verify unique display order between 1 and 5
            Set<Integer> orders = new HashSet<>();
            for (RecipeMediaItemRequest item : items) {
                if (item.displayOrder() < 1 || item.displayOrder() > 5) {
                    throw new AppException(ErrorCode.VALIDATION_FAILED, "Thứ tự hiển thị phải nằm trong khoảng từ 1 đến 5.");
                }
                if (!orders.add(item.displayOrder())) {
                    throw new AppException(ErrorCode.VALIDATION_FAILED, "Thứ tự hiển thị ảnh không được trùng lặp.");
                }
            }
        }

        // Delete previous media entities for this recipe. Flush first: Hibernate runs inserts before
        // deletes, so the new rows would otherwise collide with UQ_RECIPE_MEDIA_order.
        recipeMediaRepository.deleteByRecipeId(recipeId);
        recipeMediaRepository.flush();

        if (items.isEmpty()) {
            return List.of();
        }

        List<RecipeMediaEntity> newEntities = items.stream()
                .map(item -> new RecipeMediaEntity(
                        recipeId,
                        item.blobUrl(),
                        item.mimeType(),
                        item.displayOrder(),
                        item.isCover()
                ))
                .toList();

        List<RecipeMediaEntity> saved = recipeMediaRepository.saveAll(newEntities);
        log.info("Saved {} media items for recipeId={}", saved.size(), recipeId);

        return saved.stream()
                .map(RecipeMediaResponse::fromEntity)
                .toList();
    }

    /**
     * Delete all media of a recipe and release resources on Azure Blob Storage.
     * Only the active Expert author of a published recipe may do this (BR-64, AC-04.4, AC-04.7).
     */
    @Transactional
    public void deleteRecipeMedia(Long recipeId) {
        recipePostService.lockOwnEditableRecipe(recipeId);
        List<RecipeMediaEntity> mediaList = recipeMediaRepository.findByRecipeIdOrderByDisplayOrderAsc(recipeId);
        for (RecipeMediaEntity media : mediaList) {
            storageClient.deleteImage(media.getBlobUrl());
        }
        recipeMediaRepository.deleteByRecipeId(recipeId);
        log.info("Cleaned up {} media items for recipeId={}", mediaList.size(), recipeId);
    }
}
