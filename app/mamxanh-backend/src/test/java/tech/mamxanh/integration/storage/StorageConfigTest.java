package tech.mamxanh.integration.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigTest {

    @Test
    @DisplayName("storageClient returns MockStorageClient when connection string is missing or invalid")
    void storageClient_mockFallback() {
        StorageConfig config = new StorageConfig();

        // Empty connection string
        StorageProperties emptyProps = new StorageProperties("", "mamxanh-recipes");
        StorageClient client1 = config.storageClient(emptyProps);
        assertThat(client1).isInstanceOf(MockStorageClient.class);

        // Invalid connection string (triggers catch block)
        StorageProperties invalidProps = new StorageProperties("invalid-connection-string", "mamxanh-recipes");
        StorageClient client2 = config.storageClient(invalidProps);
        assertThat(client2).isInstanceOf(MockStorageClient.class);
    }
}
