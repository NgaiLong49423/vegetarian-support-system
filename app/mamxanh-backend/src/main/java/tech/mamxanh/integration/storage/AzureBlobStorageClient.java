package tech.mamxanh.integration.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * Azure Blob Storage implementation of StorageClient (FR-14).
 * Uploads recipe images with UUID filenames and sets the appropriate MIME type.
 */
public class AzureBlobStorageClient implements StorageClient {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageClient.class);

    private final BlobContainerClient containerClient;
    private final BlobContainerClient privateApplicationContainerClient;

    public AzureBlobStorageClient(BlobContainerClient containerClient) {
        this(containerClient, null);
    }

    public AzureBlobStorageClient(BlobContainerClient containerClient,
            BlobContainerClient privateApplicationContainerClient) {
        this.containerClient = containerClient;
        this.privateApplicationContainerClient = privateApplicationContainerClient;
    }

    @Override
    public String uploadImage(InputStream inputStream, long size, String mimeType, String originalFilename,
            String folder) {
        String extension = resolveExtension(mimeType, originalFilename);
        String blobName = folder + "/" + UUID.randomUUID() + extension;

        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(inputStream, size, true);

        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(mimeType);
        blobClient.setHttpHeaders(headers);

        String blobUrl = blobClient.getBlobUrl();
        log.info("Uploaded image to Azure Blob Storage: {}", blobUrl);
        return blobUrl;
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
            log.error("Failed to delete blob at URL {}: {}", blobUrl, e.getMessage(), e);
        }
    }

    @Override
    public String uploadPrivateApplicationFile(byte[] content, String mimeType, String extension) {
        BlobContainerClient privateContainer = requirePrivateApplicationContainer();
        String blobKey = EXPERT_APPLICATION_FOLDER + "/" + UUID.randomUUID() + extension;
        BlobClient blobClient = privateContainer.getBlobClient(blobKey);
        try {
            blobClient.upload(new ByteArrayInputStream(content), content.length, true);
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(mimeType));
            return blobKey;
        } catch (RuntimeException exception) {
            try { blobClient.deleteIfExists(); }
            catch (RuntimeException cleanupException) {
                log.warn("Could not clean up private expert application blob after upload failure");
            }
            throw exception;
        }
    }

    @Override
    public byte[] downloadPrivateApplicationFile(String blobKey) {
        BlobClient blobClient = requirePrivateApplicationContainer().getBlobClient(requireApplicationBlobKey(blobKey));
        try (InputStream input = blobClient.openInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read private application attachment", exception);
        }
    }

    @Override
    public void deletePrivateApplicationFile(String blobKey) {
        if (blobKey == null || blobKey.isBlank()) return;
        requirePrivateApplicationContainer().getBlobClient(requireApplicationBlobKey(blobKey)).deleteIfExists();
    }

    private BlobContainerClient requirePrivateApplicationContainer() {
        return Objects.requireNonNull(privateApplicationContainerClient,
                "Private application attachment storage is not configured");
    }

    private static String requireApplicationBlobKey(String blobKey) {
        if (blobKey == null || !blobKey.startsWith(EXPERT_APPLICATION_FOLDER + "/")
                || blobKey.contains("..") || blobKey.contains("\\")) {
            throw new IllegalArgumentException("Invalid private application blob key");
        }
        return blobKey;
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
            log.warn("Could not parse blob name from URL: {}", blobUrl);
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
