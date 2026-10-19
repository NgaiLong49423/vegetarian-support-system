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
 * or rejects uploads through {@link UnavailableStorageClient} without taking down unrelated features.
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
                return new AzureBlobStorageClient(containerClient);
            } catch (Exception e) {
                log.warn("Azure Blob Storage initialization failed ({}); uploads will be rejected.", e.getClass().getSimpleName());
                return new UnavailableStorageClient();
            }
        }

        log.info("AZURE_STORAGE_CONNECTION_STRING is not set; image uploads will be rejected.");
        return new UnavailableStorageClient();
    }
}
