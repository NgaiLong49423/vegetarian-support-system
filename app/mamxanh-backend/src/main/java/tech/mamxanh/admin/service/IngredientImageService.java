package tech.mamxanh.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tech.mamxanh.admin.dto.IngredientImageResponse;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.storage.StorageClient;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.repository.IngredientRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** Uploads and persists the administrator-managed ingredient reference image. */
@Service
public class IngredientImageService {

    static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;

    private final IngredientRepository ingredientRepository;
    private final StorageClient storageClient;

    public IngredientImageService(IngredientRepository ingredientRepository, StorageClient storageClient) {
        this.ingredientRepository = ingredientRepository;
        this.storageClient = storageClient;
    }

    @Transactional
    public IngredientImageResponse upload(long ingredientId, MultipartFile file) {
        IngredientEntity ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INGREDIENT_NOT_FOUND",
                        "Không tìm thấy nguyên liệu."));
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn ảnh minh họa nguyên liệu.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE, "Ảnh nguyên liệu tối đa 2 MB.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể đọc tệp ảnh nguyên liệu.");
        }
        ImageType type = ImageType.detect(content);
        if (type == null || !StringUtils.hasText(file.getContentType())
                || !type.mimeType.equalsIgnoreCase(file.getContentType())) {
            throw new AppException(ErrorCode.UNSUPPORTED_IMAGE_TYPE,
                    "Ảnh nguyên liệu phải là tệp JPEG, PNG hoặc WebP hợp lệ.");
        }

        String url = storageClient.uploadImage(new ByteArrayInputStream(content), content.length, type.mimeType,
                "ingredient" + type.extension, StorageClient.INGREDIENT_FOLDER);
        String previous = ingredient.getImageUrl();
        ingredient.setImageUrl(url);
        scheduleCleanup(url, previous);
        return new IngredientImageResponse(ingredient.getId(), url);
    }

    private void scheduleCleanup(String uploadedUrl, String previousUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deletePrevious(previousUrl);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deletePrevious(previousUrl);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storageClient.deleteImage(uploadedUrl);
                }
            }
        });
    }

    private void deletePrevious(String previousUrl) {
        if (previousUrl != null && previousUrl.contains("/" + StorageClient.INGREDIENT_FOLDER + "/")) {
            storageClient.deleteImage(previousUrl);
        }
    }

    private enum ImageType {
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png"),
        WEBP("image/webp", ".webp");

        private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

        private final String mimeType;
        private final String extension;

        ImageType(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }

        private static ImageType detect(byte[] content) {
            if (startsWith(content, 0, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)) return JPEG;
            if (startsWith(content, 0, PNG_SIGNATURE)) return PNG;
            if (startsWith(content, 0, (byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F')
                    && startsWith(content, 8, (byte) 'W', (byte) 'E', (byte) 'B', (byte) 'P')) return WEBP;
            return null;
        }

        private static boolean startsWith(byte[] content, int offset, byte... signature) {
            if (content.length < offset + signature.length) return false;
            for (int index = 0; index < signature.length; index++) {
                if (content[offset + index] != signature[index]) return false;
            }
            return true;
        }
    }
}
