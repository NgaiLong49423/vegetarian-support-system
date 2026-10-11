package tech.mamxanh.recipe.dto.response;

/**
 * Response DTO returned after uploading an image file (FR-14, AC-14.1, AC-14.2).
 */
public record UploadImageResponse(
        String blobUrl,
        String mimeType,
        long sizeBytes
) {}
