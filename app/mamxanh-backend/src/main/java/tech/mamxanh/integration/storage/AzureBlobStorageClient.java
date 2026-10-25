package tech.mamxanh.integration.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

/**
 * Azure Blob Storage implementation of StorageClient (FR-14).
 * Uploads recipe images with UUID filenames and sets the appropriate MIME type.
 */
public class AzureBlobStorageClient implements StorageClient {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageClient.class);

    private final BlobContainerClient containerClient;

    public AzureBlobStorageClient(BlobContainerClient containerClient) {
        this.containerClient = containerClient;
    }

    @Override
    public String uploadImage(InputStream inputStream, long size, String mimeType, String originalFilename,
            String folder) {
        String extension = resolveExtension(mimeType, originalFilename);
        String blobName = folder + "/" + UUID.randomUUID() + extension;

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(inputStream, size, true);
            BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(mimeType);
            blobClient.setHttpHeaders(headers);
            return blobClient.getBlobUrl();
        } catch (RuntimeException exception) {
            // SDK messages can contain request details; keep credentials and URLs out of logs/errors.
            log.warn("Azure image upload failed ({}).", exception.getClass().getSimpleName());
            throw new AppException(ErrorCode.MEDIA_STORAGE_UNAVAILABLE);
        }
    }

    @Override
    public void deleteImage(String blobUrl) {
        if (!StringUtils.hasText(blobUrl)) {
            return;
        }
        try {
            String blobName = extractBlobName(blobUrl);
            if (blobName != null) {
                BlobClient blobClient = containerClient.getBlobClient(blobName);
                boolean deleted = blobClient.deleteIfExists();
                if (deleted) {
                    log.info("Deleted recipe image from Azure Blob Storage: {}", blobName);
                } else {
                    log.warn("Blob not found for deletion: {}", blobName);
                }
            }
        } catch (Exception e) {
            log.warn("Azure image deletion failed ({}).", e.getClass().getSimpleName());
        }
    }

    @Override
    public boolean isCloudStorageConfigured() {
        return true;
    }

    private String extractBlobName(String blobUrl) {
        try {
            URI uri = URI.create(blobUrl);
            String path = uri.getPath(); // e.g. /mamxanh-recipes/recipes/abc.jpg
            String containerPath = "/" + containerClient.getBlobContainerName() + "/";
            int index = path.indexOf(containerPath);
            if (index != -1) {
                return path.substring(index + containerPath.length());
            }
            // fallback: return everything after leading slash
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.warn("Could not parse image URL for deletion.");
            return null;
        }
    }

    private String resolveExtension(String mimeType, String originalFilename) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp")) {
                return ext;
            }
        }
        if ("image/png".equalsIgnoreCase(mimeType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(mimeType)) {
            return ".webp";
        }
        return ".jpg";
    }
}
