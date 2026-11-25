package tech.mamxanh.admin.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/** Appeal steps on a report that hid a Recipe Post (FR-28, BR-27). */
class RecipeReportAppealTest {
    private static final LocalDateTime HIDDEN_AT = LocalDateTime.of(2026, 11, 20, 8, 0);
    private static final String MESSAGE = "Bài đã ghi đúng nguyên liệu thuần chay, xin xem lại.";
    private static final String REASON = "Đã kiểm tra lại nội dung bài viết.";

    @Test
    void submitThenAllowEditThenResubmitThenRestore() {
        RecipeReportEntity report = hiddenReport();

        report.submitAppeal(MESSAGE, HIDDEN_AT.plusDays(1));
        assertThat(report.getAppealStatus()).isEqualTo(AppealStatus.PENDING);
        assertThat(report.getAppealMessage()).isEqualTo(MESSAGE);
        assertThat(report.getAppealedAt()).isEqualTo(HIDDEN_AT.plusDays(1));

        report.decideAppeal(AppealDecision.ALLOW_EDIT, REASON, 7L, HIDDEN_AT.plusDays(2));
        assertThat(report.getAppealStatus()).isEqualTo(AppealStatus.EDIT_ALLOWED);
        assertThat(report.getAppealHandledBy()).isEqualTo(7L);
        assertThat(report.getAppealDecisionReason()).isEqualTo(REASON);

        report.resubmitAppeal(HIDDEN_AT.plusDays(3));
        assertThat(report.getAppealStatus()).isEqualTo(AppealStatus.RESUBMITTED);
        assertThat(report.getAppealResubmittedAt()).isEqualTo(HIDDEN_AT.plusDays(3));

        report.decideAppeal(AppealDecision.RESTORE, "Bài đã được sửa đúng nhãn.", 8L, HIDDEN_AT.plusDays(4));
        assertThat(report.getAppealStatus()).isEqualTo(AppealStatus.RESTORED);
        assertThat(report.getAppealHandledBy()).isEqualTo(8L);
        assertThat(report.getAppealHandledAt()).isEqualTo(HIDDEN_AT.plusDays(4));
    }

    @Test
    void keepHiddenEndsPendingAppeal() {
        RecipeReportEntity report = hiddenReport();
        report.submitAppeal(MESSAGE, HIDDEN_AT);

        report.decideAppeal(AppealDecision.KEEP_HIDDEN, REASON, 7L, HIDDEN_AT);

        assertThat(report.getAppealStatus()).isEqualTo(AppealStatus.KEPT_HIDDEN);
    }

    @Test
    void allowEditRejectedAfterResubmit() {
        RecipeReportEntity report = hiddenReport();
        report.submitAppeal(MESSAGE, HIDDEN_AT);
        report.decideAppeal(AppealDecision.ALLOW_EDIT, REASON, 7L, HIDDEN_AT);
        report.resubmitAppeal(HIDDEN_AT);

        assertError(() -> report.decideAppeal(AppealDecision.ALLOW_EDIT, REASON, 7L, HIDDEN_AT),
                ErrorCode.APPEAL_STATE_INVALID);
        assertThat(report.getAppealStatus()).isEqualTo(AppealStatus.RESUBMITTED);
    }

    @Test
    void cannotAppealTwice() {
        RecipeReportEntity report = hiddenReport();
        report.submitAppeal(MESSAGE, HIDDEN_AT);

        assertError(() -> report.submitAppeal(MESSAGE, HIDDEN_AT), ErrorCode.APPEAL_NOT_ALLOWED);
    }

    @Test
    void cannotAppealNonHideDecision() {
        RecipeReportEntity report = new RecipeReportEntity(1L, 2L, RecipeReportReason.OTHER, null, HIDDEN_AT);
        report.resolve(ReportDecision.WARN_AUTHOR, REASON, HIDDEN_AT);

        assertError(() -> report.submitAppeal(MESSAGE, HIDDEN_AT), ErrorCode.APPEAL_NOT_ALLOWED);
        assertThat(report.getAppealStatus()).isNull();
    }

    @Test
    void resubmitOnlyWhenEditAllowed() {
        RecipeReportEntity report = hiddenReport();
        report.submitAppeal(MESSAGE, HIDDEN_AT);

        assertError(() -> report.resubmitAppeal(HIDDEN_AT), ErrorCode.APPEAL_STATE_INVALID);
    }

    @Test
    void decidingTerminalOrMissingAppealFails() {
        RecipeReportEntity notAppealed = hiddenReport();
        assertError(() -> notAppealed.decideAppeal(AppealDecision.RESTORE, REASON, 7L, HIDDEN_AT),
                ErrorCode.APPEAL_STATE_INVALID);

        RecipeReportEntity restored = hiddenReport();
        restored.submitAppeal(MESSAGE, HIDDEN_AT);
        restored.decideAppeal(AppealDecision.RESTORE, REASON, 7L, HIDDEN_AT);
        assertError(() -> restored.decideAppeal(AppealDecision.KEEP_HIDDEN, REASON, 7L, HIDDEN_AT),
                ErrorCode.APPEAL_STATE_INVALID);
        assertThat(restored.getAppealStatus()).isEqualTo(AppealStatus.RESTORED);
    }

    private static RecipeReportEntity hiddenReport() {
        RecipeReportEntity report = new RecipeReportEntity(1L, 2L, RecipeReportReason.OTHER, null, HIDDEN_AT);
        report.resolve(ReportDecision.HIDE_RECIPE, "Bài ghi sai nhãn thuần chay.", HIDDEN_AT);
        return report;
    }

    private static void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).errorCode()).isEqualTo(expected));
    }
}
