package tech.mamxanh.integration.storage;

import java.io.InputStream;

/**
 * Storage client abstraction for recipe media uploads and management (FR-14).
 * Decouples recipe business logic from the underlying storage mechanism.
 */
public interface StorageClient {

    /**
     * Upload an image stream to storage.
     *
     * @param inputStream      data stream of the image
     * @param size             size of stream in bytes
     * @param mimeType         MIME type (image/jpeg, image/png, image/webp)
     * @param originalFilename original filename to extract file extension
     * @return publicly accessible blob URL
     */
    String uploadImage(InputStream inputStream, long size, String mimeType, String originalFilename);

    /**
     * Delete an image from storage by its blob URL.
     *
     * @param blobUrl public URL of the blob
     */
    void deleteImage(String blobUrl);

    /**
     * Check if storage client is backed by real cloud storage.
     *
     * @return true if cloud storage is configured, false if running in local mock fallback
     */
    boolean isCloudStorageConfigured();
}
