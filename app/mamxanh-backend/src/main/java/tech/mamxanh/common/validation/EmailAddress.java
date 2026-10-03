package tech.mamxanh.common.validation;

import java.util.Locale;

/**
 * Email handling shared by request DTOs. Emails are trimmed and lower-cased before validation,
 * lookup and storage so that {@code An@Example.com} and {@code an@example.com} are one account.
 * Only ASCII addresses are accepted because {@code USER.email} is a VARCHAR column.
 */
public final class EmailAddress {

    /** ASCII local part, dotted domain with an alphabetic top-level label. */
    public static final String PATTERN = "^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$";

    public static final int MAX_LENGTH = 255;

    private EmailAddress() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
