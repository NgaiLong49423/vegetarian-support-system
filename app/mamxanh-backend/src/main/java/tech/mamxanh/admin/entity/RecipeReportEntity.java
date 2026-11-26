package tech.mamxanh.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/** Private report record; no public or recipe-author read endpoint exposes its reporter. */
@Entity
@Table(name = "REPORT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "recipe_id")
    private Long recipeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 30)
    private RecipeReportReason reasonCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "handled_by")
    private Long handledBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 30)
    private ReportDecision decision;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "appeal_status", length = 20)
    private AppealStatus appealStatus;

    @Column(name = "appeal_message", length = 1000)
    private String appealMessage;

    @Column(name = "appealed_at")
    private LocalDateTime appealedAt;

    @Column(name = "appeal_resubmitted_at")
    private LocalDateTime appealResubmittedAt;

    @Column(name = "appeal_decision_reason", length = 1000)
    private String appealDecisionReason;

    @Column(name = "appeal_handled_at")
    private LocalDateTime appealHandledAt;

    @Column(name = "appeal_handled_by")
    private Long appealHandledBy;

    public RecipeReportEntity(long reporterId, long recipeId, RecipeReportReason reasonCode,
            String description, LocalDateTime now) {
        this.reporterId = reporterId;
        this.recipeId = recipeId;
        this.reasonCode = reasonCode;
        this.description = description;
        this.status = "OPEN";
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** AC-28.1: an OPEN report moves to IN_REVIEW and records the Administrator handling it. */
    public void claim(long administratorId, LocalDateTime now) {
        this.status = "IN_REVIEW";
        this.handledBy = administratorId;
        this.updatedAt = now;
    }

    /** Records the final decision; the caller has checked that this Administrator holds the report. */
    public void resolve(ReportDecision decision, String reason, LocalDateTime now) {
        this.status = "RESOLVED";
        this.decision = decision;
        this.decisionReason = reason;
        this.handledAt = now;
        this.updatedAt = now;
    }

    /** BR-27: the author may appeal a hide decision once; later steps go through the Administrator. */
    public void submitAppeal(String message, LocalDateTime now) {
        if (decision != ReportDecision.HIDE_RECIPE || appealStatus != null) {
            throw new AppException(ErrorCode.APPEAL_NOT_ALLOWED);
        }
        this.appealStatus = AppealStatus.PENDING;
        this.appealMessage = message;
        this.appealedAt = now;
        this.updatedAt = now;
    }

    /** The author asks for a second review after editing the post that the Administrator let them edit. */
    public void resubmitAppeal(LocalDateTime now) {
        if (appealStatus != AppealStatus.EDIT_ALLOWED) {
            throw new AppException(ErrorCode.APPEAL_STATE_INVALID);
        }
        this.appealStatus = AppealStatus.RESUBMITTED;
        this.appealResubmittedAt = now;
        this.updatedAt = now;
    }

    /**
     * Records the Administrator's latest appeal decision: RESTORE or KEEP_HIDDEN on a PENDING or
     * RESUBMITTED appeal, ALLOW_EDIT only on a PENDING one. The caller applies the effect on the post.
     */
    public void decideAppeal(AppealDecision appealDecision, String reason, long administratorId, LocalDateTime now) {
        boolean open = appealStatus == AppealStatus.PENDING || appealStatus == AppealStatus.RESUBMITTED;
        if (!open || (appealDecision == AppealDecision.ALLOW_EDIT && appealStatus != AppealStatus.PENDING)) {
            throw new AppException(ErrorCode.APPEAL_STATE_INVALID);
        }
        this.appealStatus = switch (appealDecision) {
            case RESTORE -> AppealStatus.RESTORED;
            case KEEP_HIDDEN -> AppealStatus.KEPT_HIDDEN;
            case ALLOW_EDIT -> AppealStatus.EDIT_ALLOWED;
        };
        this.appealDecisionReason = reason;
        this.appealHandledBy = administratorId;
        this.appealHandledAt = now;
        this.updatedAt = now;
    }
}
