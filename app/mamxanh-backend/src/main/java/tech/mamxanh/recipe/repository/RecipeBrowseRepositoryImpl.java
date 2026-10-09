package tech.mamxanh.recipe.repository;

import java.time.LocalDateTime;
import java.util.List;
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
            """;

    @PersistenceContext private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public BrowsePage findPublished(String keyword, RecipeSortMode sortMode, LocalDateTime viewSince,
            LocalDateTime now, int page, int size) {
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
        var query = entityManager.createNativeQuery(PAGE_SELECT + " ORDER BY " + orderBy
                + tieBreak + " OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY");
        bind(query, keyword, viewSince, now);
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
        var count = entityManager.createNativeQuery(COUNT_SELECT);
        count.setParameter("keyword", keyword);
        count.setParameter("pattern", "%" + keyword.toLowerCase(java.util.Locale.ROOT) + "%");
        long total = ((Number) count.getSingleResult()).longValue();
        return new BrowsePage(items, total);
    }

    private static void bind(jakarta.persistence.Query query, String keyword, LocalDateTime viewSince, LocalDateTime now) {
        query.setParameter("keyword", keyword);
        query.setParameter("pattern", "%" + keyword.toLowerCase(java.util.Locale.ROOT) + "%");
        query.setParameter("viewSince", viewSince);
        query.setParameter("now", now);
    }
}
