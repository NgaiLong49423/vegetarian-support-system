package tech.mamxanh.nutrition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.nutrition.dto.request.ConversionSaveRequest;
import tech.mamxanh.nutrition.dto.request.CatalogStatusUpdateRequest;
import tech.mamxanh.nutrition.dto.request.IngredientSaveRequest;
import tech.mamxanh.nutrition.dto.request.UnitSaveRequest;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionId;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.entity.UnitEntity;
import tech.mamxanh.nutrition.repository.IngredientRepository;
import tech.mamxanh.nutrition.repository.IngredientUnitConversionRepository;
import tech.mamxanh.nutrition.repository.UnitRepository;

@ExtendWith(MockitoExtension.class)
class IngredientCatalogServiceTest {
    @Mock private IngredientRepository ingredientRepository;
    @Mock private UnitRepository unitRepository;
    @Mock private IngredientUnitConversionRepository conversionRepository;
    private IngredientCatalogService service;

    @BeforeEach void setUp() { service = new IngredientCatalogService(ingredientRepository, unitRepository, conversionRepository); }

    @Test void createsVietnameseIngredientWithNutritionDisabledUntilDataIsComplete() {
        IngredientSaveRequest request = new IngredientSaveRequest("Chuối tây", "Trái cây", "USDA", null, LocalDate.of(2026, 9, 1));
        when(ingredientRepository.existsByNameIgnoreCase("Chuối tây")).thenReturn(false);
        when(ingredientRepository.save(org.mockito.ArgumentMatchers.any(IngredientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createIngredient(request);

        assertThat(response.name()).isEqualTo("Chuối tây");
        assertThat(response.ingredientGroup()).isEqualTo("Trái cây");
        assertThat(response.nutritionSupported()).isFalse();
    }

    @Test void rejectsDuplicateIngredientName() {
        IngredientSaveRequest request = new IngredientSaveRequest("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        when(ingredientRepository.existsByNameIgnoreCase("Đậu phụ")).thenReturn(true);

        assertThatThrownBy(() -> service.createIngredient(request)).isInstanceOf(ApiException.class).hasMessage("Tên nguyên liệu đã tồn tại.");
    }

    @Test void rejectsDuplicateConversionPair() {
        IngredientEntity ingredient = new IngredientEntity("Chuối tây", "Trái cây", "USDA", null, LocalDate.now());
        UnitEntity unit = new UnitEntity("quả", "Quả", MeasurementDimension.COUNT, BigDecimal.ONE);
        IngredientUnitConversionId id = new IngredientUnitConversionId(1L, 2);
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));
        when(conversionRepository.existsById(id)).thenReturn(true);

        assertThatThrownBy(() -> service.createConversion(1L, 2, new ConversionSaveRequest(new BigDecimal("120"), true)))
                .isInstanceOf(ApiException.class).hasMessage("Cặp nguyên liệu và đơn vị đã có tỷ lệ quy đổi.");
    }

    @Test void exposesOnlyActiveConversionToDependentRecipeValidation() {
        IngredientUnitConversionId id = new IngredientUnitConversionId(1L, 2);
        IngredientUnitConversionEntity conversion = new IngredientUnitConversionEntity(id, new BigDecimal("120.00"), true);
        IngredientEntity ingredient = new IngredientEntity("Chuối tây", "Trái cây", "USDA", null, LocalDate.now());
        UnitEntity unit = new UnitEntity("quả", "Quả", MeasurementDimension.COUNT, BigDecimal.ONE);
        when(conversionRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(conversion));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        assertThat(service.findActiveGramsPerUnit(1L, 2)).contains(new BigDecimal("120.00"));

        ingredient.setActive(false);
        assertThat(service.findActiveGramsPerUnit(1L, 2)).isEmpty();
        ingredient.setActive(true);
        unit.setActive(false);
        assertThat(service.findActiveGramsPerUnit(1L, 2)).isEmpty();
    }

    @Test void rejectsMassUnitConversionOnCreate() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        UnitEntity unit = new UnitEntity("g", "Gam", MeasurementDimension.MASS, BigDecimal.ONE);
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> service.createConversion(1L, 2, new ConversionSaveRequest(new BigDecimal("100"), false)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("MASS_CONVERSION_NOT_ALLOWED");
                });
    }

    @Test void searchesByIngredientNameOrGroup() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        when(ingredientRepository.findByNameContainingIgnoreCaseOrIngredientGroupContainingIgnoreCase("đậu", "đậu"))
                .thenReturn(java.util.List.of(ingredient));

        var results = service.listIngredients("  đậu  ");

        assertThat(results).singleElement().extracting(response -> response.name()).isEqualTo("Đậu phụ");
    }

    @Test void listsAllIngredientsForNullOrBlankQueryAndSortsByName() {
        IngredientEntity banana = new IngredientEntity("Chuối", "Trái cây", "USDA", null, LocalDate.now());
        IngredientEntity tofu = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        when(ingredientRepository.findAll()).thenReturn(List.of(banana, tofu));

        assertThat(service.listIngredients(null)).extracting(response -> response.name())
                .containsExactly("Chuối", "Đậu phụ");
        assertThat(service.listIngredients("  ")).extracting(response -> response.name())
                .containsExactly("Chuối", "Đậu phụ");
    }

    @Test void searchesActiveIngredientsByGroupAndHandlesEmptyAndMissingQueries() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        when(ingredientRepository.findByStatus(tech.mamxanh.nutrition.entity.CatalogStatus.ACTIVE))
                .thenReturn(List.of(ingredient));

        assertThat(service.findActiveIngredients("hạt")).extracting(response -> response.name())
                .containsExactly("Đậu phụ");
        assertThat(service.findActiveIngredients(null)).extracting(response -> response.name())
                .containsExactly("Đậu phụ");
        assertThat(service.findActiveIngredients("   ")).extracting(response -> response.name())
                .containsExactly("Đậu phụ");
        assertThat(service.findActiveIngredients("không có")).isEmpty();
    }

    @Test void deactivatesIngredientWithoutDeletingItsRecord() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));

        var response = service.setIngredientStatus(1L, new CatalogStatusUpdateRequest(false));

        assertThat(response.active()).isFalse();
        assertThat(ingredient.getStatus().name()).isEqualTo("INACTIVE");
    }

    @Test void rejectsConversionForInactiveIngredient() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        ingredient.setActive(false);
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));

        assertThatThrownBy(() -> service.createConversion(1L, 2, new ConversionSaveRequest(new BigDecimal("120"), false)))
                .isInstanceOf(ApiException.class).hasMessage("Nguyên liệu đã ngừng sử dụng.");
    }

    @Test void exposesOnlyActiveIngredientsAndUnitsForNewRecipeLinks() {
        IngredientEntity activeIngredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        UnitEntity activeUnit = new UnitEntity("g", "gam", MeasurementDimension.MASS, BigDecimal.ONE);
        when(ingredientRepository.findByStatus(tech.mamxanh.nutrition.entity.CatalogStatus.ACTIVE)).thenReturn(java.util.List.of(activeIngredient));
        when(unitRepository.findByActiveTrue()).thenReturn(java.util.List.of(activeUnit));

        assertThat(service.findActiveIngredients("đậu")).extracting(response -> response.name()).containsExactly("Đậu phụ");
        assertThat(service.findActiveUnits()).extracting(response -> response.code()).containsExactly("g");
    }

    @Test void updatesAndDeactivatesUnitWithoutDeletingIt() {
        UnitEntity unit = new UnitEntity("ml", "mililit", MeasurementDimension.VOLUME, BigDecimal.ONE);
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        var updated = service.updateUnit(2, new UnitSaveRequest("l", "lít", MeasurementDimension.MASS, new BigDecimal("1000")));
        var deactivated = service.setUnitStatus(2, new CatalogStatusUpdateRequest(false));

        assertThat(updated.code()).isEqualTo("l");
        assertThat(updated.baseFactor()).isEqualByComparingTo("1000");
        assertThat(updated.dimension()).isEqualTo(MeasurementDimension.MASS);
        assertThat(deactivated.active()).isFalse();
    }

    @Test void rejectsChangingUnitDimensionWhenConversionsExist() {
        UnitEntity unit = new UnitEntity("ml", "Mililit", MeasurementDimension.VOLUME, BigDecimal.ONE);
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));
        when(conversionRepository.existsByIdUnitId(2)).thenReturn(true);

        assertThatThrownBy(() -> service.updateUnit(2,
                new UnitSaveRequest("g", "Gam", MeasurementDimension.MASS, BigDecimal.ONE)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("UNIT_HAS_INGREDIENT_CONVERSIONS");
                });
        assertThat(unit.getDimension()).isEqualTo(MeasurementDimension.VOLUME);
    }

    @Test void updatesUnitWithSameCodeAndRejectsAnotherUnitsCode() {
        UnitEntity unit = new UnitEntity("g", "gam", MeasurementDimension.MASS, BigDecimal.ONE);
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        var updated = service.updateUnit(2, new UnitSaveRequest("g", "gam mới", MeasurementDimension.MASS, BigDecimal.ONE));
        assertThat(updated.name()).isEqualTo("gam mới");

        when(unitRepository.existsByCodeIgnoreCase("ml")).thenReturn(true);
        assertThatThrownBy(() -> service.updateUnit(2,
                new UnitSaveRequest("ml", "mililit", MeasurementDimension.VOLUME, BigDecimal.ONE)))
                .isInstanceOf(ApiException.class).hasMessage("Ký hiệu đơn vị đã tồn tại.");
    }

    @Test void updatesIngredientMetadataAndRejectsRenamingToAnExistingName() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        var request = new IngredientSaveRequest(" Đậu hũ ", " Đậu hạt ", " USDA ", "https://example.com/source", LocalDate.of(2026, 9, 1));

        var updated = service.updateIngredient(1L, request);

        assertThat(updated.name()).isEqualTo("Đậu hũ");
        assertThat(updated.sourceUrl()).isEqualTo("https://example.com/source");
        when(ingredientRepository.existsByNameIgnoreCase("Chuối")).thenReturn(true);
        assertThatThrownBy(() -> service.updateIngredient(1L,
                new IngredientSaveRequest("Chuối", "Trái cây", "USDA", null, LocalDate.now())))
                .isInstanceOf(ApiException.class).hasMessage("Tên nguyên liệu đã tồn tại.");
        assertThat(ingredient.getName()).isEqualTo("Đậu hũ");
    }

    @Test void createsTrimmedUnitAndRejectsDuplicateCode() {
        when(unitRepository.save(org.mockito.ArgumentMatchers.any(UnitEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new UnitSaveRequest(" kg ", " kilôgam ", MeasurementDimension.MASS, new BigDecimal("1000"));

        var created = service.createUnit(request);

        assertThat(created.code()).isEqualTo("kg");
        assertThat(created.name()).isEqualTo("kilôgam");
        assertThat(created.baseFactor()).isEqualByComparingTo("1000");
        when(unitRepository.existsByCodeIgnoreCase("kg")).thenReturn(true);
        assertThatThrownBy(() -> service.createUnit(request)).isInstanceOf(ApiException.class)
                .hasMessage("Ký hiệu đơn vị đã tồn tại.");
    }

    @Test void updatesConversionAndDisablesItWithoutChangingItsPair() {
        var id = new IngredientUnitConversionId(1L, 2);
        var conversion = new IngredientUnitConversionEntity(id, new BigDecimal("120"), false);
        UnitEntity unit = new UnitEntity("quả", "Quả", MeasurementDimension.COUNT, BigDecimal.ONE);
        when(conversionRepository.findById(id)).thenReturn(Optional.of(conversion));
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        var updated = service.updateConversion(1L, 2, new ConversionSaveRequest(new BigDecimal("135"), true));
        var disabled = service.setConversionStatus(1L, 2, new CatalogStatusUpdateRequest(false));

        assertThat(updated.gramsPerUnit()).isEqualByComparingTo("135");
        assertThat(updated.approximate()).isTrue();
        assertThat(disabled.ingredientId()).isEqualTo(1L);
        assertThat(disabled.unitId()).isEqualTo(2);
        assertThat(disabled.active()).isFalse();
    }

    @Test void listsConversionsWithAndWithoutAnIngredientFilter() {
        IngredientUnitConversionEntity first = new IngredientUnitConversionEntity(
                new IngredientUnitConversionId(1L, 2), new BigDecimal("120"), true);
        IngredientUnitConversionEntity second = new IngredientUnitConversionEntity(
                new IngredientUnitConversionId(3L, 4), new BigDecimal("250"), false);
        when(conversionRepository.findAll()).thenReturn(List.of(first, second));
        when(conversionRepository.findByIdIngredientId(1L)).thenReturn(List.of(first));

        assertThat(service.listConversions(null)).hasSize(2);
        assertThat(service.listConversions(1L)).singleElement()
                .satisfies(response -> assertThat(response.ingredientId()).isEqualTo(1L));
    }

    @Test void rejectsMassUnitConversionOnUpdate() {
        var id = new IngredientUnitConversionId(1L, 2);
        var conversion = new IngredientUnitConversionEntity(id, new BigDecimal("120"), true);
        UnitEntity unit = new UnitEntity("kg", "Kilogram", MeasurementDimension.MASS, new BigDecimal("1000"));
        when(conversionRepository.findById(id)).thenReturn(Optional.of(conversion));
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> service.updateConversion(1L, 2, new ConversionSaveRequest(new BigDecimal("135"), false)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("MASS_CONVERSION_NOT_ALLOWED");
                });
        assertThat(conversion.getGramsPerUnit()).isEqualByComparingTo("120");
    }

    @Test void rejectsReactivatingMassUnitConversionButAllowsDeactivation() {
        var id = new IngredientUnitConversionId(1L, 2);
        var conversion = new IngredientUnitConversionEntity(id, new BigDecimal("120"), false);
        conversion.setActive(false);
        UnitEntity unit = new UnitEntity("g", "Gam", MeasurementDimension.MASS, BigDecimal.ONE);
        when(conversionRepository.findById(id)).thenReturn(Optional.of(conversion));
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> service.setConversionStatus(1L, 2, new CatalogStatusUpdateRequest(true)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("MASS_CONVERSION_NOT_ALLOWED");
                });
        assertThat(conversion.isActive()).isFalse();

        conversion.setActive(true);
        service.setConversionStatus(1L, 2, new CatalogStatusUpdateRequest(false));
        assertThat(conversion.isActive()).isFalse();
    }

    @Test void mapsConcurrentDuplicateInsertToConflictAndRethrowsOtherDatabaseErrors() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        UnitEntity unit = new UnitEntity("quả", "Quả", MeasurementDimension.COUNT, BigDecimal.ONE);
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));
        SQLException duplicateKey = new SQLException("Duplicate key", "23000", 2627);
        doThrow(new DataIntegrityViolationException("Duplicate key", duplicateKey))
                .when(conversionRepository).insertNew(1L, 2, new BigDecimal("120"), true);

        assertThatThrownBy(() -> service.createConversion(1L, 2,
                new ConversionSaveRequest(new BigDecimal("120"), true)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Cặp nguyên liệu và đơn vị đã có tỷ lệ quy đổi.");

        DataIntegrityViolationException otherConstraint = new DataIntegrityViolationException(
                "Check constraint", new SQLException("Check constraint", "23000", 547));
        doThrow(otherConstraint).when(conversionRepository).insertNew(1L, 2, new BigDecimal("135"), false);
        assertThatThrownBy(() -> service.createConversion(1L, 2,
                new ConversionSaveRequest(new BigDecimal("135"), false)))
                .isSameAs(otherConstraint);
    }

    @Test void hidesConversionWhenItIsMissingOrItsIngredientOrUnitIsMissing() {
        IngredientUnitConversionId id = new IngredientUnitConversionId(1L, 2);
        when(conversionRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());
        assertThat(service.findActiveGramsPerUnit(1L, 2)).isEmpty();

        when(conversionRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(
                new IngredientUnitConversionEntity(id, new BigDecimal("120"), true)));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.empty());
        when(unitRepository.findById(2)).thenReturn(Optional.empty());
        assertThat(service.findActiveGramsPerUnit(1L, 2)).isEmpty();
    }

    @Test void rejectsHardDeleteAndComparesConversionIdsByBothKeys() {
        assertThatThrownBy(() -> service.rejectHardDelete("nguyên liệu"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Không hỗ trợ xóa vĩnh viễn nguyên liệu; hãy chuyển sang trạng thái ngừng sử dụng.");

        IngredientUnitConversionId id = new IngredientUnitConversionId(1L, 2);
        assertThat(id).isEqualTo(new IngredientUnitConversionId(1L, 2))
                .isNotEqualTo(new IngredientUnitConversionId(2L, 2))
                .isNotEqualTo(new IngredientUnitConversionId(1L, 3))
                .isNotEqualTo(null)
                .hasSameHashCodeAs(new IngredientUnitConversionId(1L, 2));
    }

    @Test void missingCatalogResourcesReturnNotFoundInsteadOfBeingCreated() {
        assertThatThrownBy(() -> service.setIngredientStatus(99L, new CatalogStatusUpdateRequest(false)))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
                    assertThat(error.getCode()).isEqualTo("INGREDIENT_NOT_FOUND");
                });
        assertThatThrownBy(() -> service.setUnitStatus(99, new CatalogStatusUpdateRequest(false)))
                .isInstanceOf(ApiException.class).hasMessage("Không tìm thấy đơn vị.");
        assertThatThrownBy(() -> service.updateConversion(99L, 99, new ConversionSaveRequest(BigDecimal.ONE, false)))
                .isInstanceOf(ApiException.class).hasMessage("Không tìm thấy tỷ lệ quy đổi.");
    }

    @Test void rejectsNewConversionForAnInactiveUnit() {
        var ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        var unit = new UnitEntity("quả", "Quả", MeasurementDimension.COUNT, BigDecimal.ONE);
        unit.setActive(false);
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> service.createConversion(1L, 2, new ConversionSaveRequest(new BigDecimal("120"), false)))
                .isInstanceOf(ApiException.class).hasMessage("Đơn vị đã ngừng sử dụng.");
    }
}
