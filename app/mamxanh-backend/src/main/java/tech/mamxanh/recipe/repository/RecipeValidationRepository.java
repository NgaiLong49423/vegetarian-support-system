package tech.mamxanh.recipe.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class RecipeValidationRepository {
    private final JdbcTemplate jdbcTemplate;

    public RecipeValidationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isActiveIngredient(Long ingredientId) {
        if (ingredientId == null) {
            return true;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM INGREDIENT WHERE ingredient_id = ? AND status = 'ACTIVE'", Integer.class, ingredientId);
        return count != null && count > 0;
    }

    public boolean hasValidConvertibleUnit(Long ingredientId, int unitId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM UNIT u
                WHERE u.unit_id = ? AND u.is_active = 1
                  AND (u.dimension = 'MASS' OR EXISTS (
                    SELECT 1 FROM INGREDIENT_UNIT_CONVERSION c
                    WHERE c.unit_id = u.unit_id AND c.ingredient_id = ? AND c.is_active = 1
                  ))
                """, Integer.class, unitId, ingredientId == null ? -1L : ingredientId);
        return count != null && count > 0;
    }

    public IngredientUnit findIngredientUnit(Long ingredientId, int unitId) {
        return jdbcTemplate.queryForObject("""
                SELECT i.name AS ingredient_name, u.code AS unit_code, u.name AS unit_name
                FROM UNIT u
                LEFT JOIN INGREDIENT i ON i.ingredient_id = ?
                WHERE u.unit_id = ?
                """, (rs, rowNum) -> new IngredientUnit(rs.getString("ingredient_name"),
                        rs.getString("unit_code"), rs.getString("unit_name")),
                ingredientId, unitId);
    }

    public List<IngredientOption> findIngredientOptions(String keyword) {
        return jdbcTemplate.query("""
                SELECT TOP (100) i.ingredient_id, i.name AS ingredient_name,
                       u.unit_id, u.code AS unit_code, u.name AS unit_name
                FROM INGREDIENT i
                JOIN UNIT u ON u.is_active = 1
                LEFT JOIN INGREDIENT_UNIT_CONVERSION c
                  ON c.ingredient_id = i.ingredient_id AND c.unit_id = u.unit_id AND c.is_active = 1
                WHERE i.status = 'ACTIVE'
                  AND (@keyword = '' OR i.name LIKE N'%' + @keyword + N'%')
                  AND (u.dimension = 'MASS' OR c.unit_id IS NOT NULL)
                ORDER BY i.name, u.unit_id
                """.replace("@keyword", "?"), (rs, rowNum) -> new IngredientOption(rs.getLong("ingredient_id"),
                        rs.getString("ingredient_name"), rs.getInt("unit_id"), rs.getString("unit_code"),
                        rs.getString("unit_name")), keyword, keyword);
    }

    public List<UnitOption> findCustomIngredientUnits() {
        return jdbcTemplate.query("SELECT unit_id, code, name FROM UNIT WHERE is_active = 1 AND dimension = 'MASS' ORDER BY unit_id",
                (rs, rowNum) -> new UnitOption(rs.getInt("unit_id"), rs.getString("code"), rs.getString("name")));
    }

    public record IngredientUnit(String ingredientName, String unitCode, String unitName) { }
    public record IngredientOption(long ingredientId, String name, int unitId, String unitCode, String unitName) { }
    public record UnitOption(int unitId, String code, String name) { }
}
