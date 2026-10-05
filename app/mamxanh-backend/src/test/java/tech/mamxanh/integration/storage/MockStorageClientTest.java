package tech.mamxanh.integration.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MockStorageClientTest {

    @Test
    @DisplayName("MockStorageClient generates deterministic URL and tracks stored blobs")
    void uploadAndTrackMockBlobs() {
        MockStorageClient client = new MockStorageClient("mamxanh-recipes");

        assertThat(client.isCloudStorageConfigured()).isFalse();

        byte[] data = "fake-data".getBytes();
        String url1 = client.uploadImage(new ByteArrayInputStream(data), data.length, "image/png", "sample.png");
        assertThat(url1).startsWith("https://mamxanh.blob.core.windows.net/mamxanh-recipes/recipes/");
        assertThat(url1).endsWith(".png");
        assertThat(client.containsBlob(url1)).isTrue();

        String url2 = client.uploadImage(new ByteArrayInputStream(data), data.length, "image/webp", "sample.webp");
        assertThat(url2).endsWith(".webp");

        client.deleteImage(url1);
        assertThat(client.containsBlob(url1)).isFalse();
    }

    @Test
    @DisplayName("StorageProperties handles defaults and connection string presence")
    void storageProperties() {
        StorageProperties prop1 = new StorageProperties("", null);
        assertThat(prop1.containerName()).isEqualTo("mamxanh-recipes");
        assertThat(prop1.hasConnectionString()).isFalse();

        StorageProperties prop2 = new StorageProperties("DefaultEndpointsProtocol=https;...", "custom-container");
        assertThat(prop2.containerName()).isEqualTo("custom-container");
        assertThat(prop2.hasConnectionString()).isTrue();
    }

    @Test
    @DisplayName("StorageConfig returns MockStorageClient when connection string is blank")
    void storageConfigFallback() {
        StorageConfig config = new StorageConfig();
        StorageProperties properties = new StorageProperties("", "mamxanh-recipes");

        StorageClient client = config.storageClient(properties);
        assertThat(client).isInstanceOf(MockStorageClient.class);
    }
}
