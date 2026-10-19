package tech.mamxanh.integration.storage;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/** Keeps unrelated application features available without fabricating successful uploads. */
public class UnavailableStorageClient implements StorageClient {
    private static final Logger log = LoggerFactory.getLogger(UnavailableStorageClient.class);

    @Override
    public String uploadImage(InputStream inputStream, long size, String mimeType, String originalFilename, String folder) {
        throw new AppException(ErrorCode.MEDIA_STORAGE_UNAVAILABLE);
    }

    @Override
    public void deleteImage(String blobUrl) {
        // Existing media deletion is best-effort; never log URLs that may contain credentials.
        if (blobUrl != null && !blobUrl.isBlank()) log.warn("Cannot delete image while storage is unavailable.");
    }

    @Override
    public boolean isCloudStorageConfigured() {
        return false;
    }
}
