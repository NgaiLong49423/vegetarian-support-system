package tech.mamxanh.integration.storage;

import java.io.InputStream;

/**
 * Storage client abstraction for recipe media and private expert application attachments.
 * Decouples recipe business logic from the underlying storage mechanism.
 */
public interface StorageClient {

    /** Folder for Recipe Post images (FR-14). */
    String RECIPE_FOLDER = "recipes";

    /** Folder for member avatars (FR-23). */
    String AVATAR_FOLDER = "avatars";
    String EXPERT_APPLICATION_FOLDER = "expert-applications";

    /**
     * Upload a recipe image stream to storage (in {@link #RECIPE_FOLDER}).
     *
     * @param inputStream      data stream of the image
     * @param size             size of stream in bytes
     * @param mimeType         MIME type (image/jpeg, image/png, image/webp)
     * @param originalFilename original filename to extract file extension
     * @return publicly accessible blob URL
     */
    default String uploadImage(InputStream inputStream, long size, String mimeType, String originalFilename) {
        return uploadImage(inputStream, size, mimeType, originalFilename, RECIPE_FOLDER);
    }

    /**
     * Upload an image stream to storage under the given folder.
     *
     * @param folder blob folder, {@link #RECIPE_FOLDER} or {@link #AVATAR_FOLDER}
     * @return publicly accessible blob URL
     */
    String uploadImage(InputStream inputStream, long size, String mimeType, String originalFilename, String folder);

    /**
     * Delete an image from storage by its blob URL.
     *
     * @param blobUrl public URL of the blob
     */
    void deleteImage(String blobUrl);

    /** Uploads application-only data to a private container and returns its internal blob key. */
    default String uploadPrivateApplicationFile(byte[] content, String mimeType, String extension) {
        throw new UnsupportedOperationException("Private application file storage is unavailable");
    }

    /** Reads a private application file by its server-generated blob key. */
    default byte[] downloadPrivateApplicationFile(String blobKey) {
        throw new UnsupportedOperationException("Private application file storage is unavailable");
    }

    /** Deletes a private application file by its server-generated blob key. */
    default void deletePrivateApplicationFile(String blobKey) {
        throw new UnsupportedOperationException("Private application file storage is unavailable");
    }

    /**
     * Check if storage client is backed by real cloud storage.
     *
     * @return true if cloud storage is configured, false if running in local mock fallback
     */
    boolean isCloudStorageConfigured();
}
