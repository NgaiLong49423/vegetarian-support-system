package tech.mamxanh.integration.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Azure Blob Storage client (FR-14).
 * Creates {@link AzureBlobStorageClient} when connection string is configured,
 * or gracefully falls back to {@link MockStorageClient} for local development.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    public StorageClient storageClient(StorageProperties properties) {
        if (properties.hasConnectionString()) {
            try {
                log.info("Initializing Azure Blob Storage client with container: {}", properties.containerName());
                BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                        .connectionString(properties.connectionString())
                        .buildClient();

                BlobContainerClient containerClient = serviceClient.getBlobContainerClient(properties.containerName());
                if (!containerClient.exists()) {
                    containerClient.create();
                    log.info("Created Azure Blob Storage container: {}", properties.containerName());
                }
                BlobContainerClient privateApplicationContainer = serviceClient
                        .getBlobContainerClient(properties.expertApplicationContainerName());
                if (!privateApplicationContainer.exists()) {
                    privateApplicationContainer.create();
                    log.info("Created private expert application storage container: {}",
                            properties.expertApplicationContainerName());
                }
                if (privateApplicationContainer.getAccessPolicy().getBlobAccessType() != null) {
                    throw new PrivateContainerPublicAccessException();
                }
                return new AzureBlobStorageClient(containerClient, privateApplicationContainer);
            } catch (PrivateContainerPublicAccessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Failed to initialize Azure Blob Storage client; falling back to MockStorageClient: {}", e.getMessage());
                return new MockStorageClient(properties.containerName());
            }
        }

        log.info("AZURE_STORAGE_CONNECTION_STRING is not set. Using MockStorageClient for local development.");
        return new MockStorageClient(properties.containerName());
    }

    static final class PrivateContainerPublicAccessException extends IllegalStateException {
        PrivateContainerPublicAccessException() {
            super("Expert application storage container must not allow public access");
        }
    }
}
