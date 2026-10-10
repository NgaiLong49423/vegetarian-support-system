package tech.mamxanh.auth.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import tech.mamxanh.auth.dto.request.UpdateProfileRequest;
import tech.mamxanh.auth.dto.response.MemberProfileResponse;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.MemberProfileEntity;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.repository.MemberProfileRepository;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.storage.StorageClient;

/**
 * FR-23 member profile: UC-23.2 public profile (Q55, Q56) and UC-23.3 own profile settings with
 * name, bio (Q58) and avatar (AC-23.5, AC-23.7, Q54). The account always comes from the session.
 */
@Service
public class MemberProfileService {

    static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter JOINED_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MemberProfileRepository repository;
    private final CurrentUserService currentUserService;
    private final StorageClient storageClient;
    private final Clock clock;

    public MemberProfileService(MemberProfileRepository repository, CurrentUserService currentUserService,
            StorageClient storageClient, Clock clock) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.storageClient = storageClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse getPublicProfile(long userId) {
        currentUserService.requirePublicMember(userId);
        return response(repository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_PROFILE_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse getOwnProfile() {
        return response(currentMember());
    }

    @Transactional
    public MemberProfileResponse updateOwnProfile(UpdateProfileRequest request) {
        MemberProfileEntity profile = currentMember();
        profile.updateProfile(request.displayName(), request.bio(), LocalDateTime.now(clock));
        return response(profile);
    }

    /**
     * Stores a JPEG, PNG or WebP image of at most 2 MB. The type is read from the file's first
     * bytes and must match the declared content type, so a renamed file is refused. The previous
     * avatar is removed from storage only when it was uploaded here and only after the commit.
     */
    @Transactional
    public MemberProfileResponse uploadAvatar(MultipartFile file) {
        MemberProfileEntity profile = currentMember();
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn ảnh đại diện.");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE, "Ảnh đại diện tối đa 2 MB.");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể đọc tệp ảnh đại diện.");
        }
        ImageType type = ImageType.detect(content);
        if (type == null || !type.mimeType().equalsIgnoreCase(file.getContentType())) {
            throw new AppException(ErrorCode.UNSUPPORTED_IMAGE_TYPE,
                    "Ảnh đại diện phải là tệp JPEG, PNG hoặc WebP hợp lệ.");
        }
        String url = storageClient.uploadImage(new ByteArrayInputStream(content), content.length, type.mimeType(),
                "avatar" + type.extension(), StorageClient.AVATAR_FOLDER);
        String previous = profile.getAvatarUrl();
        profile.changeAvatar(url, LocalDateTime.now(clock));
        if (previous != null && previous.contains("/" + StorageClient.AVATAR_FOLDER + "/")) {
            deleteAfterCommit(previous);
        }
        return response(profile);
    }

    private void deleteAfterCommit(String blobUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storageClient.deleteImage(blobUrl);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storageClient.deleteImage(blobUrl);
            }
        });
    }

    private MemberProfileEntity currentMember() {
        long userId = currentUserService.requireAuthenticatedUserId();
        MemberProfileEntity profile = repository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (profile.getRole() == Role.ADMIN || profile.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.MEMBER_ACCESS_REQUIRED,
                    "Chỉ Member có tài khoản hoạt động mới có hồ sơ cá nhân.");
        }
        return profile;
    }

    private static MemberProfileResponse response(MemberProfileEntity profile) {
        String joinedMonth = profile.getCreatedAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM)
                .format(JOINED_MONTH);
        return new MemberProfileResponse(profile.getId(), profile.getDisplayName(), profile.getAvatarUrl(),
                profile.getBio(), joinedMonth);
    }

    /** Image formats accepted for avatars, recognised by their file signature. */
    enum ImageType {
        JPEG("image/jpeg", ".jpg"), PNG("image/png", ".png"), WEBP("image/webp", ".webp");

        private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

        private final String mimeType;
        private final String extension;

        ImageType(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }

        String mimeType() {
            return mimeType;
        }

        String extension() {
            return extension;
        }

        static ImageType detect(byte[] content) {
            if (startsWith(content, 0, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)) {
                return JPEG;
            }
            if (startsWith(content, 0, PNG_SIGNATURE)) {
                return PNG;
            }
            if (startsWith(content, 0, (byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F')
                    && startsWith(content, 8, (byte) 'W', (byte) 'E', (byte) 'B', (byte) 'P')) {
                return WEBP;
            }
            return null;
        }

        private static boolean startsWith(byte[] content, int offset, byte... signature) {
            if (content.length < offset + signature.length) {
                return false;
            }
            for (int index = 0; index < signature.length; index++) {
                if (content[offset + index] != signature[index]) {
                    return false;
                }
            }
            return true;
        }
    }
}
