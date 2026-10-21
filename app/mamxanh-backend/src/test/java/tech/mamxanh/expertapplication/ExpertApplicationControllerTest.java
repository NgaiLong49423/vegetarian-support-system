package tech.mamxanh.expertapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.expertapplication.controller.ExpertApplicationController;
import tech.mamxanh.expertapplication.dto.ApproveRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;
import tech.mamxanh.expertapplication.dto.ApplicationAttachmentContent;
import tech.mamxanh.expertapplication.dto.ReviewRequest;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;

@ExtendWith(MockitoExtension.class)
class ExpertApplicationControllerTest {
    @Mock private ExpertApplicationService service;
    @InjectMocks private ExpertApplicationController controller;

    @Test
    void submitDelegatesValidatedApplication() {
        var request = new ExpertApplicationRequest("Experience in vegetarian cooking", "VEGAN",
                "A detailed example vegetarian recipe summary.", null);
        var expected = response("PENDING");
        when(service.submit(request)).thenReturn(expected);

        assertThat(controller.submit(request)).isSameAs(expected);
        verify(service).submit(request);
    }

    @Test
    void multipartSubmissionDelegatesFilesSeparatelyFromProfileAvatar() {
        var request = new ExpertApplicationRequest("Experience in vegetarian cooking", "VEGAN",
                "A detailed example vegetarian recipe summary.", null);
        var certificate = new MockMultipartFile("certificate", "credential.pdf", "application/pdf", "%PDF-1.7".getBytes());
        var avatar = new MockMultipartFile("avatar", "portrait.png", "image/png", new byte[] {1});
        var expected = response("PENDING");
        when(service.submitWithAttachments(request, certificate, avatar)).thenReturn(expected);

        assertThat(controller.submitWithAttachments(request, certificate, avatar)).isSameAs(expected);
        verify(service).submitWithAttachments(request, certificate, avatar);
    }

    @Test
    void historyAppliesRequestedPagination() {
        var expected = new PageResponse<>(List.of(response("REJECTED")), 2, 5, 6, 2);
        when(service.ownHistory(2, 5)).thenReturn(expected);

        assertThat(controller.history(2, 5)).isSameAs(expected);
    }

    @Test
    void adminListRejectsUnsupportedStatus() {
        assertThatThrownBy(() -> controller.list("ARCHIVED", 0, 20)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void adminListDelegatesStatusAndPagination() {
        var expected = new PageResponse<>(List.of(response("PENDING")), 0, 10, 1, 1);
        when(service.adminList("PENDING", 0, 10)).thenReturn(expected);

        assertThat(controller.list("PENDING", 0, 10)).isSameAs(expected);
    }

    @Test
    void adminDetailDelegatesApplicationId() {
        var expected = response("PENDING");
        when(service.adminDetail(9)).thenReturn(expected);

        assertThat(controller.detail(9)).isSameAs(expected);
    }

    @Test
    void attachmentResponseStreamsPrivateContentWithNoSniffHeader() {
        byte[] content = "%PDF-1.7".getBytes();
        when(service.downloadAttachment(9, "certificate"))
                .thenReturn(new ApplicationAttachmentContent(content, MediaType.APPLICATION_PDF_VALUE, "certificate.pdf"));

        var response = controller.attachment(9, "certificate");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getBody()).containsExactly(content);
    }

    @Test
    void approvalUsesOptionalNote() {
        var expected = response("APPROVED");
        when(service.approve(9, "Reviewed")).thenReturn(expected);

        assertThat(controller.approve(9, new ApproveRequest("Reviewed"))).isSameAs(expected);
        assertThat(controller.approve(9, null)).isNull();
        verify(service).approve(9, "Reviewed");
        verify(service).approve(9, null);
    }

    @Test
    void rejectionPassesRequiredReason() {
        var expected = response("REJECTED");
        when(service.reject(9, "Insufficient detail")).thenReturn(expected);

        assertThat(controller.reject(9, new ReviewRequest("Insufficient detail"))).isSameAs(expected);
    }

    private static ExpertApplicationResponse response(String status) {
        return new ExpertApplicationResponse(9, 17, "Applicant", "applicant@example.org", "Experience text",
                "VEGAN", "Sample recipe summary", null, false, false, status, null, LocalDateTime.now(), null);
    }
}
