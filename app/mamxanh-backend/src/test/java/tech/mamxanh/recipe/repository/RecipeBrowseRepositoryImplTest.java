package tech.mamxanh.recipe.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tech.mamxanh.recipe.service.RecipeSortMode;

class RecipeBrowseRepositoryImplTest {
    private EntityManager entityManager;
    private Query pageQuery;
    private Query countQuery;
    private RecipeBrowseRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        pageQuery = mock(Query.class);
        countQuery = mock(Query.class);
        repository = new RecipeBrowseRepositoryImpl();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            return sql.startsWith("SELECT COUNT_BIG(*)") ? countQuery : pageQuery;
        });
        when(pageQuery.setParameter(anyString(), any())).thenReturn(pageQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(pageQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[] {41L, 3L, 1L, 9L, 4L, 2L}));
        when(countQuery.getSingleResult()).thenReturn(1L);
    }

    @Test
    void mapsBrowseRowsAndUsesAnAllowlistedOrderWithStableTieBreakersForEveryMode() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 8, 0, 0);
        for (RecipeSortMode mode : RecipeSortMode.values()) {
            var result = repository.findPublished("canh", null, null, List.of(), null,
                    mode, now.minusHours(24), now, 0, 12);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.rows()).containsExactly(new RecipeBrowseRepository.BrowseRow(41, 3, 1, 9, 4, 2));
        }
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, org.mockito.Mockito.times(12)).createNativeQuery(sqlCaptor.capture());
        List<String> pageQueries = sqlCaptor.getAllValues().stream().filter(sql -> sql.contains("OFFSET :offset ROWS")).toList();
        assertThat(pageQueries).hasSize(6);
        assertThat(pageQueries).anySatisfy(sql -> assertThat(sql).contains("ORDER BY m.published_at DESC, m.recipe_id ASC"));
        assertThat(pageQueries).anySatisfy(sql -> assertThat(sql).contains("ORDER BY likePercentage DESC, COALESCE(rs.likes, 0) DESC, m.published_at DESC, m.recipe_id ASC"));
        assertThat(pageQueries).anySatisfy(sql -> assertThat(sql).contains("ORDER BY COALESCE(vs.periodViews, 0) DESC, m.published_at DESC, m.recipe_id ASC"));
        assertThat(pageQueries).anySatisfy(sql -> assertThat(sql).contains("ORDER BY COALESCE(cs.comments, 0) DESC, m.published_at DESC, m.recipe_id ASC"));
        assertThat(pageQueries).anySatisfy(sql -> assertThat(sql).contains("ORDER BY activeScore DESC, m.published_at DESC, m.recipe_id ASC"));
        assertThat(pageQueries).anySatisfy(sql -> assertThat(sql).contains("ORDER BY trendingScore DESC, m.published_at DESC, m.recipe_id ASC"));
        verify(pageQuery, org.mockito.Mockito.times(6)).setParameter("size", 12);
    }

    @Test
    void usesOnlyBoundSearchValuesAndTheAllTimeCutoffAsAValue() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 8, 0, 0);
        repository.findPublished("%' OR 1=1 --", null, null, List.of(), null, RecipeSortMode.NEWEST,
                LocalDateTime.of(1, 1, 1, 0, 0), now, 1, 12);
        verify(pageQuery).setParameter("keyword", "%' OR 1=1 --");
        verify(pageQuery).setParameter("viewSince", LocalDateTime.of(1, 1, 1, 0, 0));
    }

    @Test
    void buildsParameterizedPredicatesForAllFiltersAndEverySelectedIngredient() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 8, 0, 0);
        repository.findPublished("canh", "VEGAN", "SOUP", List.of(11L, 22L), 30,
                RecipeSortMode.NEWEST, now.minusDays(7), now, 0, 12);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, org.mockito.Mockito.times(2)).createNativeQuery(sqlCaptor.capture());
        assertThat(sqlCaptor.getAllValues()).allSatisfy(sql -> {
            assertThat(sql).contains("rp.vegetarian_type = :vegetarianType")
                    .contains("rp.dish_category = :dishCategory")
                    .contains("rp.prep_time_min + rp.cook_time_min <= :maxTotalTimeMinutes")
                    .contains("ri.ingredient_id = :ingredientId0")
                    .contains("ri.ingredient_id = :ingredientId1");
        });
        verify(pageQuery).setParameter("vegetarianType", "VEGAN");
        verify(pageQuery).setParameter("dishCategory", "SOUP");
        verify(pageQuery).setParameter("maxTotalTimeMinutes", 30);
        verify(pageQuery).setParameter("ingredientId0", 11L);
        verify(pageQuery).setParameter("ingredientId1", 22L);
    }
}
