package tech.mamxanh.nutrition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        when(conversionRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(conversion));

        assertThat(service.findActiveGramsPerUnit(1L, 2)).contains(new BigDecimal("120.00"));
    }

    @Test void searchesByIngredientNameOrGroup() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        when(ingredientRepository.findByNameContainingIgnoreCaseOrIngredientGroupContainingIgnoreCase("đậu", "đậu"))
                .thenReturn(java.util.List.of(ingredient));

        var results = service.listIngredients("  đậu  ");

        assertThat(results).singleElement().extracting(response -> response.name()).isEqualTo("Đậu phụ");
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

        var updated = service.updateUnit(2, new UnitSaveRequest("l", "lít", MeasurementDimension.VOLUME, new BigDecimal("1000")));
        var deactivated = service.setUnitStatus(2, new CatalogStatusUpdateRequest(false));

        assertThat(updated.code()).isEqualTo("l");
        assertThat(updated.baseFactor()).isEqualByComparingTo("1000");
        assertThat(deactivated.active()).isFalse();
    }
}
