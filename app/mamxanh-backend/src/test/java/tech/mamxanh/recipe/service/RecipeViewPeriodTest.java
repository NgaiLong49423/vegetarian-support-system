package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;

class RecipeViewPeriodTest {
    @Test
    void defaultsToAllTimeAndDefinesAllSupportedWindows() {
        assertThat(RecipeViewPeriod.from(null)).isEqualTo(RecipeViewPeriod.ALL_TIME);
        assertThat(RecipeViewPeriod.from("all-time")).isEqualTo(RecipeViewPeriod.ALL_TIME);
        assertThat(RecipeViewPeriod.from("last-24-hours").hours()).isEqualTo(24);
        assertThat(RecipeViewPeriod.from("last-7-days").hours()).isEqualTo(168);
        assertThat(RecipeViewPeriod.from("last-30-days").hours()).isEqualTo(720);
    }

    @Test
    void rejectsUnsupportedWindows() {
        assertThatThrownBy(() -> RecipeViewPeriod.from("last-90-days"))
                .isInstanceOf(AppException.class);
    }
}
