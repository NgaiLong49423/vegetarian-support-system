package tech.mamxanh.admin.entity;

/** Outcomes an Administrator records when resolving a Recipe Post report (BR-26, CK_REPORT_decision). */
public enum ReportDecision {
    NO_VIOLATION,
    WARN_AUTHOR,
    HIDE_RECIPE,
    LOCK_AUTHOR
}
