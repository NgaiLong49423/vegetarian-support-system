package tech.mamxanh.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
