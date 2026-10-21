package tech.mamxanh.integration.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Azure Blob Storage integration (FR-14).
 */
@ConfigurationProperties(prefix = "azure.storage")
public record StorageProperties(
        String connectionString,
        String containerName,
        String expertApplicationContainerName
) {
    public StorageProperties(String connectionString, String containerName) {
        this(connectionString, containerName, null);
    }

    @ConstructorBinding
    public StorageProperties {
        if (!StringUtils.hasText(containerName)) {
            containerName = "mamxanh-recipes";
        }
        if (!StringUtils.hasText(expertApplicationContainerName)) {
            expertApplicationContainerName = "mamxanh-expert-applications-private";
        }
    }

    public boolean hasConnectionString() {
        return StringUtils.hasText(connectionString);
    }
}
