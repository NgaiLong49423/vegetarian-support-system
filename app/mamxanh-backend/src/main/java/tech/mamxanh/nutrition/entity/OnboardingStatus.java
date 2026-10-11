package tech.mamxanh.nutrition.entity;

/**
 * FR-31 Onboarding invitation state ({@code CK_USER_onboarding_status}). It only drives whether the
 * invitation is shown; the personalized-AI gate checks the profile data itself.
 */
public enum OnboardingStatus {
    /** The invitation has not been answered; shown once after a new account signs in (AC-31.10). */
    NOT_STARTED,
    /** The invitation was closed without completing the profile; never shown again automatically. */
    SKIPPED,
    /** The questionnaire was completed. */
    COMPLETED
}
