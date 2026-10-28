package tech.mamxanh.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.storage.StorageClient;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.repository.IngredientRepository;

@ExtendWith(MockitoExtension.class)
class IngredientImageServiceTest {

    @Mock private IngredientRepository ingredientRepository;
    @Mock private StorageClient storageClient;

    private IngredientImageService service;
    private IngredientEntity ingredient;

    @BeforeEach
    void setUp() {
        service = new IngredientImageService(ingredientRepository, storageClient);
        ingredient = new IngredientEntity("Chuối", "Trái cây", "USDA", null, LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(ingredient, "id", 7L);
        when(ingredientRepository.findById(7L)).thenReturn(Optional.of(ingredient));
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void uploadsSupportedFormatsAndUsesDetectedContentTypeAndExtension() {
        stubUploadUrl();
        List<MockMultipartFile> files = List.of(
                file("photo.jpg", "image/jpeg", (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0),
                file("photo.png", "image/png", (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A),
                file("photo.webp", "image/webp", 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'));

        for (MockMultipartFile upload : files) {
            var response = service.upload(7L, upload);
            assertThat(response.imageUrl()).isEqualTo("https://blob/ingredients/new.png");
            verify(storageClient).uploadImage(any(), eq((long) upload.getSize()), eq(upload.getContentType()),
                    eq("ingredient" + extension(upload)), eq(StorageClient.INGREDIENT_FOLDER));
        }
        assertThat(ingredient.getImageUrl()).isEqualTo("https://blob/ingredients/new.png");
    }

    @Test
    void rejectsMissingIngredientAndInvalidFilesBeforeStorageUpload() {
        when(ingredientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upload(99L, file("photo.png", "image/png", (byte) 1)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertAppError(() -> service.upload(7L, null), ErrorCode.VALIDATION_FAILED);
        assertAppError(() -> service.upload(7L, new MockMultipartFile("file", "empty.png", "image/png", new byte[0])),
                ErrorCode.VALIDATION_FAILED);
        assertAppError(() -> service.upload(7L, new MockMultipartFile("file", "large.png", "image/png",
                new byte[(int) IngredientImageService.MAX_IMAGE_BYTES + 1])), ErrorCode.FILE_TOO_LARGE);
        assertAppError(() -> service.upload(7L, file("fake.png", "image/png", (byte) 1)),
                ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        assertAppError(() -> service.upload(7L, file("mismatch.jpg", "image/png", (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)),
                ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        verify(storageClient, never()).uploadImage(any(), anyLong(), any(), any(), any());
    }

    @Test
    void wrapsFileReadErrorsAndDeletesReplacedImagesOutsideTransactions() throws IOException {
        MultipartFile unreadable = mock(MultipartFile.class);
        when(unreadable.isEmpty()).thenReturn(false);
        when(unreadable.getSize()).thenReturn(4L);
        when(unreadable.getBytes()).thenThrow(new IOException("read failed"));
        assertAppError(() -> service.upload(7L, unreadable), ErrorCode.INTERNAL_ERROR);

        stubUploadUrl();
        ingredient.setImageUrl("https://blob/ingredients/old.webp");
        service.upload(7L, file("new.png", "image/png", (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A));
        verify(storageClient).deleteImage("https://blob/ingredients/old.webp");
    }

    @Test
    void defersOldImageDeletionUntilCommitAndDeletesNewImageAfterRollback() {
        stubUploadUrl();
        ingredient.setImageUrl("https://blob/ingredients/old.webp");
        TransactionSynchronizationManager.initSynchronization();
        service.upload(7L, file("new.png", "image/png", (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A));
        List<TransactionSynchronization> committed = TransactionSynchronizationManager.getSynchronizations();
        committed.forEach(TransactionSynchronization::afterCommit);
        committed.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        verify(storageClient).deleteImage("https://blob/ingredients/old.webp");
        TransactionSynchronizationManager.clearSynchronization();

        TransactionSynchronizationManager.initSynchronization();
        service.upload(7L, file("rollback.png", "image/png", (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storageClient).deleteImage("https://blob/ingredients/new.png");
    }

    private static MockMultipartFile file(String name, String contentType, int... values) {
        byte[] bytes = new byte[values.length];
        for (int index = 0; index < values.length; index++) bytes[index] = (byte) values[index];
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private void stubUploadUrl() {
        when(storageClient.uploadImage(any(), anyLong(), any(), any(), any()))
                .thenReturn("https://blob/ingredients/new.png");
    }

    private static String extension(MockMultipartFile file) {
        return switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Unexpected test image type");
        };
    }

    private static void assertAppError(ThrowingCallable upload, ErrorCode expected) {
        assertThatThrownBy(upload).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(expected));
    }
}
