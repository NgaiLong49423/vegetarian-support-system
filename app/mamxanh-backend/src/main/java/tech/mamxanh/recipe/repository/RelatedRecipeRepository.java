package tech.mamxanh.recipe.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Bounded SQL Server ranking over current public recipes; no cached recipe copies. */
@Repository
public class RelatedRecipeRepository {
    private static final String RANKING = """
            WITH public_recipes AS (
              SELECT recipe_id, dish_category, vegetarian_type, published_at
              FROM RECIPE_POST WHERE status = 'PUBLISHED'
            ), raw_amounts AS (
              SELECT ri.recipe_id, ri.ingredient_id,
                CASE WHEN ri.ingredient_id IS NULL THEN NULL
                  WHEN u.dimension = 'MASS' THEN ri.quantity * u.base_factor
                  ELSE ri.quantity * c.grams_per_unit END AS grams
              FROM RECIPE_INGREDIENT ri JOIN public_recipes p ON p.recipe_id = ri.recipe_id
              LEFT JOIN [UNIT] u ON u.unit_id = ri.unit_id
              LEFT JOIN INGREDIENT_UNIT_CONVERSION c
                ON c.ingredient_id = ri.ingredient_id AND c.unit_id = ri.unit_id AND c.is_active = 1
            ), amounts AS (
              SELECT recipe_id, ingredient_id,
                CASE WHEN COUNT(grams) = COUNT(*) THEN SUM(grams) ELSE NULL END AS grams
              FROM raw_amounts GROUP BY recipe_id, ingredient_id
            ), measured AS (
              SELECT recipe_id, ingredient_id, grams,
                MAX(grams) OVER (PARTITION BY recipe_id) AS largest,
                SUM(CASE WHEN grams IS NULL THEN 1 ELSE 0 END) OVER (PARTITION BY recipe_id) AS missing
              FROM amounts
            ), principal AS (
              SELECT recipe_id, ingredient_id FROM measured
              WHERE missing = 0 AND grams = largest
            ), scored AS (
              SELECT p.recipe_id, p.dish_category, p.published_at,
                CASE WHEN p.dish_category = source.dish_category THEN 1 ELSE 0 END
                + CASE WHEN p.vegetarian_type = source.vegetarian_type THEN 1 ELSE 0 END
                + CASE WHEN EXISTS (
                    SELECT 1 FROM principal a JOIN principal b ON a.ingredient_id = b.ingredient_id
                    WHERE a.recipe_id = :sourceId AND b.recipe_id = p.recipe_id
                  ) THEN 1 ELSE 0 END AS score,
                (SELECT COUNT(DISTINCT a.ingredient_id)
                  FROM RECIPE_INGREDIENT a JOIN RECIPE_INGREDIENT b ON a.ingredient_id = b.ingredient_id
                  WHERE a.recipe_id = :sourceId AND b.recipe_id = p.recipe_id) AS shared
              FROM public_recipes p JOIN public_recipes source ON source.recipe_id = :sourceId
              WHERE p.recipe_id <> :sourceId
                AND (:ai = 0 OR p.vegetarian_type = source.vegetarian_type)
            ), ranked AS (
              SELECT *, ROW_NUMBER() OVER (
                PARTITION BY dish_category ORDER BY score DESC, shared DESC, published_at DESC, recipe_id ASC
              ) AS category_rank FROM scored WHERE score > 0
            )
            SELECT recipe_id FROM ranked
            ORDER BY CASE WHEN :ai = 1 THEN category_rank ELSE 0 END ASC,
              score DESC, shared DESC, published_at DESC, recipe_id ASC
            OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Long> findCandidates(long sourceId, int limit, boolean ai) {
        List<?> rows = entityManager.createNativeQuery(RANKING)
                .setParameter("sourceId", sourceId).setParameter("limit", limit)
                .setParameter("ai", ai ? 1 : 0).getResultList();
        return rows.stream().map(row -> ((Number) row).longValue()).toList();
    }
}
