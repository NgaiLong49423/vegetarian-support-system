package tech.mamxanh.notification.service;

/**
 * Published inside the report decision transaction; the email itself is sent after commit (UC-49.3).
 * Title and message are the in-app notification's text, so they carry no trace of the reporter (BR-28).
 */
public record ModerationEmailRequested(String email, String displayName, String title, String message,
        String targetPath) {
    /** Keeps the recipient address out of logs. */
    @Override
    public String toString() {
        return "ModerationEmailRequested[redacted]";
    }
}
