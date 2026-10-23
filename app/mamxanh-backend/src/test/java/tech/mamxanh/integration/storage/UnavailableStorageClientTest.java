package tech.mamxanh.integration.storage;

import static org.assertj.core.api.Assertions.*;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

class UnavailableStorageClientTest {
    @Test void refusesToInventUploadUrlsWhileKeepingDeletionBestEffort() {
        StorageClient client = new UnavailableStorageClient();
        assertThat(client.isCloudStorageConfigured()).isFalse();
        assertThatThrownBy(() -> client.uploadImage(new ByteArrayInputStream(new byte[]{1}), 1, "image/png", "test.png"))
                .isInstanceOf(AppException.class).satisfies(error -> {
                    assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.MEDIA_STORAGE_UNAVAILABLE);
                    assertThat(error.getMessage()).doesNotContain("AccountKey", "ConnectionString", "https://");
                });
        assertThatCode(() -> { client.deleteImage(null); client.deleteImage(""); client.deleteImage("https://example.test/image.png"); })
                .doesNotThrowAnyException();
    }
}
