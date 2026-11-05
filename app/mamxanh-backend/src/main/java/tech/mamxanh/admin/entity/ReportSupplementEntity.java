package tech.mamxanh.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Append-only information a reporter adds to an open report (FR-30, AC-30.1); never updated or deleted. */
@Entity
@Table(name = "REPORT_SUPPLEMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportSupplementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplement_id")
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ReportSupplementEntity(long reportId, String content, LocalDateTime now) {
        this.reportId = reportId;
        this.content = content;
        this.createdAt = now;
    }
}
