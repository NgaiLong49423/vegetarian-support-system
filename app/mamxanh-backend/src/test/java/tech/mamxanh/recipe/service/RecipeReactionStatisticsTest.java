package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RecipeReactionStatisticsTest {
    @Test
    void noVotesIsDifferentFromZeroSatisfaction() {
        assertThat(RecipeReactionStatistics.likePercentage(0, 0)).isNull();
        assertThat(RecipeReactionStatistics.likePercentage(0, 5)).isEqualByComparingTo("0");
    }

    @ParameterizedTest
    @CsvSource({"10,1,91", "1,2,33", "2,1,67", "1,7,13", "5,0,100", "0,5,0"})
    void roundsThePercentageOnceToAWholeNumber(long likes, long dislikes, String expected) {
        var result = RecipeReactionStatistics.likePercentage(likes, dislikes);
        assertThat(result).isEqualByComparingTo(expected);
        assertThat(result.scale()).isZero();
    }
}
