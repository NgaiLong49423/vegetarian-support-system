package tech.mamxanh.recipe.service;

import java.util.Locale;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/** Public recipe ordering options shared by the guest browse API and FR-08 consumers. */
public enum RecipeSortMode {
    NEWEST,
    MOST_LIKED,
    MOST_VIEWED,
    MOST_COMMENTED,
    MOST_ACTIVE,
    TRENDING;

    public static RecipeSortMode from(String value) {
        if (value == null || value.isBlank()) return NEWEST;
        try {
            return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
