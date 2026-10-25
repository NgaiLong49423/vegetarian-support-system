package tech.mamxanh.expertapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import tech.mamxanh.auth.service.ExpertPromotionService;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;
import tech.mamxanh.expertapplication.dto.ExpertApplicationAttachment;
import tech.mamxanh.expertapplication.dto.ApplicationAttachmentContent;
import tech.mamxanh.expertapplication.repository.ExpertApplicationRepository;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;
import tech.mamxanh.notification.service.NotificationService;
import tech.mamxanh.integration.storage.StorageClient;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ExpertApplicationServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 12, 0);
    @Mock private ExpertApplicationRepository applications;
    @Mock private ExpertPromotionService promotion;
    @Mock private NotificationService notifications;
    @Mock private StorageClient storage;
    private ExpertApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ExpertApplicationService(applications, promotion, notifications,
                Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC), storage);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("42", "test", List.of()));
    }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void submitRequiresEligibleCustomerAndReturnsCreatedApplication() {
        var request = new ExpertApplicationRequest("  More than twenty chars experience  ", "VEGAN",
                "  A sample recipe summary with enough details.  ", "  https://example.org  ");
        var expected = response("PENDING", null);
        when(applications.insert(42, request, NOW)).thenReturn(9L);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(service.submit(request)).isSameAs(expected);

        verify(promotion).requireActiveCustomer(42);
        verify(applications).insert(42, request, NOW);
    }

    @Test
    void legacySubmissionConstructorKeepsTextOnlyApplicationsAvailable() {
        var request = request();
        var expected = response("PENDING", null);
        var legacyService = new ExpertApplicationService(applications, promotion, notifications,
                Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC));
        when(applications.insert(42, request, NOW)).thenReturn(9L);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(legacyService.submit(request)).isSameAs(expected);
    }

    @Test
    void duplicatePendingApplicationBecomesConflict() {
        var request = new ExpertApplicationRequest("  More than twenty chars experience  ", "VEGAN",
                "  A sample recipe summary with enough details.  ", null);
        when(applications.insert(42, request, NOW)).thenThrow(new DataIntegrityViolationException("unique pending"));

        assertThatThrownBy(() -> service.submit(request)).isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void submitWithAttachmentsStoresOnlyPrivateKeysOnApplication() {
        var request = new ExpertApplicationRequest("More than twenty chars experience", "VEGAN",
                "A sample recipe summary with enough details.", null);
        byte[] pdf = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        var certificate = new MockMultipartFile("certificate", "credential.pdf", "application/pdf", pdf);
        var avatar = new MockMultipartFile("avatar", "portrait.png", "image/png", png);
        var expected = new ExpertApplicationResponse(9, 42, "Applicant", "applicant@example.org", "Experience text",
                "VEGAN", "Sample recipe summary", null, true, true, "PENDING", null, NOW, null);
        when(storage.uploadPrivateApplicationFile(pdf, "application/pdf", ".pdf"))
                .thenReturn("expert-applications/certificate.pdf");
        when(storage.uploadPrivateApplicationFile(png, "image/png", ".png"))
                .thenReturn("expert-applications/avatar.png");
        when(applications.insert(42, request, "expert-applications/certificate.pdf", "application/pdf",
                "expert-applications/avatar.png", "image/png", NOW)).thenReturn(9L);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(service.submitWithAttachments(request, certificate, avatar)).isSameAs(expected);
        verify(promotion).requireActiveCustomer(42);
        verify(applications).insert(42, request, "expert-applications/certificate.pdf", "application/pdf",
                "expert-applications/avatar.png", "image/png", NOW);
    }

    @Test
    void submitWithAttachmentsAllowsBothOptionalFilesToBeAbsent() {
        var request = request();
        var expected = response("PENDING", null);
        when(applications.insert(42, request, null, null, null, null, NOW)).thenReturn(9L);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(service.submitWithAttachments(request, null, null)).isSameAs(expected);
        verify(applications).insert(42, request, null, null, null, null, NOW);
    }

    @Test
    void acceptsJpegCertificatesAndWebpApplicationAvatars() {
        var request = request();
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        var certificate = new MockMultipartFile("certificate", "credential.jpg", "image/jpeg", jpeg);
        var avatar = new MockMultipartFile("avatar", "portrait.webp", "image/webp", webp);
        var expected = response("PENDING", null);
        when(storage.uploadPrivateApplicationFile(jpeg, "image/jpeg", ".jpg"))
                .thenReturn("expert-applications/certificate.jpg");
        when(storage.uploadPrivateApplicationFile(webp, "image/webp", ".webp"))
                .thenReturn("expert-applications/avatar.webp");
        when(applications.insert(42, request, "expert-applications/certificate.jpg", "image/jpeg",
                "expert-applications/avatar.webp", "image/webp", NOW)).thenReturn(9L);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(service.submitWithAttachments(request, certificate, avatar)).isSameAs(expected);
    }

    @Test
    void invalidDeclaredMimeDoesNotUploadOrCreateApplication() {
        var request = new ExpertApplicationRequest("More than twenty chars experience", "VEGAN",
                "A sample recipe summary with enough details.", null);
        var certificate = new MockMultipartFile("certificate", "credential.pdf", "application/pdf", "not a PDF".getBytes());

        assertThatThrownBy(() -> service.submitWithAttachments(request, certificate, null))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(storage, org.mockito.Mockito.never()).uploadPrivateApplicationFile(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsEmptyAndOversizedAttachmentsBeforeUpload() {
        var request = request();
        var empty = new MockMultipartFile("certificate", "empty.pdf", "application/pdf", new byte[0]);
        var oversizedCertificate = new MockMultipartFile("certificate", "large.pdf", "application/pdf",
                new byte[5 * 1024 * 1024 + 1]);
        var tooLarge = new MockMultipartFile("avatar", "large.png", "image/png",
                new byte[2 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.submitWithAttachments(request, empty, null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.submitWithAttachments(request, null, tooLarge))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus())
                        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
        assertThatThrownBy(() -> service.submitWithAttachments(request, oversizedCertificate, null))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus())
                        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
        verify(storage, org.mockito.Mockito.never()).uploadPrivateApplicationFile(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsCertificateMimeMismatchAndUnsupportedAvatarContent() {
        var request = request();
        var mismatchedCertificate = new MockMultipartFile("certificate", "credential.pdf", "image/png",
                "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        var unsupportedAvatar = new MockMultipartFile("avatar", "credential.pdf", "application/pdf",
                "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        var shortSignature = new MockMultipartFile("avatar", "portrait.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> service.submitWithAttachments(request, mismatchedCertificate, null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.submitWithAttachments(request, null, unsupportedAvatar))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.submitWithAttachments(request, null, shortSignature))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void downloadAttachmentValidatesKindAndStoredContentType() {
        assertThatThrownBy(() -> service.downloadAttachment(9L, "other"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        when(applications.attachment(9L, "certificate"))
                .thenReturn(new ExpertApplicationAttachment("private/key", "text/plain"));
        assertThatThrownBy(() -> service.downloadAttachment(9L, "certificate"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void downloadAttachmentReturnsPrivateBytesWithSafeFilename() {
        byte[] content = {1, 2, 3};
        when(applications.attachment(9L, "avatar"))
                .thenReturn(new ExpertApplicationAttachment("private/key", "image/webp"));
        when(storage.downloadPrivateApplicationFile("private/key")).thenReturn(content);

        ApplicationAttachmentContent downloaded = service.downloadAttachment(9L, "avatar");

        assertThat(downloaded.content()).containsExactly(1, 2, 3);
        assertThat(downloaded.contentType()).isEqualTo("image/webp");
        assertThat(downloaded.filename()).isEqualTo("avatar.webp");
    }

    @Test
    void downloadAttachmentUsesSafeExtensionsForSupportedTypes() {
        var supportedTypes = List.of(
                new String[]{"certificate", "application/pdf", "certificate.pdf"},
                new String[]{"avatar", "image/jpeg", "avatar.jpg"},
                new String[]{"avatar", "image/png", "avatar.png"},
                new String[]{"avatar", "image/webp", "avatar.webp"});
        long id = 20L;
        for (String[] type : supportedTypes) {
            when(applications.attachment(id, type[0]))
                    .thenReturn(new ExpertApplicationAttachment("private/key", type[1]));
            when(storage.downloadPrivateApplicationFile("private/key")).thenReturn(new byte[]{1});

            assertThat(service.downloadAttachment(id++, type[0]).filename()).isEqualTo(type[2]);
        }
    }

    @Test
    void rollbackSynchronizationDeletesUploadedFilesAfterDatabaseRollback() {
        var request = request();
        byte[] pdf = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var certificate = new MockMultipartFile("certificate", "credential.pdf", "application/pdf", pdf);
        when(storage.uploadPrivateApplicationFile(pdf, "application/pdf", ".pdf"))
                .thenReturn("expert-applications/certificate.pdf");
        when(applications.insert(42, request, "expert-applications/certificate.pdf", "application/pdf",
                null, null, NOW)).thenReturn(9L);
        when(applications.findById(9L)).thenThrow(new IllegalStateException("database read failed"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> service.submitWithAttachments(request, certificate, null))
                    .isInstanceOf(IllegalStateException.class);
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(storage).deletePrivateApplicationFile("expert-applications/certificate.pdf");
    }

    @Test
    void rollbackSynchronizationKeepsUploadedFilesAfterCommit() {
        var request = request();
        byte[] pdf = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var certificate = new MockMultipartFile("certificate", "credential.pdf", "application/pdf", pdf);
        var expected = response("PENDING", null);
        when(storage.uploadPrivateApplicationFile(pdf, "application/pdf", ".pdf"))
                .thenReturn("expert-applications/certificate.pdf");
        when(applications.insert(42, request, "expert-applications/certificate.pdf", "application/pdf",
                null, null, NOW)).thenReturn(9L);
        when(applications.findById(9L)).thenReturn(expected);

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThat(service.submitWithAttachments(request, certificate, null)).isSameAs(expected);
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        org.mockito.Mockito.verify(storage, org.mockito.Mockito.never())
                .deletePrivateApplicationFile("expert-applications/certificate.pdf");
    }

    @Test
    void failedApplicationInsertDeletesPreviouslyUploadedAttachment() {
        var request = new ExpertApplicationRequest("More than twenty chars experience", "VEGAN",
                "A sample recipe summary with enough details.", null);
        byte[] pdf = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var certificate = new MockMultipartFile("certificate", "credential.pdf", "application/pdf", pdf);
        when(storage.uploadPrivateApplicationFile(pdf, "application/pdf", ".pdf"))
                .thenReturn("expert-applications/certificate.pdf");
        when(applications.insert(42, request, "expert-applications/certificate.pdf", "application/pdf",
                null, null, NOW)).thenThrow(new DataIntegrityViolationException("pending exists"));

        assertThatThrownBy(() -> service.submitWithAttachments(request, certificate, null))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(storage).deletePrivateApplicationFile("expert-applications/certificate.pdf");
    }

    @Test
    void unexpectedApplicationFailureCleansUpUploadedAttachmentEvenWhenDeleteFails() {
        var request = request();
        byte[] pdf = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var certificate = new MockMultipartFile("certificate", "credential.pdf", "application/pdf", pdf);
        when(storage.uploadPrivateApplicationFile(pdf, "application/pdf", ".pdf"))
                .thenReturn("expert-applications/certificate.pdf");
        when(applications.insert(42, request, "expert-applications/certificate.pdf", "application/pdf",
                null, null, NOW)).thenThrow(new IllegalStateException("database unavailable"));
        org.mockito.Mockito.doThrow(new IllegalStateException("storage unavailable"))
                .when(storage).deletePrivateApplicationFile("expert-applications/certificate.pdf");

        assertThatThrownBy(() -> service.submitWithAttachments(request, certificate, null))
                .isInstanceOf(IllegalStateException.class).hasMessage("database unavailable");

        verify(storage).deletePrivateApplicationFile("expert-applications/certificate.pdf");
    }

    @Test
    void customerHistoryIsScopedToCallerAndIncludesCount() {
        var row = response("REJECTED", "Please add detail.");
        when(applications.page(42L, null, 1, 10)).thenReturn(List.of(row));
        when(applications.count(42L, null)).thenReturn(11L);

        var result = service.ownHistory(1, 10);

        assertThat(result.content()).containsExactly(row);
        assertThat(result.totalElements()).isEqualTo(11L);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void adminListUsesRequestedStatusAndPage() {
        var row = response("PENDING", null);
        when(applications.page(null, "PENDING", 0, 20)).thenReturn(List.of(row));
        when(applications.count(null, "PENDING")).thenReturn(1L);

        assertThat(service.adminList("PENDING", 0, 20).content()).containsExactly(row);
    }

    @Test
    void adminDetailDelegatesLookup() {
        var expected = response("PENDING", null);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(service.adminDetail(9L)).isSameAs(expected);
    }

    @Test
    void rejectPersistsDecisionAndNotifiesWithTrimmedReason() {
        var expected = response("REJECTED", "Insufficient practical detail.");
        when(applications.transitionPending(9L, "REJECTED", "Insufficient practical detail.", 42, NOW)).thenReturn(1);
        when(applications.applicantId(9L)).thenReturn(17L);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(service.reject(9L, "  Insufficient practical detail.  ")).isSameAs(expected);

        verify(notifications).expertApplicationDecision(17L, 9L, false, "Insufficient practical detail.");
    }

    @Test
    void staleRejectionDoesNotNotify() {
        when(applications.transitionPending(9L, "REJECTED", "Insufficient practical detail.", 42, NOW)).thenReturn(0);

        assertThatThrownBy(() -> service.reject(9L, "Insufficient practical detail."))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void missingAuthenticationIsUnauthorized() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("anonymousUser", ""));

        assertThatThrownBy(() -> service.ownHistory(0, 20)).isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private static ExpertApplicationResponse response(String status, String note) {
        return new ExpertApplicationResponse(9, 17, "Applicant", "applicant@example.org", "Experience text",
                "VEGAN", "Sample recipe summary", null, false, false, status, note, NOW, null);
    }

    private static ExpertApplicationRequest request() {
        return new ExpertApplicationRequest("More than twenty chars experience", "VEGAN",
                "A sample recipe summary with enough details.", null);
    }
}
