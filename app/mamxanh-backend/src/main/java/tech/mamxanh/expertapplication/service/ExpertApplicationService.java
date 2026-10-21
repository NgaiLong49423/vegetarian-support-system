package tech.mamxanh.expertapplication.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tech.mamxanh.auth.service.ExpertPromotionService;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;
import tech.mamxanh.expertapplication.dto.ApplicationAttachmentContent;
import tech.mamxanh.expertapplication.repository.ExpertApplicationRepository;
import tech.mamxanh.integration.storage.StorageClient;
import tech.mamxanh.notification.service.NotificationService;

@Service
public class ExpertApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ExpertApplicationService.class);
    private final ExpertApplicationRepository applications;
    private final ExpertPromotionService promotion;
    private final NotificationService notifications;
    private final Clock clock;
    private final StorageClient storage;

    private static final long MAX_CERTIFICATE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_APPLICATION_AVATAR_BYTES = 2L * 1024 * 1024;

    @Autowired
    public ExpertApplicationService(ExpertApplicationRepository applications,
            ExpertPromotionService promotion, NotificationService notifications, Clock clock, StorageClient storage) {
        this.applications = applications; this.promotion = promotion;
        this.notifications = notifications; this.clock = clock; this.storage = storage;
    }

    public ExpertApplicationService(ExpertApplicationRepository applications,
            ExpertPromotionService promotion, NotificationService notifications, Clock clock) {
        this(applications, promotion, notifications, clock, null);
    }

    @Transactional
    public ExpertApplicationResponse submit(ExpertApplicationRequest request) {
        long userId = userId();
        promotion.requireActiveCustomer(userId);
        try {
            long id = applications.insert(userId, request, now());
            return applications.findById(id);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "EXPERT_APPLICATION_PENDING_EXISTS", "Bạn đã có đơn đang chờ xét duyệt.");
        }
    }

    @Transactional
    public ExpertApplicationResponse submitWithAttachments(ExpertApplicationRequest request,
            MultipartFile certificate, MultipartFile applicationAvatar) {
        long userId = userId();
        promotion.requireActiveCustomer(userId);
        var certificateFile = validateAttachment(certificate, AttachmentKind.CERTIFICATE);
        var avatarFile = validateAttachment(applicationAvatar, AttachmentKind.AVATAR);
        List<String> uploadedKeys = new ArrayList<>();
        boolean rollbackCleanupRegistered = false;
        try {
            String certificateKey = certificateFile == null ? null : upload(certificateFile, uploadedKeys);
            String avatarKey = avatarFile == null ? null : upload(avatarFile, uploadedKeys);
            rollbackCleanupRegistered = registerRollbackCleanup(uploadedKeys);
            long id = applications.insert(userId, request, certificateKey,
                    certificateFile == null ? null : certificateFile.contentType(), avatarKey,
                    avatarFile == null ? null : avatarFile.contentType(), now());
            return applications.findById(id);
        } catch (DataIntegrityViolationException exception) {
            if (!rollbackCleanupRegistered) deleteUploaded(uploadedKeys);
            throw new ApiException(HttpStatus.CONFLICT, "EXPERT_APPLICATION_PENDING_EXISTS",
                    "Bạn đã có đơn đang chờ xét duyệt.");
        } catch (RuntimeException exception) {
            if (!rollbackCleanupRegistered) deleteUploaded(uploadedKeys);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ApplicationAttachmentContent downloadAttachment(long applicationId, String kind) {
        if (!"certificate".equals(kind) && !"avatar".equals(kind)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ATTACHMENT_KIND", "Loại tệp đính kèm không hợp lệ.");
        }
        var attachment = applications.attachment(applicationId, kind);
        String extension = switch (attachment.contentType()) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Tệp đính kèm có định dạng không hợp lệ.");
        };
        return new ApplicationAttachmentContent(storage.downloadPrivateApplicationFile(attachment.blobKey()),
                attachment.contentType(), kind + extension);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpertApplicationResponse> ownHistory(int page, int size) {
        long userId = userId();
        return PageResponse.of(applications.page(userId, null, page, size), page, size, applications.count(userId, null));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpertApplicationResponse> adminList(String status, int page, int size) {
        return PageResponse.of(applications.page(null, status, page, size), page, size, applications.count(null, status));
    }

    @Transactional(readOnly = true)
    public ExpertApplicationResponse adminDetail(long id) { return applications.findById(id); }

    @Transactional
    public ExpertApplicationResponse approve(long id, String note) {
        long reviewer = userId();
        LocalDateTime now = now();
        if (applications.transitionPending(id, "APPROVED", note, reviewer, now) == 0) throw stale();
        long applicantId = applications.applicantId(id);
        promotion.promoteActiveCustomer(applicantId);
        notifications.expertApplicationDecision(applicantId, id, true, null);
        return applications.findById(id);
    }

    @Transactional
    public ExpertApplicationResponse reject(long id, String reason) {
        long reviewer = userId();
        LocalDateTime now = now();
        String cleanReason = reason.trim();
        if (applications.transitionPending(id, "REJECTED", cleanReason, reviewer, now) == 0) throw stale();
        long applicantId = applications.applicantId(id);
        notifications.expertApplicationDecision(applicantId, id, false, cleanReason);
        return applications.findById(id);
    }

    private long userId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try { return Long.parseLong(authentication.getName()); }
        catch (RuntimeException exception) { throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Cần đăng nhập."); }
    }
    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
    private static ApiException stale() { return new ApiException(HttpStatus.CONFLICT, "EXPERT_APPLICATION_STALE", "Đơn đã được xử lý hoặc không còn chờ duyệt."); }

    private String upload(ValidatedAttachment file, List<String> uploadedKeys) {
        String key = storage.uploadPrivateApplicationFile(file.content(), file.contentType(), file.extension());
        uploadedKeys.add(key);
        return key;
    }

    private boolean registerRollbackCleanup(List<String> uploadedKeys) {
        if (uploadedKeys.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) return false;
        List<String> keys = List.copyOf(uploadedKeys);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) deleteUploaded(keys);
            }
        });
        return true;
    }

    private void deleteUploaded(List<String> keys) {
        if (storage == null) return;
        keys.forEach(key -> {
            try { storage.deletePrivateApplicationFile(key); }
            catch (RuntimeException exception) {
                log.warn("Could not clean up private expert application attachment after submission failure");
            }
        });
    }

    private static ValidatedAttachment validateAttachment(MultipartFile file, AttachmentKind kind) {
        if (file == null) return null;
        if (file.isEmpty()) throw invalidFile(kind, "Tệp đính kèm không được để trống.");
        long maxBytes = kind == AttachmentKind.CERTIFICATE ? MAX_CERTIFICATE_BYTES : MAX_APPLICATION_AVATAR_BYTES;
        if (file.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    kind == AttachmentKind.CERTIFICATE ? "Tài liệu chứng chỉ tối đa 5 MB." : "Ảnh đại diện của đơn tối đa 2 MB.");
        }
        byte[] content;
        try { content = file.getBytes(); }
        catch (IOException exception) { throw invalidFile(kind, "Không thể đọc tệp đính kèm."); }
        String mimeType = detectedMimeType(content);
        boolean allowed = kind == AttachmentKind.CERTIFICATE
                ? List.of("application/pdf", "image/jpeg", "image/png").contains(mimeType)
                : List.of("image/jpeg", "image/png", "image/webp").contains(mimeType);
        if (!allowed || !mimeType.equalsIgnoreCase(file.getContentType())) {
            throw invalidFile(kind, kind == AttachmentKind.CERTIFICATE
                    ? "Tài liệu phải là PDF, JPEG hoặc PNG hợp lệ."
                    : "Ảnh đại diện phải là JPEG, PNG hoặc WebP hợp lệ.");
        }
        String extension = switch (mimeType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw invalidFile(kind, "Định dạng tệp không được hỗ trợ.");
        };
        return new ValidatedAttachment(content, mimeType, extension);
    }

    private static String detectedMimeType(byte[] bytes) {
        if (startsWith(bytes, 0, (byte) '%', (byte) 'P', (byte) 'D', (byte) 'F', (byte) '-')) return "application/pdf";
        if (startsWith(bytes, 0, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)) return "image/jpeg";
        if (startsWith(bytes, 0, (byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A)) return "image/png";
        if (startsWith(bytes, 0, (byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F')
                && startsWith(bytes, 8, (byte) 'W', (byte) 'E', (byte) 'B', (byte) 'P')) return "image/webp";
        return "";
    }

    private static boolean startsWith(byte[] bytes, int offset, byte... signature) {
        if (bytes.length < offset + signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if (bytes[offset + index] != signature[index]) return false;
        }
        return true;
    }

    private static ApiException invalidFile(AttachmentKind kind, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST,
                kind == AttachmentKind.CERTIFICATE ? "EXPERT_APPLICATION_CERTIFICATE_INVALID" : "EXPERT_APPLICATION_AVATAR_INVALID",
                message);
    }

    private enum AttachmentKind { CERTIFICATE, AVATAR }
    private record ValidatedAttachment(byte[] content, String contentType, String extension) { }
}
