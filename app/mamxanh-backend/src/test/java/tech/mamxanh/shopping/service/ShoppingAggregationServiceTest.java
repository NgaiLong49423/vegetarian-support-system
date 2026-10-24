package tech.mamxanh.shopping.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.nutrition.dto.response.*;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.service.*;
import tech.mamxanh.shopping.dto.request.ShoppingAggregationLine;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item;

@ExtendWith(MockitoExtension.class)
class ShoppingAggregationServiceTest {
    @Mock IngredientCatalogLookupService catalog;
    @Mock IngredientConversionLookupService conversions;
    ShoppingAggregationService service;
    List<ConversionResponse> rates;
    final List<UnitResponse> units = List.of(unit(1,"g",MeasurementDimension.MASS),unit(2,"kg",MeasurementDimension.MASS),
            unit(3,"ml",MeasurementDimension.VOLUME),unit(4,"L",MeasurementDimension.VOLUME),
            unit(5,"quả",MeasurementDimension.COUNT),unit(6,"bìa",MeasurementDimension.COUNT),
            unit(7,"tbsp",MeasurementDimension.VOLUME));
    final List<IngredientResponse> ingredients = List.of(ingredient(1,"Đậu phụ","Đậu"),ingredient(2,"Nước tương","Gia vị"),
            ingredient(3,"Bơ","Rau củ"),ingredient(4,"Đậu phụ","Đậu"));

    @BeforeEach void setup() {
        service = new ShoppingAggregationService(catalog, conversions); rates = new ArrayList<>();
        lenient().when(catalog.findIngredientReferences(anyCollection())).thenAnswer(call -> {
            Collection<Long> ids = call.getArgument(0); return ingredients.stream().filter(i -> ids.contains(i.id())).toList();
        });
        lenient().when(catalog.findUnitReferences(anyCollection())).thenAnswer(call -> {
            Collection<Integer> ids = call.getArgument(0); return units.stream().filter(u -> ids.contains(u.id())).toList();
        });
        lenient().when(catalog.findActiveUnits()).thenReturn(units);
        lenient().when(conversions.findActiveConversions(anyCollection())).thenAnswer(call -> List.copyOf(rates));
    }

    @Test void ac541CombinesGramsAndKilograms() {
        var output = items(service.aggregate(List.of(line(1,1,"600"),line(1,2,"0.8"))));
        assertThat(output).singleElement().satisfies(i -> {
            assertThat(i.quantity()).isEqualByComparingTo("1400"); assertThat(i.unitCode()).isEqualTo("g");
            assertThat(i.incompatibleUnits()).isFalse();
        });
    }

    @Test void ac542CombinesMillilitersAndLiters() {
        assertThat(items(service.aggregate(List.of(line(2,3,"500"),line(2,4,"0.75")))))
                .singleElement().satisfies(i -> { assertThat(i.quantity()).isEqualByComparingTo("1250"); assertThat(i.unitCode()).isEqualTo("ml"); });
    }

    @Test void ac543NeverAssumesMassVolumeDensity() {
        var output = items(service.aggregate(List.of(line(2,1,"300"),line(2,3,"200"))));
        assertThat(output).hasSize(2).allSatisfy(i -> assertThat(i.incompatibleUnits()).isTrue());
        assertThat(output).extracting(Item::unitCode).containsExactlyInAnyOrder("g","ml");
        assertThat(output).extracting(Item::quantity).containsExactlyInAnyOrder(new BigDecimal("300"),new BigDecimal("200"));
    }

    @Test void ac544UsesOnlyTheCorrectIngredientsOfficialRule() {
        rates.add(rate(1,6,"150",true));
        assertThat(items(service.aggregate(List.of(line(1,6,"2"),line(1,1,"300")))))
                .singleElement().satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("600"));
    }

    @Test void ac545KeepsFruitCountsAndMassSeparateWithoutRule() {
        assertThat(items(service.aggregate(List.of(line(3,5,"2"),line(3,1,"300")))))
                .hasSize(2).allSatisfy(i -> assertThat(i.incompatibleUnits()).isTrue());
    }

    @Test void keepsSameCountUnitEvenWhenItsGramsRuleExists() {
        rates.add(rate(3,5,"120",true));
        assertThat(items(service.aggregate(List.of(line(3,5,"2"),line(3,5,"3")))))
                .singleElement().satisfies(i -> {assertThat(i.quantity()).isEqualByComparingTo("5"); assertThat(i.unitCode()).isEqualTo("quả");});
    }

    @Test void countUnitsDoNotMergeWithoutOfficialBridge() {
        assertThat(items(service.aggregate(List.of(line(1,5,"2"),line(1,6,"3"))))).hasSize(2);
    }

    @Test void neverBorrowsAnotherIngredientsRateOrAnInactiveRate() {
        rates.add(rate(4,6,"150",true)); rates.add(rate(1,6,"150",false));
        assertThat(items(service.aggregate(List.of(line(1,6,"2"),line(1,1,"300"))))).hasSize(2);
    }

    @Test void invalidConversionRatesNeverBecomeRatios() {
        rates.add(rate(1,6,"0",true));
        assertThat(items(service.aggregate(List.of(line(1,6,"2"),line(1,1,"300"))))).hasSize(2);
    }

    @Test void officialVolumeDensitySupportsCrossDimensionMerge() {
        rates.add(rate(2,3,"1.25",true));
        assertThat(items(service.aggregate(List.of(line(2,1,"300"),line(2,3,"200")))))
                .singleElement().satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("550"));
    }

    @Test void approvedMlLiterRelationUsesTheOfficialRateForEitherStandardUnit() {
        rates.add(rate(2,3,"1.25",true));
        assertThat(items(service.aggregate(List.of(line(2,1,"100"),line(2,4,"1")))))
                .singleElement().satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("1350"));
    }

    @Test void partialRulesMergeOnlyCompatibleBuckets() {
        rates.add(rate(1,6,"150",true));
        var output = items(service.aggregate(List.of(line(1,6,"2"),line(1,1,"300"),line(1,5,"1"))));
        assertThat(output).hasSize(2).allSatisfy(i -> assertThat(i.incompatibleUnits()).isTrue());
        assertThat(output).filteredOn(i -> i.unitCode().equals("g")).singleElement()
                .satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("600"));
    }

    @Test void sameDisplayNameDoesNotMergeDifferentIngredientIds() {
        assertThat(items(service.aggregate(List.of(line(1,1,"100"),line(4,1,"200")))))
                .hasSize(2).extracting(Item::ingredientId).containsExactlyInAnyOrder(1L,4L);
    }

    @Test void manualNamesIgnoreCaseButRequireTheSameUnitAndStaySeparateFromCatalog() {
        var rows = List.of(manual("  KHĂN GIẤY  ",1,"1"),manual("khăn giấy",1,"2"),manual("Khăn  giấy",1,"3"),manual("Khăn giấy",2,"1"),
                manual("Đậu phụ",1,"50"),line(1,1,"100"));
        var output = items(service.aggregate(rows));
        assertThat(output).hasSize(4);
        assertThat(output).filteredOn(i -> i.ingredientId()==null && i.unitId()==1 && i.name().contains("KHĂN"))
                .singleElement().satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("6"));
    }

    @Test void manualNamesRejectBlankOverlongAndControlCharacterValues() {
        var result = service.aggregate(List.of(manual("   ",1,"1"), manual("x".repeat(201),1,"1"),
                manual("Tên\u0001không hợp lệ",1,"1"), new ShoppingAggregationLine(null,null,BigDecimal.ONE,1)));
        assertThat(items(result)).isEmpty();
        assertThat(result.warnings()).extracting(ShoppingAggregationResult.Warning::code)
                .containsExactly("INVALID_MANUAL_NAME","INVALID_MANUAL_NAME","INVALID_MANUAL_NAME","INVALID_MANUAL_NAME");
    }

    @Test void equivalentVolumeRatesMergeAndInvalidOfficialRatesAreIgnored() {
        rates.add(new ConversionResponse(2L,3,new BigDecimal("9000"),false,true));
        rates.add(new ConversionResponse(2L,3,null,false,true));
        rates.add(rate(2,3,"0",true));
        rates.add(rate(2,3,"1.25",true));
        rates.add(rate(2,4,"1250",true));
        var result = service.aggregate(List.of(line(2,1,"300"),line(2,3,"200"),line(2,4,"0.2")));
        assertThat(items(result)).singleElement().satisfies(item -> {
            assertThat(item.quantity()).isEqualByComparingTo("800");
            assertThat(item.unitCode()).isEqualTo("g");
            assertThat(item.incompatibleUnits()).isFalse();
        });
        assertThat(result.warnings()).isEmpty();
    }

    @Test void keepsConvertibleBucketsSeparateWhenNoMassTargetUnitExists() {
        when(catalog.findActiveUnits()).thenReturn(List.of());
        rates.add(rate(2,3,"1.25",true));
        rates.add(rate(2,7,"15",true));
        var output = items(service.aggregate(List.of(line(2,3,"100"),line(2,7,"2"))));
        assertThat(output).hasSize(2).allSatisfy(item -> assertThat(item.incompatibleUnits()).isTrue());
        assertThat(output).extracting(Item::unitCode).containsExactlyInAnyOrder("ml","tbsp");
    }

    @Test void rejectsInvalidRowsWithSourceIndexedWarningsAndKeepsValidRows() {
        var result = service.aggregate(Arrays.asList(null,line(1,1,"0"),line(1,1,"-2"),
                new ShoppingAggregationLine(1L,null,null,1),line(1,999,"2"),line(999,1,"2"),manual("<script>",1,"2"),line(1,1,"3")));
        assertThat(items(result)).singleElement().satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("3"));
        assertThat(result.warnings()).hasSize(7).extracting(ShoppingAggregationResult.Warning::sourceIndex).containsExactly(0,1,2,3,4,5,6);
    }

    @Test void decimalArithmeticNeverRoundsSmallPositiveQuantitiesToZero() {
        assertThat(items(service.aggregate(List.of(line(1,1,"0.0001"),line(1,1,"0.0002")))))
                .singleElement().satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("0.0003"));
        assertThat(items(service.aggregate(List.of(line(1,1,"0.1"),line(1,1,"0.2")))))
                .singleElement().satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("0.3"));
    }

    @Test void tablespoonsNeverAutomaticallyConvertThroughTheCatalogBaseFactor() {
        assertThat(items(service.aggregate(List.of(line(2,7,"1"),line(2,3,"15"))))).hasSize(2);
    }

    @Test void conflictingVolumeRulesStaySeparateFromMassAndProduceWarning() {
        rates.add(rate(2,3,"1",true)); rates.add(rate(2,4,"2000",true));
        var result=service.aggregate(List.of(line(2,1,"100"),line(2,3,"100"),line(2,4,"1")));
        assertThat(items(result)).hasSize(2);
        assertThat(result.warnings()).extracting(ShoppingAggregationResult.Warning::code).contains("CONFLICTING_CONVERSION");
    }

    @Test void groupsByCatalogCategoryAndResultsAreImmutable() {
        var result = service.aggregate(List.of(line(3,1,"10"),line(1,1,"20"),line(2,3,"30")));
        assertThat(result.groups()).extracting(ShoppingAggregationResult.Group::name).containsExactly("Gia vị","Rau củ","Đậu");
        assertThatThrownBy(() -> result.groups().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.groups().getFirst().items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void sumsDoNotDependOnInputOrder() {
        var rows = new ArrayList<>(List.of(line(1,2,"1.2"),line(1,1,"300"),line(1,1,"0.0001")));
        var first = service.aggregate(rows); Collections.reverse(rows);
        assertThat(service.aggregate(rows)).isEqualTo(first);
    }

    @Test void emptyInputDoesNotQueryCatalogAndNullInputIsRejected() {
        assertThat(service.aggregate(List.of()).groups()).isEmpty();
        assertThatThrownBy(() -> service.aggregate(null)).isInstanceOf(AppException.class);
        verifyNoInteractions(catalog, conversions);
    }

    @Test void resolvesCatalogAndRatesInBatchesInsteadOfPerRow() {
        service.aggregate(List.of(line(1,1,"1"),line(1,1,"2"),line(1,2,"3")));
        verify(catalog).findIngredientReferences(List.of(1L));
        verify(conversions).findActiveConversions(List.of(1L));
        verify(conversions,never()).findActiveGramsPerUnit(anyLong(),anyInt());
    }

    @Test void fallsBackToAvailableKilogramsWhenGramTargetIsAbsent() {
        when(catalog.findActiveUnits()).thenReturn(List.of(units.get(1)));
        assertThat(items(service.aggregate(List.of(line(1,2,"1"),line(1,2,"2")))))
                .singleElement().satisfies(i -> {assertThat(i.unitCode()).isEqualTo("kg");assertThat(i.quantity()).isEqualByComparingTo("3");});
    }

    private ShoppingAggregationLine line(long id,int unit,String quantity) {return new ShoppingAggregationLine(id,null,new BigDecimal(quantity),unit);}
    private ShoppingAggregationLine manual(String name,int unit,String quantity) {return new ShoppingAggregationLine(null,name,new BigDecimal(quantity),unit);}
    private ConversionResponse rate(long id,int unit,String quantity,boolean active) {return new ConversionResponse(id,unit,new BigDecimal(quantity),false,active);}
    private List<Item> items(ShoppingAggregationResult result) {return result.groups().stream().flatMap(g -> g.items().stream()).toList();}
    private static IngredientResponse ingredient(long id,String name,String group) {return new IngredientResponse(id,name,group,null,null,null,false,true);}
    private static UnitResponse unit(int id,String code,MeasurementDimension dimension) {return new UnitResponse(id,code,code,dimension,new BigDecimal(code.equals("kg")||code.equals("L")?"1000":"1"),true);}
}
