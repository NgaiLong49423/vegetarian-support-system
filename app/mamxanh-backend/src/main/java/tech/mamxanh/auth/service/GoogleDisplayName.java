package tech.mamxanh.auth.service;

/**
 * Q41: display name of an account created from Google. The Google name is trimmed and cut to 50
 * characters; a name that is then empty or shorter than 3 characters falls back to the email
 * local part when that part has 3–50 characters, otherwise to a fixed default.
 */
final class GoogleDisplayName {

    static final int MIN_LENGTH = 3;
    static final int MAX_LENGTH = 50;
    static final String DEFAULT_NAME = "Thành viên Mâm Xanh";

    private GoogleDisplayName() {
    }

    static String of(String googleName, String email) {
        String name = googleName == null ? "" : cut(googleName.strip());
        if (length(name) >= MIN_LENGTH) {
            return name;
        }
        String localPart = email.substring(0, email.indexOf('@'));
        int localLength = length(localPart);
        return localLength >= MIN_LENGTH && localLength <= MAX_LENGTH ? localPart : DEFAULT_NAME;
    }

    /** Counts characters as code points so an emoji is one character and is never split. */
    private static int length(String value) {
        return value.codePointCount(0, value.length());
    }

    private static String cut(String value) {
        if (length(value) <= MAX_LENGTH) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, MAX_LENGTH));
    }
}
