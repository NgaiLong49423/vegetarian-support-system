package tech.mamxanh.recipe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.storage.StorageClient;
import tech.mamxanh.recipe.dto.request.RecipeMediaItemRequest;
import tech.mamxanh.recipe.dto.request.UpdateRecipeMediaRequest;
import tech.mamxanh.recipe.dto.response.RecipeMediaResponse;
import tech.mamxanh.recipe.dto.response.UploadImageResponse;
import tech.mamxanh.recipe.entity.RecipeMediaEntity;
import tech.mamxanh.recipe.repository.RecipeMediaRepository;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeMediaServiceTest {

    @Mock
    private StorageClient storageClient;

    @Mock
    private RecipeMediaRepository recipeMediaRepository;

    private RecipeMediaService recipeMediaService;

    @BeforeEach
    void setUp() {
        recipeMediaService = new RecipeMediaService(storageClient, recipeMediaRepository);
    }

    @Test
    @DisplayName("AC-14.1 & AC-14.2: Upload valid JPEG image under 5MB succeeds")
    void uploadImage_success() {
        byte[] content = new byte[1024]; // 1 KB
        MockMultipartFile file = new MockMultipartFile("file", "recipe-dish.jpg", "image/jpeg", content);

        when(storageClient.uploadImage(any(InputStream.class), eq((long) content.length), eq("image/jpeg"), eq("recipe-dish.jpg")))
                .thenReturn("https://mamxanh.blob.core.windows.net/mamxanh-recipes/recipes/recipe-dish.jpg");

        UploadImageResponse response = recipeMediaService.uploadImage(file);

        assertThat(response.blobUrl()).isEqualTo("https://mamxanh.blob.core.windows.net/mamxanh-recipes/recipes/recipe-dish.jpg");
        assertThat(response.mimeType()).isEqualTo("image/jpeg");
        assertThat(response.sizeBytes()).isEqualTo(content.length);
    }

    @Test
    @DisplayName("AC-14.1: Reject image larger than 5MB with FILE_TOO_LARGE")
    void uploadImage_fileTooLarge() {
        byte[] content = new byte[(int) (RecipeMediaService.MAX_FILE_SIZE_BYTES + 1)];
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", content);

        assertThatThrownBy(() -> recipeMediaService.uploadImage(file))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).errorCode()).isEqualTo(ErrorCode.FILE_TOO_LARGE));
    }

    @Test
    @DisplayName("AC-14.1: Reject unsupported MIME type (e.g. image/gif) with UNSUPPORTED_IMAGE_TYPE")
    void uploadImage_unsupportedMimeType() {
        MockMultipartFile file = new MockMultipartFile("file", "anim.gif", "image/gif", new byte[100]);

        assertThatThrownBy(() -> recipeMediaService.uploadImage(file))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).errorCode()).isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE));
    }

    @Test
    @DisplayName("AC-14.1: Reject empty file with VALIDATION_FAILED")
    void uploadImage_emptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> recipeMediaService.uploadImage(file))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    @DisplayName("AC-14.2 & AC-14.3: Set media with 3 images and exactly 1 cover succeeds")
    void setRecipeMedia_success() {
        Long recipeId = 100L;
        List<RecipeMediaItemRequest> items = List.of(
                new RecipeMediaItemRequest("https://blob/1.jpg", "image/jpeg", 1, true),
                new RecipeMediaItemRequest("https://blob/2.png", "image/png", 2, false),
                new RecipeMediaItemRequest("https://blob/3.webp", "image/webp", 3, false)
        );
        UpdateRecipeMediaRequest request = new UpdateRecipeMediaRequest(items);

        when(recipeMediaRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecipeMediaResponse> responses = recipeMediaService.setRecipeMedia(recipeId, request);

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).isCover()).isTrue();
        assertThat(responses.get(1).isCover()).isFalse();
        verify(recipeMediaRepository).deleteByRecipeId(recipeId);
    }

    @Test
    @DisplayName("AC-14.1: Reject more than 5 images with MAX_RECIPE_MEDIA_EXCEEDED")
    void setRecipeMedia_exceedsFiveImages() {
        Long recipeId = 100L;
        List<RecipeMediaItemRequest> items = List.of(
                new RecipeMediaItemRequest("https://blob/1.jpg", "image/jpeg", 1, true),
                new RecipeMediaItemRequest("https://blob/2.jpg", "image/jpeg", 2, false),
                new RecipeMediaItemRequest("https://blob/3.jpg", "image/jpeg", 3, false),
                new RecipeMediaItemRequest("https://blob/4.jpg", "image/jpeg", 4, false),
                new RecipeMediaItemRequest("https://blob/5.jpg", "image/jpeg", 5, false),
                new RecipeMediaItemRequest("https://blob/6.jpg", "image/jpeg", 6, false)
        );
        UpdateRecipeMediaRequest request = new UpdateRecipeMediaRequest(items);

        assertThatThrownBy(() -> recipeMediaService.setRecipeMedia(recipeId, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).errorCode()).isEqualTo(ErrorCode.MAX_RECIPE_MEDIA_EXCEEDED));
    }

    @Test
    @DisplayName("AC-14.3: Reject images with 0 cover images with INVALID_COVER_IMAGE_CONFIGURATION")
    void setRecipeMedia_zeroCovers() {
        Long recipeId = 100L;
        List<RecipeMediaItemRequest> items = List.of(
                new RecipeMediaItemRequest("https://blob/1.jpg", "image/jpeg", 1, false),
                new RecipeMediaItemRequest("https://blob/2.jpg", "image/jpeg", 2, false)
        );
        UpdateRecipeMediaRequest request = new UpdateRecipeMediaRequest(items);

        assertThatThrownBy(() -> recipeMediaService.setRecipeMedia(recipeId, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).errorCode()).isEqualTo(ErrorCode.INVALID_COVER_IMAGE_CONFIGURATION));
    }

    @Test
    @DisplayName("AC-14.3: Reject images with multiple cover images with INVALID_COVER_IMAGE_CONFIGURATION")
    void setRecipeMedia_multipleCovers() {
        Long recipeId = 100L;
        List<RecipeMediaItemRequest> items = List.of(
                new RecipeMediaItemRequest("https://blob/1.jpg", "image/jpeg", 1, true),
                new RecipeMediaItemRequest("https://blob/2.jpg", "image/jpeg", 2, true)
        );
        UpdateRecipeMediaRequest request = new UpdateRecipeMediaRequest(items);

        assertThatThrownBy(() -> recipeMediaService.setRecipeMedia(recipeId, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).errorCode()).isEqualTo(ErrorCode.INVALID_COVER_IMAGE_CONFIGURATION));
    }

    @Test
    @DisplayName("Validation: Reject duplicate displayOrder")
    void setRecipeMedia_duplicateDisplayOrder() {
        Long recipeId = 100L;
        List<RecipeMediaItemRequest> items = List.of(
                new RecipeMediaItemRequest("https://blob/1.jpg", "image/jpeg", 1, true),
                new RecipeMediaItemRequest("https://blob/2.jpg", "image/jpeg", 1, false)
        );
        UpdateRecipeMediaRequest request = new UpdateRecipeMediaRequest(items);

        assertThatThrownBy(() -> recipeMediaService.setRecipeMedia(recipeId, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    @DisplayName("BR-20: Empty media items list is allowed for 0 images")
    void setRecipeMedia_emptyList() {
        Long recipeId = 100L;
        UpdateRecipeMediaRequest request = new UpdateRecipeMediaRequest(List.of());

        List<RecipeMediaResponse> responses = recipeMediaService.setRecipeMedia(recipeId, request);

        assertThat(responses).isEmpty();
        verify(recipeMediaRepository).deleteByRecipeId(recipeId);
    }

    @Test
    @DisplayName("deleteRecipeMedia deletes from blob storage and database")
    void deleteRecipeMedia() {
        Long recipeId = 100L;
        RecipeMediaEntity m1 = new RecipeMediaEntity(recipeId, "https://blob/1.jpg", "image/jpeg", 1, true);
        RecipeMediaEntity m2 = new RecipeMediaEntity(recipeId, "https://blob/2.png", "image/png", 2, false);

        when(recipeMediaRepository.findByRecipeIdOrderByDisplayOrderAsc(recipeId)).thenReturn(List.of(m1, m2));

        recipeMediaService.deleteRecipeMedia(recipeId);

        verify(storageClient).deleteImage("https://blob/1.jpg");
        verify(storageClient).deleteImage("https://blob/2.png");
        verify(recipeMediaRepository).deleteByRecipeId(recipeId);
    }
}
