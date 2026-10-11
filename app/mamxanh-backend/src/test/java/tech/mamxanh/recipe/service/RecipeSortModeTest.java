package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;

class RecipeSortModeTest {
    @Test
    void defaultsBlankSortToNewestAndAcceptsKnownSortNamesCaseInsensitively() {
        assertThat(RecipeSortMode.from(null)).isEqualTo(RecipeSortMode.NEWEST);
        assertThat(RecipeSortMode.from(" ")).isEqualTo(RecipeSortMode.NEWEST);
        for (RecipeSortMode mode : RecipeSortMode.values()) {
            assertThat(RecipeSortMode.from(mode.name().toLowerCase(java.util.Locale.ROOT))).isEqualTo(mode);
        }
        assertThat(RecipeSortMode.from("most-liked")).isEqualTo(RecipeSortMode.MOST_LIKED);
    }

    @Test
    void rejectsUnknownSortNames() {
        assertThatThrownBy(() -> RecipeSortMode.from("title; drop table recipe_post"))
                .isInstanceOf(AppException.class);
    }
}
