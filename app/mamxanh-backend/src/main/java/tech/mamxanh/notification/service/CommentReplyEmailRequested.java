package tech.mamxanh.notification.service;

/** Published inside the reply transaction; the email itself is sent after commit (AC-49.2). */
public record CommentReplyEmailRequested(String email, String displayName, String replierName, String recipeTitle,
        String targetPath) {

    /** Keeps the recipient address out of logs. */
    @Override
    public String toString() {
        return "CommentReplyEmailRequested[redacted]";
    }
}
