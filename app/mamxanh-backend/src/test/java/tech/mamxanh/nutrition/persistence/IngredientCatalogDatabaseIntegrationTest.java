package tech.mamxanh.nutrition.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.entity.CatalogStatus;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.entity.UnitEntity;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.nutrition.dto.request.ConversionSaveRequest;
import tech.mamxanh.nutrition.dto.request.IngredientSaveRequest;
import tech.mamxanh.nutrition.repository.IngredientRepository;
import tech.mamxanh.nutrition.repository.UnitRepository;
import tech.mamxanh.nutrition.service.IngredientCatalogService;

@SpringBootTest
@Transactional
class IngredientCatalogDatabaseIntegrationTest extends SqlServerIntegrationTest {
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private IngredientCatalogService catalogService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    @Test void v4SchemaStoresIngredientGroupAndRegistersPositiveUnitFactorConstraint() {
        String ingredientName = "Nguyên liệu kiểm thử " + UUID.randomUUID();
        IngredientEntity ingredient = ingredientRepository.saveAndFlush(
                new IngredientEntity(ingredientName, "Nguyên liệu khác", "Nguồn kiểm thử", null, LocalDate.now()));
        IngredientEntity inactiveIngredient = new IngredientEntity(
                "Nguyên liệu ngừng dùng " + UUID.randomUUID(), "Nguyên liệu khác", "Nguồn kiểm thử", null, LocalDate.now());
        inactiveIngredient.setActive(false);
        ingredientRepository.saveAndFlush(inactiveIngredient);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        UnitEntity activeUnit = unitRepository.saveAndFlush(
                new UnitEntity("u" + suffix, "Đơn vị kiểm thử", MeasurementDimension.COUNT, BigDecimal.ONE));
        UnitEntity inactiveUnit = new UnitEntity("x" + suffix, "Đơn vị ngừng dùng", MeasurementDimension.COUNT, BigDecimal.ONE);
        inactiveUnit.setActive(false);
        unitRepository.saveAndFlush(inactiveUnit);

        assertThat(ingredientRepository.findById(ingredient.getId()))
                .get().extracting(IngredientEntity::getIngredientGroup).isEqualTo("Nguyên liệu khác");
        assertThat(ingredientRepository.findByStatus(CatalogStatus.ACTIVE))
                .extracting(IngredientEntity::getName).contains(ingredientName).doesNotContain(inactiveIngredient.getName());
        assertThat(unitRepository.findByActiveTrue())
                .extracting(UnitEntity::getCode).contains(activeUnit.getCode()).doesNotContain(inactiveUnit.getCode());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys.check_constraints
                WHERE name = 'CK_UNIT_base_factor_positive'
                """, Integer.class)).isEqualTo(1);
    }

    @Test void storesIngredientSpecificConversionAndExposesItToRecipeValidation() {
        String suffix = UUID.randomUUID().toString();
        IngredientEntity ingredient = ingredientRepository.saveAndFlush(new IngredientEntity(
                "Nguyên liệu quy đổi " + suffix, "Rau củ", "Nguồn kiểm thử", null, LocalDate.now()));
        UnitEntity unit = unitRepository.saveAndFlush(new UnitEntity(
                "q" + suffix.replace("-", "").substring(0, 12), "quả kiểm thử", MeasurementDimension.COUNT, BigDecimal.ONE));

        var conversion = catalogService.createConversion(
                ingredient.getId(), unit.getId(), new ConversionSaveRequest(new BigDecimal("120.00"), true));

        assertThat(conversion.gramsPerUnit()).isEqualByComparingTo("120.00");
        assertThat(catalogService.findActiveGramsPerUnit(ingredient.getId(), unit.getId()))
                .contains(new BigDecimal("120.00"));

        ingredient.setActive(false);
        ingredientRepository.saveAndFlush(ingredient);
        assertThat(catalogService.findActiveGramsPerUnit(ingredient.getId(), unit.getId())).isEmpty();

        ingredient.setActive(true);
        ingredientRepository.saveAndFlush(ingredient);
        unit.setActive(false);
        unitRepository.saveAndFlush(unit);
        assertThat(catalogService.findActiveGramsPerUnit(ingredient.getId(), unit.getId())).isEmpty();
    }

    @Test void rejectsRemovingSourceFromNutritionSupportedIngredientBeforeMutation() {
        String name = "Nguyên liệu có nguồn " + UUID.randomUUID();
        IngredientEntity ingredient = ingredientRepository.saveAndFlush(new IngredientEntity(
                name, "Gia vị", "Nguồn kiểm thử", "https://example.com/source", LocalDate.now()));
        jdbcTemplate.update("""
                UPDATE INGREDIENT
                SET nutrition_supported = 1,
                    energy_kcal_100g = 1,
                    protein_g_100g = 1,
                    carbohydrate_g_100g = 1,
                    total_fat_g_100g = 1,
                    fiber_g_100g = 1,
                    calcium_mg_100g = 1,
                    iron_mg_100g = 1,
                    vitamin_b12_mcg_100g = 1,
                    zinc_mg_100g = 1
                WHERE ingredient_id = ?
                """, ingredient.getId());
        entityManager.clear();

        assertThatThrownBy(() -> catalogService.updateIngredient(ingredient.getId(),
                new IngredientSaveRequest(name, "Gia vị", "Nguồn kiểm thử", null, LocalDate.now())))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("SOURCE_REQUIRED_WHEN_NUTRITION_SUPPORTED");
                });
        assertThat(jdbcTemplate.queryForObject("SELECT source_url FROM INGREDIENT WHERE ingredient_id = ?",
                String.class, ingredient.getId())).isEqualTo("https://example.com/source");
    }

    @Test void storesTheSmallestSupportedPositiveConversionWithoutRounding() {
        IngredientEntity ingredient = ingredientRepository.saveAndFlush(new IngredientEntity(
                "Nguyên liệu quy đổi nhỏ " + UUID.randomUUID(), "Gia vị", "Nguồn kiểm thử", null, LocalDate.now()));
        String code = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        UnitEntity unit = unitRepository.saveAndFlush(new UnitEntity(
                code, "Đơn vị nhỏ", MeasurementDimension.COUNT, BigDecimal.ONE));

        var conversion = catalogService.createConversion(ingredient.getId(), unit.getId(),
                new ConversionSaveRequest(new BigDecimal("0.01"), true));

        assertThat(conversion.gramsPerUnit()).isEqualByComparingTo("0.01");
        BigDecimal storedValue = jdbcTemplate.queryForObject("""
                SELECT grams_per_unit
                FROM INGREDIENT_UNIT_CONVERSION
                WHERE ingredient_id = ? AND unit_id = ?
                """, BigDecimal.class, ingredient.getId(), unit.getId());
        assertThat(storedValue).isEqualByComparingTo("0.01");
    }
}
