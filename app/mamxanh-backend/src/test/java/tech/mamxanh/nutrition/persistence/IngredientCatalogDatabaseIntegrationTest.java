package tech.mamxanh.nutrition.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.entity.CatalogStatus;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.entity.UnitEntity;
import tech.mamxanh.nutrition.dto.request.ConversionSaveRequest;
import tech.mamxanh.nutrition.repository.IngredientRepository;
import tech.mamxanh.nutrition.repository.UnitRepository;
import tech.mamxanh.nutrition.service.IngredientCatalogService;

@SpringBootTest
@Transactional
class IngredientCatalogDatabaseIntegrationTest {
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private IngredientCatalogService catalogService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test void v3SchemaStoresIngredientGroupAndRegistersPositiveUnitFactorConstraint() {
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
    }
}
