package tech.mamxanh.integration.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Explicit mock for offline automated tests (FR-14); never registered by runtime StorageConfig.
 * Generates pseudo-blob URLs and tracks metadata in memory; it does not store image bytes.
 */
public class MockStorageClient implements StorageClient {

    private static final Logger log = LoggerFactory.getLogger(MockStorageClient.class);

    private final String containerName;
    private final ConcurrentMap<String, String> uploadedBlobs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, byte[]> privateApplicationFiles = new ConcurrentHashMap<>();

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
    public String uploadPrivateApplicationFile(byte[] content, String mimeType, String extension) {
        String blobKey = EXPERT_APPLICATION_FOLDER + "/" + UUID.randomUUID() + extension;
        privateApplicationFiles.put(blobKey, content.clone());
        return blobKey;
    }

    @Override
    public byte[] downloadPrivateApplicationFile(String blobKey) {
        byte[] content = privateApplicationFiles.get(blobKey);
        if (content == null) throw new IllegalArgumentException("Private application file not found");
        return content.clone();
    }

    @Override
    public void deletePrivateApplicationFile(String blobKey) {
        if (blobKey != null) privateApplicationFiles.remove(blobKey);
    }

    @Override
    public boolean isCloudStorageConfigured() {
        return false;
    }

    public boolean containsBlob(String blobUrl) {
        return uploadedBlobs.containsKey(blobUrl);
    }

    public boolean containsPrivateApplicationFile(String blobKey) {
        return privateApplicationFiles.containsKey(blobKey);
    }
}
