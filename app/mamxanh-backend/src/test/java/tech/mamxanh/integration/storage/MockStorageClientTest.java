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

        String url3 = client.uploadImage(new ByteArrayInputStream(data), data.length, "image/jpeg", "sample.jpg");
        assertThat(url3).endsWith(".jpg");

        String url4 = client.uploadImage(new ByteArrayInputStream(data), data.length, "application/octet-stream", "sample.bin");
        assertThat(url4).endsWith(".jpg");

        client.deleteImage(url1);
        assertThat(client.containsBlob(url1)).isFalse();

        // Edge cases for deleteImage
        client.deleteImage("https://non-existing-blob");
        client.deleteImage(null);
        client.deleteImage("");
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
    @DisplayName("StorageConfig never registers the test mock when connection string is blank")
    void storageConfigFallback() {
        StorageConfig config = new StorageConfig();
        StorageProperties properties = new StorageProperties("", "mamxanh-recipes");

        StorageClient client = config.storageClient(properties);
        assertThat(client).isInstanceOf(UnavailableStorageClient.class);
    }

    @Test
    @DisplayName("MockStorageClient stores private files defensively and supports deletion")
    void privateApplicationFilesAreCopiedAndDeleted() {
        MockStorageClient client = new MockStorageClient(null);
        byte[] input = {1, 2, 3};
        String key = client.uploadPrivateApplicationFile(input, "image/png", ".png");
        input[0] = 9;

        assertThat(key).startsWith("expert-applications/").endsWith(".png");
        assertThat(client.containsPrivateApplicationFile(key)).isTrue();
        byte[] downloaded = client.downloadPrivateApplicationFile(key);
        assertThat(downloaded).containsExactly(1, 2, 3);
        downloaded[1] = 9;
        assertThat(client.downloadPrivateApplicationFile(key)).containsExactly(1, 2, 3);

        client.deletePrivateApplicationFile(key);
        client.deletePrivateApplicationFile(null);
        assertThat(client.containsPrivateApplicationFile(key)).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.downloadPrivateApplicationFile(key))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
