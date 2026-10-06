package tech.mamxanh.integration.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureBlobStorageClientTest {

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    @Test
    @DisplayName("uploadImage successfully uploads stream and returns blob URL")
    void uploadImage_success() {
        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("https://testaccount.blob.core.windows.net/mamxanh-recipes/recipes/test-image.jpg");

        AzureBlobStorageClient client = new AzureBlobStorageClient(containerClient);

        byte[] content = "fake-image-bytes".getBytes();
        InputStream is = new ByteArrayInputStream(content);

        String resultUrl = client.uploadImage(is, content.length, "image/jpeg", "my-food.jpg");

        assertThat(resultUrl).isEqualTo("https://testaccount.blob.core.windows.net/mamxanh-recipes/recipes/test-image.jpg");
        assertThat(client.isCloudStorageConfigured()).isTrue();

        verify(blobClient).upload(any(InputStream.class), anyLong(), anyBoolean());
        ArgumentCaptor<BlobHttpHeaders> headersCaptor = ArgumentCaptor.forClass(BlobHttpHeaders.class);
        verify(blobClient).setHttpHeaders(headersCaptor.capture());
        assertThat(headersCaptor.getValue().getContentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("uploadImage resolves extensions for png, webp, jpeg and fallbacks")
    void uploadImage_resolveExtensions() {
        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("https://testaccount.blob.core.windows.net/mamxanh-recipes/recipes/test.png");

        AzureBlobStorageClient client = new AzureBlobStorageClient(containerClient);
        byte[] content = "fake".getBytes();

        client.uploadImage(new ByteArrayInputStream(content), content.length, "image/png", "test.png");
        client.uploadImage(new ByteArrayInputStream(content), content.length, "image/webp", "test.webp");
        client.uploadImage(new ByteArrayInputStream(content), content.length, "image/jpeg", "test.jpeg");
        client.uploadImage(new ByteArrayInputStream(content), content.length, "image/png", "unknown.xyz");
        client.uploadImage(new ByteArrayInputStream(content), content.length, "image/webp", null);
        client.uploadImage(new ByteArrayInputStream(content), content.length, "application/octet-stream", "no-extension");
    }

    @Test
    @DisplayName("deleteImage extracts blob name and calls deleteIfExists")
    void deleteImage_success() {
        when(containerClient.getBlobContainerName()).thenReturn("mamxanh-recipes");
        when(containerClient.getBlobClient("recipes/uuid-123.jpg")).thenReturn(blobClient);
        when(blobClient.deleteIfExists()).thenReturn(true);

        AzureBlobStorageClient client = new AzureBlobStorageClient(containerClient);

        client.deleteImage("https://testaccount.blob.core.windows.net/mamxanh-recipes/recipes/uuid-123.jpg");

        verify(blobClient).deleteIfExists();
    }

    @Test
    @DisplayName("deleteImage handles deletion returning false and exceptions gracefully")
    void deleteImage_edgeCases() {
        when(containerClient.getBlobContainerName()).thenReturn("mamxanh-recipes");
        when(containerClient.getBlobClient("recipes/uuid-notfound.jpg")).thenReturn(blobClient);
        when(blobClient.deleteIfExists()).thenReturn(false);

        AzureBlobStorageClient client = new AzureBlobStorageClient(containerClient);
        client.deleteImage("https://testaccount.blob.core.windows.net/mamxanh-recipes/recipes/uuid-notfound.jpg");

        // Path fallback without container prefix
        when(containerClient.getBlobClient("other-container/recipes/fallback.jpg")).thenReturn(blobClient);
        client.deleteImage("https://testaccount.blob.core.windows.net/other-container/recipes/fallback.jpg");

        // Exception during delete
        when(containerClient.getBlobClient("recipes/throw.jpg")).thenThrow(new RuntimeException("Azure network error"));
        client.deleteImage("https://testaccount.blob.core.windows.net/mamxanh-recipes/recipes/throw.jpg");

        // Invalid URI string
        client.deleteImage("htt p://invalid uri");
    }

    @Test
    @DisplayName("deleteImage with null or empty URL does not throw")
    void deleteImage_emptyUrl_noException() {
        AzureBlobStorageClient client = new AzureBlobStorageClient(containerClient);
        client.deleteImage(null);
        client.deleteImage("");
    }
}
