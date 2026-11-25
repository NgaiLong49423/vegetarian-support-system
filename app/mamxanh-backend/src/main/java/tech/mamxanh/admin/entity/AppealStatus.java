package tech.mamxanh.admin.entity;

/** Steps of an author's appeal against a HIDE_RECIPE decision (FR-28, BR-27, CK_REPORT_appeal_status). */
public enum AppealStatus {
    PENDING,
    EDIT_ALLOWED,
    RESUBMITTED,
    RESTORED,
    KEPT_HIDDEN
}
