package tech.mamxanh.integration.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Azure Blob Storage integration (FR-14).
 */
@ConfigurationProperties(prefix = "azure.storage")
public record StorageProperties(
        String connectionString,
        String containerName
) {
    public StorageProperties {
        if (!StringUtils.hasText(containerName)) {
            containerName = "mamxanh-recipes";
        }
    }

    public boolean hasConnectionString() {
        return StringUtils.hasText(connectionString);
    }
}
