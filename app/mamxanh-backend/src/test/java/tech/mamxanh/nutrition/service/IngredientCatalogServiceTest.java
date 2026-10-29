package tech.mamxanh.nutrition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.nutrition.dto.request.ConversionSaveRequest;
import tech.mamxanh.nutrition.dto.request.CatalogStatusUpdateRequest;
import tech.mamxanh.nutrition.dto.request.IngredientSaveRequest;
import tech.mamxanh.nutrition.dto.request.IngredientNutritionRequest;
import tech.mamxanh.nutrition.dto.request.NutritionSupportUpdateRequest;
import tech.mamxanh.nutrition.dto.request.UnitSaveRequest;
import tech.mamxanh.nutrition.dto.request.VolumeConversionSaveRequest;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.entity.CatalogStatus;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionId;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.entity.UnitEntity;
import tech.mamxanh.nutrition.config.NutritionCatalogProperties;
import tech.mamxanh.nutrition.repository.IngredientRepository;
import tech.mamxanh.nutrition.repository.IngredientUnitConversionRepository;
import tech.mamxanh.nutrition.repository.UnitRepository;

@ExtendWith(MockitoExtension.class)
class IngredientCatalogServiceTest {
    @Test void batchReferencesKeepInactiveHistoryAndHandleEmptyInput() {
        var ingredient = new IngredientEntity("Nấm", "Nấm", "Nguồn", null, LocalDate.of(2026,10,17));
        org.springframework.test.util.ReflectionTestUtils.setField(ingredient,"id",81L);
        ingredient.setActive(false);
        var unit = new UnitEntity("old","Đơn vị cũ",MeasurementDimension.COUNT,BigDecimal.ONE);
        org.springframework.test.util.ReflectionTestUtils.setField(unit,"id",91); unit.setActive(false);
        when(ingredientRepository.findAllById(List.of(81L))).thenReturn(List.of(ingredient));
        when(unitRepository.findAllById(List.of(91))).thenReturn(List.of(unit));
        assertThat(service.findIngredientReferences(List.of(81L))).singleElement().satisfies(i -> assertThat(i.active()).isFalse());
        assertThat(service.findUnitReferences(List.of(91))).singleElement().satisfies(u -> assertThat(u.active()).isFalse());
        assertThat(service.findIngredientReferences(null)).isEmpty();
        assertThat(service.findUnitReferences(List.of())).isEmpty();
    }

    @Test void batchConversionLookupRequiresActiveIngredientAndUnitAsWellAsActiveRate() {
        var ingredient = new IngredientEntity("Nấm", "Nấm", "Nguồn", null, LocalDate.of(2026,10,17));
        org.springframework.test.util.ReflectionTestUtils.setField(ingredient,"id",1L);
        var unit = new UnitEntity("piece","Miếng",MeasurementDimension.COUNT,BigDecimal.ONE);
        org.springframework.test.util.ReflectionTestUtils.setField(unit,"id",2);
        var valid = new IngredientUnitConversionEntity(new IngredientUnitConversionId(1L,2),new BigDecimal("120"),false);
        var unavailableUnit = new IngredientUnitConversionEntity(new IngredientUnitConversionId(1L,99),new BigDecimal("200"),false);
        when(ingredientRepository.findByIdInAndStatus(List.of(1L),CatalogStatus.ACTIVE)).thenReturn(List.of(ingredient));
        when(unitRepository.findByActiveTrue()).thenReturn(List.of(unit));
        when(conversionRepository.findByIdIngredientIdInAndActiveTrue(List.of(1L))).thenReturn(List.of(valid,unavailableUnit));
        assertThat(service.findActiveConversions(List.of(1L))).singleElement().satisfies(r -> {
            assertThat(r.unitId()).isEqualTo(2); assertThat(r.gramsPerUnit()).isEqualByComparingTo("120");
        });
    }

    @Test void batchConversionLookupReturnsNoRatesForEmptyOrInactiveIngredients() {
        assertThat(service.findActiveConversions(List.of())).isEmpty();
        when(ingredientRepository.findByIdInAndStatus(List.of(1L),CatalogStatus.ACTIVE)).thenReturn(List.of());
        assertThat(service.findActiveConversions(List.of(1L))).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(unitRepository,conversionRepository);
    }
    @Mock private IngredientRepository ingredientRepository;
    @Mock private UnitRepository unitRepository;
    @Mock private IngredientUnitConversionRepository conversionRepository;
    private IngredientCatalogService service;

    @BeforeEach void setUp() {
        service = new IngredientCatalogService(ingredientRepository, unitRepository, conversionRepository,
                new NutritionCatalogProperties(List.of("USDA FoodData Central", "NIH/Viện Dinh dưỡng")));
    }

    @Test void createsVietnameseIngredientWithNutritionDisabledUntilDataIsComplete() {
        IngredientSaveRequest request = new IngredientSaveRequest("Chuối tây", "Trái cây", "USDA FoodData Central", null, LocalDate.of(2026, 9, 1));
        when(ingredientRepository.existsByNameIgnoreCaseAndStatus("Chuối tây", CatalogStatus.ACTIVE)).thenReturn(false);
        when(ingredientRepository.save(org.mockito.ArgumentMatchers.any(IngredientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createIngredient(request);

        assertThat(response.name()).isEqualTo("Chuối tây");
        assertThat(response.ingredientGroup()).isEqualTo("Trái cây");
        assertThat(response.nutritionSupported()).isFalse();
    }

    @Test void publicGroupListContainsOnlyTheRepositoryActiveGroupCounts() {
        IngredientRepository.ActiveIngredientGroupView group = new IngredientRepository.ActiveIngredientGroupView() {
            @Override public String getGroupName() { return "Rau củ"; }
            @Override public Long getIngredientCount() { return 3L; }
        };
        when(ingredientRepository.findActiveIngredientGroups()).thenReturn(List.of(group));

        var result = service.listPublicGroups();

        assertThat(result).containsExactly(new tech.mamxanh.nutrition.dto.response.PublicIngredientGroupResponse("Rau củ", 3));
    }

    @Test void publicIngredientSearchReturnsOnlyActiveCatalogIdentityFields() {
        IngredientEntity ingredient = org.mockito.Mockito.mock(IngredientEntity.class);
        when(ingredient.getId()).thenReturn(12L);
        when(ingredient.getName()).thenReturn("Cải thìa");
        when(ingredient.getIngredientGroup()).thenReturn("Rau củ");
        when(ingredientRepository.search(eq("%cải%"), eq("Rau củ"), eq(CatalogStatus.ACTIVE), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ingredient), Pageable.ofSize(20), 1));

        var result = service.searchPublicIngredients("cải", "Rau củ", 0, 20);

        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("Cải thìa");
            assertThat(item.ingredientGroup()).isEqualTo("Rau củ");
            assertThat(item.ingredientId()).isEqualTo(12L);
        });
    }

    @Test void publicDetailPreservesKnownZeroUnknownAndSmallMicronutrients() {
        IngredientEntity ingredient = new IngredientEntity("Phô mai", "Sữa", "Private source", null, LocalDate.now());
        org.springframework.test.util.ReflectionTestUtils.setField(ingredient, "id", 42L);
        ingredient.setImageUrl("https://example.test/cheese.png");
        ingredient.updateNutrition(new BigDecimal("333"), BigDecimal.TEN, BigDecimal.ZERO,
                null, null, new BigDecimal("200"), null, new BigDecimal("0.0001"), null);
        when(ingredientRepository.findByIdAndStatus(42L, CatalogStatus.ACTIVE)).thenReturn(Optional.of(ingredient));

        var response = service.getPublicIngredient(42L);

        assertThat(response.ingredientId()).isEqualTo(42L);
        assertThat(response.imageUrl()).isEqualTo("https://example.test/cheese.png");
        assertThat(response.nutrition().carbohydrateGramsPer100g()).isEqualByComparingTo("0");
        assertThat(response.nutrition().totalFatGramsPer100g()).isNull();
        assertThat(response.nutrition().vitaminB12MicrogramsPer100g()).isEqualByComparingTo("0.0001");
    }

    @Test void publicDetailDoesNotFallBackToInactiveOrUnfilteredRecords() {
        when(ingredientRepository.findByIdAndStatus(42L, CatalogStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicIngredient(42L)).isInstanceOfSatisfying(ApiException.class, error -> {
            assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(error.getCode()).isEqualTo("INGREDIENT_NOT_FOUND");
        });
        org.mockito.Mockito.verify(ingredientRepository, org.mockito.Mockito.never()).findById(any());
    }

    @Test void rejectsDuplicateIngredientName() {
        IngredientSaveRequest request = new IngredientSaveRequest("Đậu phụ", "Đậu hạt", "USDA FoodData Central", null, LocalDate.now());
        when(ingredientRepository.existsByNameIgnoreCaseAndStatus("Đậu phụ", CatalogStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.createIngredient(request)).isInstanceOf(ApiException.class).hasMessage("Tên nguyên liệu đã tồn tại trong danh mục đang hoạt động.");
    }

    @Test void storesUnknownNutritionAsNullAndDoesNotEnableSupportOnCreate() {
        when(ingredientRepository.existsByNameIgnoreCaseAndStatus("Rau mới", CatalogStatus.ACTIVE)).thenReturn(false);
        when(ingredientRepository.save(org.mockito.ArgumentMatchers.any(IngredientEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.createIngredient(new IngredientSaveRequest("Rau mới", "Rau củ",
                "USDA FoodData Central", null, LocalDate.now()));

        assertThat(created.nutritionSupported()).isFalse();
        assertThat(created.nutrition().energyKcalPer100g()).isNull();
        assertThat(created.nutrition().vitaminB12MicrogramsPer100g()).isNull();
    }

    @Test void allowsAIPartialNutritionWithoutAReferenceUrlWhenEnablingSupport() {
        IngredientEntity ingredient = new IngredientEntity("Đậu xanh", "Đậu hạt", "Ước tính AI chưa xác minh",
                null, LocalDate.now());
        when(ingredientRepository.findById(4L)).thenReturn(Optional.of(ingredient));

        assertThatThrownBy(() -> service.setNutritionSupport(4L, new NutritionSupportUpdateRequest(true)))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("NUTRITION_VALUES_EMPTY");
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        service.updateNutrition(4L, new IngredientNutritionRequest(
                BigDecimal.TEN, null, null, null, null, null, null, null, null));
        var enabled = service.setNutritionSupport(4L, new NutritionSupportUpdateRequest(true));
        assertThat(enabled.nutritionSupported()).isTrue();
    }

    @Test void rejectsReactivatingAnIngredientWhenAnotherActiveRecordHasTheSameName() {
        IngredientEntity inactiveIngredient = new IngredientEntity("Đậu xanh", "Đậu hạt",
                "USDA FoodData Central", null, LocalDate.now());
        inactiveIngredient.setActive(false);
        when(ingredientRepository.findById(41L)).thenReturn(Optional.of(inactiveIngredient));
        when(ingredientRepository.existsByNameIgnoreCaseAndStatusAndIdNot(
                "Đậu xanh", CatalogStatus.ACTIVE, 41L)).thenReturn(true);

        assertThatThrownBy(() -> service.setIngredientStatus(41L, new CatalogStatusUpdateRequest(true)))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("INGREDIENT_NAME_EXISTS");
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
        assertThat(inactiveIngredient.getStatus()).isEqualTo(CatalogStatus.INACTIVE);
    }

    @Test void allowsIndividualNutritionValuesToBeClearedWhileSupportRemainsEnabled() {
        BigDecimal[] completeValues = {BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE};

        for (int missingIndex = 0; missingIndex < completeValues.length; missingIndex++) {
            IngredientEntity ingredient = new IngredientEntity("Đậu xanh", "Đậu hạt", "USDA FoodData Central",
                    "https://fdc.nal.usda.gov/", LocalDate.now());
            ingredient.updateNutrition(completeValues[0], completeValues[1], completeValues[2], completeValues[3],
                    completeValues[4], completeValues[5], completeValues[6], completeValues[7], completeValues[8]);
            ingredient.setNutritionSupported(true);
            BigDecimal[] partialValues = completeValues.clone();
            partialValues[missingIndex] = null;
            long ingredientId = 100L + missingIndex;
            when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.of(ingredient));

            service.updateNutrition(ingredientId, new IngredientNutritionRequest(
                    partialValues[0], partialValues[1], partialValues[2], partialValues[3], partialValues[4],
                    partialValues[5], partialValues[6], partialValues[7], partialValues[8]));
            assertThat(ingredient.isNutritionSupported()).isTrue();
            assertThat(ingredient.getEnergyKcalPer100g()).isEqualTo(partialValues[0]);
        }
    }

    @Test void doesNotRequireReferenceMetadataBeforeEnablingNutritionSupport() {
        IngredientEntity ingredient = new IngredientEntity("Đậu xanh", "Đậu hạt", "AI estimate", null, LocalDate.now());
        ingredient.updateNutrition(BigDecimal.TEN, null, null, null, null, null, null, null, null);
        when(ingredientRepository.findById(8L)).thenReturn(Optional.of(ingredient));

        assertThat(service.setNutritionSupport(8L, new NutritionSupportUpdateRequest(true)).nutritionSupported()).isTrue();
    }

    @Test void searchesWithNoActiveStatusFilterAndNormalizesBlankFilters() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA FoodData Central",
                "https://fdc.nal.usda.gov/", LocalDate.now());
        when(ingredientRepository.search(org.mockito.ArgumentMatchers.isNull(String.class),
                org.mockito.ArgumentMatchers.isNull(String.class),
                org.mockito.ArgumentMatchers.isNull(tech.mamxanh.nutrition.entity.CatalogStatus.class),
                org.mockito.ArgumentMatchers.isNull(Boolean.class),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ingredient)));

        var result = service.searchIngredients("  ", "   ", null, null, 0, 20);

        assertThat(result.content()).extracting(response -> response.name()).containsExactly("Đậu phụ");
    }

    @Test void allowsNutritionSupportWithoutASourceUrl() {
        IngredientEntity ingredient = new IngredientEntity("Đậu xanh", "Đậu hạt", "USDA FoodData Central",
                null, LocalDate.now());
        ingredient.updateNutrition(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        when(ingredientRepository.findById(7L)).thenReturn(Optional.of(ingredient));

        assertThat(service.setNutritionSupport(7L, new NutritionSupportUpdateRequest(true)).nutritionSupported()).isTrue();
    }

    @Test void rejectsClearingAllNutritionWhileSupportIsEnabled() {
        IngredientEntity ingredient = new IngredientEntity("Đậu đỏ", "Đậu hạt", "USDA FoodData Central",
                "https://fdc.nal.usda.gov/", LocalDate.now());
        ingredient.updateNutrition(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        ingredient.setNutritionSupported(true);
        when(ingredientRepository.findById(5L)).thenReturn(Optional.of(ingredient));

        assertThatThrownBy(() -> service.updateNutrition(5L, new IngredientNutritionRequest(
                null, null, null, null, null, null, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, error ->
                    assertThat(error.getCode()).isEqualTo("NUTRITION_VALUES_EMPTY"));
        assertThat(ingredient.getEnergyKcalPer100g()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(ingredient.isNutritionSupported()).isTrue();
    }

    @Test void allowsPartialNutritionThroughIngredientUpdateWhileSupportIsEnabled() {
        IngredientEntity ingredient = new IngredientEntity("Đậu đỏ", "Đậu hạt", "USDA FoodData Central",
                "https://fdc.nal.usda.gov/", LocalDate.now());
        ingredient.updateNutrition(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        ingredient.setNutritionSupported(true);
        when(ingredientRepository.findById(6L)).thenReturn(Optional.of(ingredient));

        service.updateIngredient(6L, new IngredientSaveRequest("Đậu đỏ",
                "Đậu hạt", "USDA FoodData Central", "https://fdc.nal.usda.gov/", LocalDate.now(),
                new IngredientNutritionRequest(null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                        BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)));
        assertThat(ingredient.getEnergyKcalPer100g()).isNull();
    }

    @Test void acceptsFreeFormNutritionSource() {
        org.mockito.Mockito.when(ingredientRepository.save(org.mockito.ArgumentMatchers.any(IngredientEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.createIngredient(new IngredientSaveRequest("Rau không rõ nguồn",
                "Rau củ", "Tự nhập - AI estimate", null, LocalDate.now())).sourceName())
                .isEqualTo("Tự nhập - AI estimate");
        org.mockito.Mockito.verify(ingredientRepository).save(org.mockito.ArgumentMatchers.any(IngredientEntity.class));
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

    @Test void listsAllIngredientsForNullOrBlankQueryAndSortsByCreationTimeDescending() {
        IngredientEntity banana = new IngredientEntity("Chuối", "Trái cây", "USDA", null, LocalDate.now());
        IngredientEntity tofu = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        org.springframework.test.util.ReflectionTestUtils.setField(banana, "createdAt", LocalDate.now().atStartOfDay());
        org.springframework.test.util.ReflectionTestUtils.setField(tofu, "createdAt", LocalDate.now().atTime(12, 0));
        when(ingredientRepository.findAll()).thenReturn(List.of(banana, tofu));

        assertThat(service.listIngredients(null)).extracting(response -> response.name())
                .containsExactly("Đậu phụ", "Chuối");
        assertThat(service.listIngredients("  ")).extracting(response -> response.name())
                .containsExactly("Đậu phụ", "Chuối");
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

    @Test void updatesAndDeactivatesLegacyUnitWithoutDeletingIt() {
        UnitEntity unit = new UnitEntity("bìa", "bìa", MeasurementDimension.COUNT, BigDecimal.ONE);
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        var updated = service.updateUnit(2, new UnitSaveRequest("hộp", "hộp", MeasurementDimension.COUNT, BigDecimal.ONE));
        var deactivated = service.setUnitStatus(2, new CatalogStatusUpdateRequest(false));

        assertThat(updated.code()).isEqualTo("hộp");
        assertThat(updated.baseFactor()).isEqualByComparingTo("1");
        assertThat(updated.dimension()).isEqualTo(MeasurementDimension.COUNT);
        assertThat(deactivated.active()).isFalse();
    }

    @Test void keepsStandardRecipeUnitsFixedAndListsOnlySupportedRecipeUnits() {
        UnitEntity gram = new UnitEntity("g", "gram", MeasurementDimension.MASS, BigDecimal.ONE);
        org.springframework.test.util.ReflectionTestUtils.setField(gram, "id", 1);
        UnitEntity piece = new UnitEntity("quả", "quả", MeasurementDimension.COUNT, BigDecimal.ONE);
        org.springframework.test.util.ReflectionTestUtils.setField(piece, "id", 2);
        UnitEntity unsupportedLegacy = new UnitEntity("bìa", "bìa", MeasurementDimension.COUNT, BigDecimal.ONE);
        org.springframework.test.util.ReflectionTestUtils.setField(unsupportedLegacy, "id", 3);
        IngredientEntity ingredient = new IngredientEntity("Cam", "Trái cây", "Manual", null, LocalDate.now());
        org.springframework.test.util.ReflectionTestUtils.setField(ingredient, "id", 7L);
        var countRate = new IngredientUnitConversionEntity(new IngredientUnitConversionId(7L, 2),
                new BigDecimal("130"), true);
        when(unitRepository.findById(1)).thenReturn(Optional.of(gram));
        when(ingredientRepository.findByIdAndStatus(7L, CatalogStatus.ACTIVE)).thenReturn(Optional.of(ingredient));
        when(conversionRepository.findByIdIngredientId(7L)).thenReturn(List.of(countRate));
        when(unitRepository.findByActiveTrue()).thenReturn(List.of(gram, piece, unsupportedLegacy));

        assertThatThrownBy(() -> service.updateUnit(1, new UnitSaveRequest("g", "gram", MeasurementDimension.MASS,
                new BigDecimal("2"))))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getCode()).isEqualTo("STANDARD_UNIT_FIXED"));
        assertThatThrownBy(() -> service.setUnitStatus(1, new CatalogStatusUpdateRequest(false)))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getCode()).isEqualTo("STANDARD_UNIT_REQUIRED"));
        assertThat(service.listRecipeUnits(7L)).extracting(option -> option.code()).containsExactly("g", "quả");
        assertThat(service.listRecipeUnits(7L).get(1).gramsPerUnit()).isEqualByComparingTo("130");
    }

    @Test void savesAndReactivatesSynchronizedVolumeConversionsAndRejectsIncompleteStandardSet() {
        IngredientEntity ingredient = new IngredientEntity("Nước", "Khác", "Manual", null, LocalDate.now());
        when(ingredientRepository.findById(7L)).thenReturn(Optional.of(ingredient));
        List<UnitEntity> volumeUnits = List.of(
                new UnitEntity("ml", "ml", MeasurementDimension.VOLUME, BigDecimal.ONE),
                new UnitEntity("L", "lít", MeasurementDimension.VOLUME, new BigDecimal("1000")),
                new UnitEntity("tsp", "tsp", MeasurementDimension.VOLUME, new BigDecimal("5")),
                new UnitEntity("tbsp", "tbsp", MeasurementDimension.VOLUME, new BigDecimal("15")),
                new UnitEntity("cup", "cup", MeasurementDimension.VOLUME, new BigDecimal("240")));
        for (int index = 0; index < volumeUnits.size(); index++) {
            org.springframework.test.util.ReflectionTestUtils.setField(volumeUnits.get(index), "id", index + 1);
        }
        when(unitRepository.findByActiveTrue()).thenReturn(volumeUnits);
        when(conversionRepository.findById(any())).thenReturn(Optional.empty());
        when(conversionRepository.findByIdIngredientId(7L)).thenReturn(List.of());
        var saved = service.saveVolumeConversions(7L, new VolumeConversionSaveRequest(new BigDecimal("0.9"), true));
        assertThat(saved).hasSize(5);
        assertThat(saved).extracting(tech.mamxanh.nutrition.dto.response.ConversionResponse::gramsPerUnit)
                .containsExactly(new BigDecimal("0.9"), new BigDecimal("900.0"), new BigDecimal("4.5"),
                        new BigDecimal("13.5"), new BigDecimal("216.0"));
        org.mockito.Mockito.verify(conversionRepository, org.mockito.Mockito.times(5)).insertNew(
                eq(7L), org.mockito.ArgumentMatchers.anyInt(), any(BigDecimal.class), eq(true));
        assertThat(service.setVolumeConversionStatus(7L, new CatalogStatusUpdateRequest(false))).isEmpty();
    }

    @Test void updatesEveryExistingVolumeConversionAndCanRestoreTheWholeGroup() {
        IngredientEntity ingredient = new IngredientEntity("Nước", "Khác", "Manual", null, LocalDate.now());
        when(ingredientRepository.findById(7L)).thenReturn(Optional.of(ingredient));
        List<UnitEntity> units = List.of(
                new UnitEntity("ml", "ml", MeasurementDimension.VOLUME, BigDecimal.ONE),
                new UnitEntity("L", "lít", MeasurementDimension.VOLUME, new BigDecimal("1000")),
                new UnitEntity("tsp", "tsp", MeasurementDimension.VOLUME, new BigDecimal("5")),
                new UnitEntity("tbsp", "tbsp", MeasurementDimension.VOLUME, new BigDecimal("15")),
                new UnitEntity("cup", "cup", MeasurementDimension.VOLUME, new BigDecimal("240")));
        List<IngredientUnitConversionEntity> conversions = new java.util.ArrayList<>();
        for (int index = 0; index < units.size(); index++) {
            int unitId = index + 1;
            org.springframework.test.util.ReflectionTestUtils.setField(units.get(index), "id", unitId);
            conversions.add(new IngredientUnitConversionEntity(new IngredientUnitConversionId(7L, unitId),
                    new BigDecimal("1"), false));
        }
        when(unitRepository.findByActiveTrue()).thenReturn(units);
        when(conversionRepository.findById(any())).thenAnswer(invocation -> conversions.stream()
                .filter(conversion -> conversion.getId().equals(invocation.getArgument(0))).findFirst());
        when(conversionRepository.findByIdIngredientId(7L)).thenReturn(conversions);

        var saved = service.saveVolumeConversions(7L, new VolumeConversionSaveRequest(new BigDecimal("0.92"), false));
        var restored = service.setVolumeConversionStatus(7L, new CatalogStatusUpdateRequest(true));

        assertThat(saved).hasSize(5).allSatisfy(conversion -> assertThat(conversion.active()).isTrue());
        assertThat(saved.get(4).gramsPerUnit()).isEqualByComparingTo("220.80");
        assertThat(restored).hasSize(5).allSatisfy(conversion -> assertThat(conversion.active()).isTrue());
        assertThat(conversions).allSatisfy(conversion -> assertThat(conversion.isActive()).isTrue());
    }

    @Test void rejectsSavingVolumeConversionsWhenAStandardUnitIsMissing() {
        when(ingredientRepository.findById(7L)).thenReturn(Optional.of(
                new IngredientEntity("Nước", "Khác", "Manual", null, LocalDate.now())));
        when(unitRepository.findByActiveTrue()).thenReturn(List.of(
                new UnitEntity("ml", "ml", MeasurementDimension.VOLUME, BigDecimal.ONE)));

        assertThatThrownBy(() -> service.saveVolumeConversions(7L,
                new VolumeConversionSaveRequest(new BigDecimal("0.9"), false)))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo("STANDARD_VOLUME_UNITS_INCOMPLETE"));
    }

    @Test void rejectsChangingUnitDimensionWhenConversionsExist() {
        UnitEntity unit = new UnitEntity("oz", "Ounce", MeasurementDimension.VOLUME, BigDecimal.ONE);
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
        UnitEntity unit = new UnitEntity("custom", "đơn vị tùy chỉnh", MeasurementDimension.MASS, BigDecimal.ONE);
        when(unitRepository.findById(2)).thenReturn(Optional.of(unit));

        var updated = service.updateUnit(2, new UnitSaveRequest("custom", "đơn vị mới", MeasurementDimension.MASS, BigDecimal.ONE));
        assertThat(updated.name()).isEqualTo("đơn vị mới");

        when(unitRepository.existsByCodeIgnoreCase("other")).thenReturn(true);
        assertThatThrownBy(() -> service.updateUnit(2,
                new UnitSaveRequest("other", "đơn vị khác", MeasurementDimension.MASS, BigDecimal.ONE)))
                .isInstanceOf(ApiException.class).hasMessage("Ký hiệu đơn vị đã tồn tại.");
    }

    @Test void updatesIngredientMetadataAndRejectsRenamingToAnExistingName() {
        IngredientEntity ingredient = new IngredientEntity("Đậu phụ", "Đậu hạt", "USDA", null, LocalDate.now());
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        var request = new IngredientSaveRequest(" Đậu hũ ", " Đậu hạt ", " USDA FoodData Central ", "https://example.com/source", LocalDate.of(2026, 9, 1));

        var updated = service.updateIngredient(1L, request);

        assertThat(updated.name()).isEqualTo("Đậu hũ");
        assertThat(updated.sourceUrl()).isEqualTo("https://example.com/source");
        when(ingredientRepository.existsByNameIgnoreCaseAndStatusAndIdNot(
                "Chuối", CatalogStatus.ACTIVE, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.updateIngredient(1L,
                new IngredientSaveRequest("Chuối", "Trái cây", "USDA FoodData Central", null, LocalDate.now())))
                .isInstanceOf(ApiException.class).hasMessage("Tên nguyên liệu đã tồn tại trong danh mục đang hoạt động.");
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

    @Test void rejectsReactivatingOrDeactivatingMassUnitConversion() {
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
        assertThatThrownBy(() -> service.setConversionStatus(1L, 2, new CatalogStatusUpdateRequest(false)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("MASS_CONVERSION_NOT_ALLOWED");
                });
        assertThat(conversion.isActive()).isTrue();
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
                .hasMessage("Không hỗ trợ xóa vĩnh viễn nguyên liệu. Hãy chuyển sang trạng thái ngừng sử dụng.");

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
