package tech.mamxanh.integration.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Fallback / mock storage client for local development and offline automated testing (FR-14).
 * Generates deterministic pseudo-blob URLs and keeps track of uploads in memory without cloud dependencies.
 */
public class MockStorageClient implements StorageClient {

    private static final Logger log = LoggerFactory.getLogger(MockStorageClient.class);

    private final String containerName;
    private final ConcurrentMap<String, String> uploadedBlobs = new ConcurrentHashMap<>();

    public MockStorageClient(String containerName) {
        this.containerName = StringUtils.hasText(containerName) ? containerName : "mamxanh-recipes";
    }

    @Override
    public String uploadImage(InputStream inputStream, long size, String mimeType, String originalFilename,
            String folder) {
        String ext = ".jpg";
        if ("image/png".equalsIgnoreCase(mimeType)) {
            ext = ".png";
        } else if ("image/webp".equalsIgnoreCase(mimeType)) {
            ext = ".webp";
        }
        String filename = UUID.randomUUID() + ext;
        String mockBlobUrl = "https://mamxanh.blob.core.windows.net/" + containerName + "/" + folder + "/" + filename;
        uploadedBlobs.put(mockBlobUrl, mimeType);
        log.info("[MockStorage] Stored mock blob (size: {} bytes, mime: {}): {}", size, mimeType, mockBlobUrl);
        return mockBlobUrl;
    }

    @Override
    public void deleteImage(String blobUrl) {
        if (blobUrl != null) {
            uploadedBlobs.remove(blobUrl);
            log.info("[MockStorage] Deleted mock blob: {}", blobUrl);
        }
    }

    @Override
    public boolean isCloudStorageConfigured() {
        return false;
    }

    public boolean containsBlob(String blobUrl) {
        return uploadedBlobs.containsKey(blobUrl);
    }
}
