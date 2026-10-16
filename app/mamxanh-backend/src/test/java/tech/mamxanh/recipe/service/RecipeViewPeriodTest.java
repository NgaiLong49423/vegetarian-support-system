package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

class RecipeViewPeriodTest {
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "all-time", " ALL_TIME "})
    void emptyOrAllTimeSelectsUnlimitedHistory(String input) {
        assertThat(RecipeViewPeriod.from(input)).isEqualTo(RecipeViewPeriod.ALL_TIME);
        assertThat(RecipeViewPeriod.from(input).hours()).isNull();
    }

    @ParameterizedTest @CsvSource({"last-24-hours,24", "last-7-days,168", "last-30-days,720",
            " LAST_7_DAYS ,168", "last_30_days,720"})
    void supportedPeriodsNormalizeAndUseRollingHourDurations(String input, int hours) {
        assertThat(RecipeViewPeriod.from(input).hours()).isEqualTo(hours);
    }

    @ParameterizedTest @ValueSource(strings = {"LAST_3_DAYS","TODAY","-1","last-90-days"})
    void unsupportedWindowIsAValidationError(String input) {
        assertThatThrownBy(() -> RecipeViewPeriod.from(input)).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
