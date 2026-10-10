package tech.mamxanh.recipe.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tech.mamxanh.recipe.service.RecipeSortMode;
import tech.mamxanh.recipe.service.RecipeRankingPolicy;

@Repository
class RecipeBrowseRepositoryImpl implements RecipeBrowseRepository {
    private static final String METRICS = """
            WITH matched AS (
              SELECT rp.recipe_id, rp.published_at
              FROM RECIPE_POST rp
              WHERE rp.status = 'PUBLISHED'
                AND (:keyword = '' OR LOWER(rp.title) LIKE :pattern OR LOWER(COALESCE(rp.description, '')) LIKE :pattern)
                %s
            ),
            reaction_stats AS (
              SELECT rr.recipe_id,
                SUM(CASE WHEN rr.reaction_type = 'LIKE' THEN 1 ELSE 0 END) AS likes,
                SUM(CASE WHEN rr.reaction_type = 'DISLIKE' THEN 1 ELSE 0 END) AS dislikes,
                SUM(CASE WHEN rr.updated_at >= :since7d AND rr.updated_at <= :now THEN 1 ELSE 0 END) AS reactions7d,
                SUM(CASE WHEN rr.updated_at >= :since3d AND rr.updated_at <= :now THEN 1 ELSE 0 END) AS reactions3d
              FROM RECIPE_REACTION rr JOIN matched m ON m.recipe_id = rr.recipe_id
              GROUP BY rr.recipe_id
            ),
            view_stats AS (
              SELECT rv.recipe_id,
                COUNT_BIG(*) AS views,
                SUM(CASE WHEN rv.viewed_at >= :since7d AND rv.viewed_at <= :now THEN 1 ELSE 0 END) AS views7d,
                SUM(CASE WHEN rv.viewed_at >= :since3d AND rv.viewed_at <= :now THEN 1 ELSE 0 END) AS views3d,
                SUM(CASE WHEN rv.viewed_at >= :viewSince THEN 1 ELSE 0 END) AS periodViews
              FROM RECIPE_VIEW rv JOIN matched m ON m.recipe_id = rv.recipe_id
              GROUP BY rv.recipe_id
            ),
            comment_stats AS (
              SELECT c.recipe_id,
                COUNT_BIG(*) AS comments,
                SUM(CASE WHEN c.created_at >= :since7d AND c.created_at <= :now THEN 1 ELSE 0 END) AS comments7d,
                SUM(CASE WHEN c.created_at >= :since3d AND c.created_at <= :now THEN 1 ELSE 0 END) AS comments3d
              FROM [COMMENT] c JOIN matched m ON m.recipe_id = c.recipe_id
              WHERE c.is_deleted = 0
              GROUP BY c.recipe_id
            )
            """;

    private static final String PAGE_SELECT = METRICS + """
            SELECT m.recipe_id,
              COALESCE(rs.likes, 0) AS likes,
              COALESCE(rs.dislikes, 0) AS dislikes,
              COALESCE(vs.views, 0) AS views,
              COALESCE(cs.comments, 0) AS comments,
              COALESCE(vs.periodViews, 0) AS periodViews,
              COALESCE(vs.views7d, 0) * :viewWeight + COALESCE(cs.comments7d, 0) * :commentWeight
                + COALESCE(rs.reactions7d, 0) * :reactionWeight AS activeScore,
              (COALESCE(vs.views3d, 0) * :viewWeight + COALESCE(cs.comments3d, 0) * :commentWeight
                + COALESCE(rs.reactions3d, 0) * :reactionWeight
                + CASE WHEN m.published_at <= :now
                    AND DATEDIFF(HOUR, m.published_at, :now) < :newRecipeHours
                    THEN :newRecipeBonus ELSE 0 END)
                / POWER(CASE WHEN m.published_at > :now THEN 2.0
                    ELSE CAST(DATEDIFF(MINUTE, m.published_at, :now) AS FLOAT) / 60.0 + 2.0 END,
                    :freshnessExponent) AS trendingScore,
              COALESCE(CAST(rs.likes AS FLOAT) * 100.0 / NULLIF(rs.likes + rs.dislikes, 0), -1) AS likePercentage
            FROM matched m
            LEFT JOIN reaction_stats rs ON rs.recipe_id = m.recipe_id
            LEFT JOIN view_stats vs ON vs.recipe_id = m.recipe_id
            LEFT JOIN comment_stats cs ON cs.recipe_id = m.recipe_id
            """;

    private static final String COUNT_SELECT = """
            SELECT COUNT_BIG(*) FROM RECIPE_POST rp
            WHERE rp.status = 'PUBLISHED'
              AND (:keyword = '' OR LOWER(rp.title) LIKE :pattern OR LOWER(COALESCE(rp.description, '')) LIKE :pattern)
              %s
            """;

    @PersistenceContext private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public BrowsePage findPublished(String keyword, String vegetarianType, String dishCategory,
            List<Long> ingredientIds, Integer maxTotalTimeMinutes, RecipeSortMode sortMode,
            LocalDateTime viewSince, LocalDateTime now, int page, int size) {
        return find(keyword, filters(vegetarianType, dishCategory, ingredientIds, maxTotalTimeMinutes, null),
                sortMode, viewSince, now, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public BrowsePage findPublishedByAuthor(long authorId, LocalDateTime now, int page, int size) {
        return find("", filters(null, null, List.of(), null, authorId), RecipeSortMode.NEWEST,
                LocalDateTime.of(1, 1, 1, 0, 0), now, page, size);
    }

    private BrowsePage find(String keyword, FilterSql filters, RecipeSortMode sortMode,
            LocalDateTime viewSince, LocalDateTime now, int page, int size) {
        String orderBy = switch (sortMode) {
            case NEWEST -> "m.published_at DESC";
            case MOST_LIKED -> "likePercentage DESC, COALESCE(rs.likes, 0) DESC";
            case MOST_VIEWED -> "COALESCE(vs.periodViews, 0) DESC";
            case MOST_COMMENTED -> "COALESCE(cs.comments, 0) DESC";
            case MOST_ACTIVE -> "activeScore DESC";
            case TRENDING -> "trendingScore DESC";
        };
        String tieBreak = sortMode == RecipeSortMode.NEWEST
                ? ", m.recipe_id ASC" : ", m.published_at DESC, m.recipe_id ASC";
        var query = entityManager.createNativeQuery(PAGE_SELECT.formatted(filters.predicate()) + " ORDER BY " + orderBy
                + tieBreak + " OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY");
        bindSearchFilters(query, keyword, filters.parameters());
        query.setParameter("viewSince", viewSince);
        query.setParameter("now", now);
        query.setParameter("since7d", now.minusDays(7));
        query.setParameter("since3d", now.minusDays(3));
        query.setParameter("viewWeight", RecipeRankingPolicy.VIEW_WEIGHT);
        query.setParameter("commentWeight", RecipeRankingPolicy.COMMENT_WEIGHT);
        query.setParameter("reactionWeight", RecipeRankingPolicy.REACTION_WEIGHT);
        query.setParameter("newRecipeHours", RecipeRankingPolicy.TRENDING_NEW_RECIPE_HOURS);
        query.setParameter("newRecipeBonus", RecipeRankingPolicy.TRENDING_NEW_RECIPE_BONUS);
        query.setParameter("freshnessExponent", RecipeRankingPolicy.TRENDING_FRESHNESS_EXPONENT);
        query.setParameter("offset", (long) page * size);
        query.setParameter("size", size);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<BrowseRow> items = rows.stream().map(row -> new BrowseRow(
                ((Number) row[0]).longValue(), ((Number) row[1]).longValue(), ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue(), ((Number) row[4]).longValue(), ((Number) row[5]).longValue())).toList();
        var count = entityManager.createNativeQuery(COUNT_SELECT.formatted(filters.predicate()));
        bindSearchFilters(count, keyword, filters.parameters());
        long total = ((Number) count.getSingleResult()).longValue();
        return new BrowsePage(items, total);
    }

    private static void bindSearchFilters(jakarta.persistence.Query query, String keyword,
            java.util.Map<String, Object> filterParameters) {
        query.setParameter("keyword", keyword);
        query.setParameter("pattern", "%" + keyword.toLowerCase(Locale.ROOT) + "%");
        filterParameters.forEach(query::setParameter);
    }

    private static FilterSql filters(String vegetarianType, String dishCategory, List<Long> ingredientIds,
            Integer maxTotalTimeMinutes, Long authorId) {
        List<String> clauses = new ArrayList<>();
        java.util.Map<String, Object> parameters = new java.util.LinkedHashMap<>();
        if (authorId != null) {
            clauses.add("AND rp.author_id = :authorId");
            parameters.put("authorId", authorId);
        }
        if (vegetarianType != null) {
            clauses.add("AND rp.vegetarian_type = :vegetarianType");
            parameters.put("vegetarianType", vegetarianType);
        }
        if (dishCategory != null) {
            clauses.add("AND rp.dish_category = :dishCategory");
            parameters.put("dishCategory", dishCategory);
        }
        if (maxTotalTimeMinutes != null) {
            clauses.add("AND rp.prep_time_min + rp.cook_time_min <= :maxTotalTimeMinutes");
            parameters.put("maxTotalTimeMinutes", maxTotalTimeMinutes);
        }
        for (int index = 0; index < ingredientIds.size(); index++) {
            String parameter = "ingredientId" + index;
            clauses.add("AND EXISTS (SELECT 1 FROM RECIPE_INGREDIENT ri WHERE ri.recipe_id = rp.recipe_id AND ri.ingredient_id = :" + parameter + ")");
            parameters.put(parameter, ingredientIds.get(index));
        }
        return new FilterSql(clauses.isEmpty() ? "" : "\n" + String.join("\n", clauses), parameters);
    }

    private record FilterSql(String predicate, java.util.Map<String, Object> parameters) { }
}
