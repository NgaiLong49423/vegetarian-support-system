package tech.mamxanh.recipe.repository;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Native SQL access to the approved RECIPE_VIEW table, enlisted in the service transaction. */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class RecipeViewRepository {
    private final JdbcTemplate jdbc;

    public RecipeViewRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public int lockViewer(String resource) {
        return jdbc.execute((ConnectionCallback<Integer>) connection -> {
            try (var statement = connection.prepareCall("{? = call sys.sp_getapplock(?, ?, ?, ?)}")) {
                statement.registerOutParameter(1, Types.INTEGER);
                statement.setString(2, resource);
                statement.setString(3, "Exclusive");
                statement.setString(4, "Transaction");
                statement.setInt(5, 1000);
                statement.execute();
                return statement.getInt(1);
            }
        });
    }

    public Optional<Long> publishedCount(long recipeId) {
        return jdbc.query("SELECT view_count FROM RECIPE_POST WHERE recipe_id = ? AND status = 'PUBLISHED'",
                (rs, row) -> rs.getLong(1), recipeId).stream().findFirst();
    }

    public Optional<LocalDateTime> latest(long recipeId, Long userId, String anonymousHash) {
        String identity = userId == null ? "user_id IS NULL AND anonymous_viewer_hash = ?" : "user_id = ?";
        return jdbc.query("SELECT TOP (1) viewed_at FROM RECIPE_VIEW WHERE recipe_id = ? AND " + identity
                        + " ORDER BY viewed_at DESC",
                (rs, row) -> rs.getTimestamp(1).toLocalDateTime(), recipeId,
                userId == null ? anonymousHash : userId).stream().findFirst();
    }

    public Optional<Long> incrementPublishedCount(long recipeId) {
        return jdbc.query("""
                UPDATE RECIPE_POST SET view_count = view_count + 1
                OUTPUT INSERTED.view_count WHERE recipe_id = ? AND status = 'PUBLISHED'
                """, (rs, row) -> rs.getLong(1), recipeId).stream().findFirst();
    }

    public void insert(long recipeId, Long userId, String anonymousHash, LocalDateTime viewedAt) {
        jdbc.update("INSERT INTO RECIPE_VIEW (recipe_id, user_id, anonymous_viewer_hash, viewed_at) VALUES (?, ?, ?, ?)",
                recipeId, userId, anonymousHash, viewedAt);
    }
}
