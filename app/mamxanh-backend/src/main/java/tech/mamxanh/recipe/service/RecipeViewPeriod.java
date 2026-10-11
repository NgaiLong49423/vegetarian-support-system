package tech.mamxanh.recipe.service;

import java.util.Locale;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

public enum RecipeViewPeriod {
    ALL_TIME(null),
    LAST_24_HOURS(24),
    LAST_7_DAYS(24 * 7),
    LAST_30_DAYS(24 * 30);

    private final Integer hours;

    RecipeViewPeriod(Integer hours) { this.hours = hours; }

    public Integer hours() { return hours; }

    public static RecipeViewPeriod from(String value) {
        if (value == null || value.isBlank()) return ALL_TIME;
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
