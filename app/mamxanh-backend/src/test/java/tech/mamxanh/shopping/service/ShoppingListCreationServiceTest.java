package tech.mamxanh.shopping.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.service.MealPlanReadService;
import tech.mamxanh.recipe.service.RecipeService;
import tech.mamxanh.recipe.service.RecipeService.ShoppingIngredient;
import tech.mamxanh.recipe.service.RecipeService.ShoppingSource;
import tech.mamxanh.shopping.dto.request.CreateMealPlanShoppingListRequest;
import tech.mamxanh.shopping.dto.request.ShoppingAggregationLine;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.entity.ShoppingListItemEntity;
import tech.mamxanh.shopping.repository.ShoppingListItemRepository;
import tech.mamxanh.shopping.repository.ShoppingListRepository;

@ExtendWith(MockitoExtension.class)
class ShoppingListCreationServiceTest {
    @Mock CurrentUserService users;
    @Mock MealPlanReadService plans;
    @Mock RecipeService recipes;
    @Mock ShoppingAggregationService aggregation;
    @Mock ShoppingListRepository lists;
    @Mock ShoppingListItemRepository items;
    @Mock ShoppingListService checklists;
    @Mock EntityManager entityManager;
    final LocalDate monday = LocalDate.of(2026, 10, 19);
    ShoppingListCreationService service;

    @BeforeEach void setup() {
        service = new ShoppingListCreationService(users, plans, recipes, aggregation, lists, items, checklists, entityManager);
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(17, Role.CUSTOMER));
    }

    @Test void derivesOwnerScalesEveryEntryAndKeepsCreationDiagnostics() {
        week(List.of(entry(1, monday), entry(2, monday.plusDays(1))));
        when(recipes.findShoppingSources(List.of(8L))).thenReturn(Map.of(8L, recipe(new BigDecimal("300"))));
        when(aggregation.aggregate(anyList())).thenReturn(output("450", true,
                List.of(new ShoppingAggregationResult.Warning(1, "SOURCE_WARNING", "Cảnh báo nguồn"))));
        persist();
        var response = service.createFromMealPlan(new CreateMealPlanShoppingListRequest(monday, null));
        @SuppressWarnings("unchecked") ArgumentCaptor<List<ShoppingAggregationLine>> lines = ArgumentCaptor.forClass(List.class);
        verify(aggregation).aggregate(lines.capture());
        assertThat(lines.getValue()).extracting(ShoppingAggregationLine::quantity).containsExactly(new BigDecimal("150"), new BigDecimal("150"));
        assertThat(lines.getValue()).allSatisfy(line -> assertThat(line.ingredientId()).isEqualTo(4));
        var list = org.mockito.ArgumentCaptor.forClass(tech.mamxanh.shopping.entity.ShoppingListEntity.class);
        verify(lists).saveAndFlush(list.capture());
        assertThat(list.getValue().getUserId()).isEqualTo(17);
        assertThat(response.incompatibleItemIds()).containsExactly(30L);
        assertThat(response.warnings()).singleElement().satisfies(w -> {
            assertThat(w.mealPlanEntryId()).isEqualTo(2); assertThat(w.recipeId()).isEqualTo(8);
        });
        verify(entityManager).refresh(list.getValue());
    }

    @Test void selectsOnlyRequestedDaysAndReportsUnavailableRecipes() {
        week(List.of(entry(1,monday),entry(2,monday.plusDays(1))));
        when(recipes.findShoppingSources(List.of(8L))).thenReturn(Map.of());
        when(aggregation.aggregate(List.of())).thenReturn(new ShoppingAggregationResult(List.of(),List.of()));
        assertThatThrownBy(() -> service.createFromMealPlan(new CreateMealPlanShoppingListRequest(monday,List.of(monday.plusDays(1)))))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(lists, items, entityManager);
    }

    @Test void neverTreatsMissingCatalogIdInRecipeAsAManualItem() {
        week(List.of(entry(1,monday)));
        var source = new ShoppingSource(8,2,List.of(new ShoppingIngredient(null,BigDecimal.ONE,1),
                new ShoppingIngredient(4L,null,1),new ShoppingIngredient(4L,BigDecimal.ONE,1)));
        when(recipes.findShoppingSources(anyList())).thenReturn(Map.of(8L,source));
        when(aggregation.aggregate(anyList())).thenReturn(output("1",false,List.of()));
        persist();
        var response = service.createFromMealPlan(new CreateMealPlanShoppingListRequest(monday,null));
        assertThat(response.warnings()).singleElement().satisfies(w -> assertThat(w.code()).isEqualTo("INVALID_RECIPE_INGREDIENT"));
        @SuppressWarnings("unchecked") ArgumentCaptor<List<ShoppingAggregationLine>> lines=ArgumentCaptor.forClass(List.class);
        verify(aggregation).aggregate(lines.capture());
        assertThat(lines.getValue()).hasSize(2).allSatisfy(line -> assertThat(line.ingredientId()).isEqualTo(4));
        assertThat(lines.getValue().getFirst().quantity()).isNull();
    }

    @Test void roundsOnlyFinalTotalsAndWarnsInsteadOfSilentlyChangingQuantity() {
        prepareOutput("0.6733333333333333333333333333");persist();
        var response=service.createFromMealPlan(new CreateMealPlanShoppingListRequest(monday,null));
        assertThat(response.warnings()).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo("QUANTITY_ROUNDED");assertThat(w.recipeId()).isNull();
        });
        @SuppressWarnings("unchecked") ArgumentCaptor<List<ShoppingListItemEntity>> rows=ArgumentCaptor.forClass(List.class);
        verify(items).saveAllAndFlush(rows.capture());
        assertThat(rows.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getQuantity()).isEqualByComparingTo("0.67");assertThat(row.isBought()).isFalse();
            assertThat(row.getShoppingListId()).isEqualTo(20);
        });
    }

    @Test void rejectsUnderflowAndOverflowBeforeWritingAnyList() {
        prepareOutput("0.00001");
        assertThatThrownBy(() -> service.createFromMealPlan(new CreateMealPlanShoppingListRequest(monday,null))).isInstanceOf(AppException.class);
        when(aggregation.aggregate(anyList())).thenReturn(output("100000000",false,List.of()));
        assertThatThrownBy(() -> service.createFromMealPlan(new CreateMealPlanShoppingListRequest(monday,null))).isInstanceOf(AppException.class);
        verifyNoInteractions(lists,items,entityManager);
    }

    @Test void emptyOwnWeekCannotCreateAnEmptyList() {
        week(List.of());
        assertThatThrownBy(() -> service.createFromMealPlan(new CreateMealPlanShoppingListRequest(monday,null))).isInstanceOf(AppException.class);
        verifyNoInteractions(recipes,aggregation,lists,items);
    }

    @Test void validatesWeekAndSelectionWithoutReadingAnotherModuleOrWriting() {
        var invalid = new ArrayList<CreateMealPlanShoppingListRequest>();
        invalid.add(null);invalid.add(new CreateMealPlanShoppingListRequest(null,null));
        invalid.add(new CreateMealPlanShoppingListRequest(monday.plusDays(1),null));
        invalid.add(new CreateMealPlanShoppingListRequest(LocalDate.of(0,1,3),null));
        invalid.add(new CreateMealPlanShoppingListRequest(LocalDate.of(9999,12,27),null));
        invalid.add(new CreateMealPlanShoppingListRequest(monday,List.of()));
        invalid.add(new CreateMealPlanShoppingListRequest(monday,List.of(monday,monday)));
        invalid.add(new CreateMealPlanShoppingListRequest(monday,Arrays.asList((LocalDate)null)));
        invalid.add(new CreateMealPlanShoppingListRequest(monday,List.of(monday.minusDays(1))));
        invalid.add(new CreateMealPlanShoppingListRequest(monday,List.of(monday.plusDays(7))));
        invalid.add(new CreateMealPlanShoppingListRequest(monday,java.util.Collections.nCopies(8,monday)));
        for (var request:invalid) assertThatThrownBy(() -> service.createFromMealPlan(request)).isInstanceOf(AppException.class);
        verifyNoInteractions(plans,recipes,aggregation,lists,items);
    }

    @Test void authorizationHappensBeforeDateValidationAndAnyDataAccess() {
        when(users.requireActiveMember()).thenThrow(new AppException(ErrorCode.ACCESS_DENIED));
        assertThatThrownBy(() -> service.createFromMealPlan(null)).isInstanceOf(AppException.class);
        verifyNoInteractions(plans,recipes,aggregation,lists,items);
    }

    @ParameterizedTest
    @MethodSource("mealDateNames")
    void persistsTheRequestedMealDatesInTheListName(LocalDate start, List<LocalDate> selected, String expected) {
        var dates = selected == null ? List.of(start) : selected;
        when(plans.getWeek(start)).thenReturn(new MealPlanWeekResponse(start,
                dates.stream().map(date -> entry(1, date)).toList()));
        when(recipes.findShoppingSources(List.of(8L))).thenReturn(Map.of(8L, recipe(BigDecimal.ONE)));
        when(aggregation.aggregate(anyList())).thenReturn(output("1", false, List.of()));
        persist();
        service.createFromMealPlan(new CreateMealPlanShoppingListRequest(start, selected));
        var saved = ArgumentCaptor.forClass(tech.mamxanh.shopping.entity.ShoppingListEntity.class);
        verify(lists).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo(expected);
    }

    static Stream<Arguments> mealDateNames() {
        LocalDate october = LocalDate.of(2026, 10, 19);
        LocalDate monthEnd = LocalDate.of(2026, 10, 26);
        LocalDate yearEnd = LocalDate.of(2026, 12, 28);
        return Stream.of(
                Arguments.of(october, null, "Đi chợ · thực đơn tuần 19–25/10/2026"),
                Arguments.of(october, List.of(october), "Đi chợ · thực đơn 19/10/2026"),
                Arguments.of(october, List.of(october.plusDays(2)), "Đi chợ · thực đơn 21/10/2026"),
                Arguments.of(october, List.of(october.plusDays(2), october, october.plusDays(1)), "Đi chợ · thực đơn 19–21/10/2026"),
                Arguments.of(october, List.of(october.plusDays(4), october, october.plusDays(2)), "Đi chợ · thực đơn 19, 21, 23/10/2026"),
                Arguments.of(october, Stream.iterate(october, date -> date.plusDays(1)).limit(7).toList(), "Đi chợ · thực đơn 19–25/10/2026"),
                Arguments.of(monthEnd, null, "Đi chợ · thực đơn tuần 26/10–01/11/2026"),
                Arguments.of(monthEnd, List.of(monthEnd.plusDays(6), monthEnd.plusDays(5)), "Đi chợ · thực đơn 31/10–01/11/2026"),
                Arguments.of(monthEnd, List.of(monthEnd.plusDays(6), monthEnd), "Đi chợ · thực đơn 26/10/2026, 01/11/2026"),
                Arguments.of(yearEnd, null, "Đi chợ · thực đơn tuần 28/12/2026–03/01/2027"),
                Arguments.of(yearEnd, List.of(yearEnd.plusDays(4), yearEnd.plusDays(3)), "Đi chợ · thực đơn 31/12/2026–01/01/2027"),
                Arguments.of(yearEnd, List.of(yearEnd.plusDays(6), yearEnd), "Đi chợ · thực đơn 28/12/2026, 03/01/2027"));
    }

    void week(List<MealPlanWeekResponse.Entry> entries) {when(plans.getWeek(monday)).thenReturn(new MealPlanWeekResponse(monday,entries));}
    MealPlanWeekResponse.Entry entry(long id,LocalDate date) {return new MealPlanWeekResponse.Entry(id,date,"LUNCH",BigDecimal.ONE,8,"Món",null,"SOUP",20,false,null);}
    ShoppingSource recipe(BigDecimal quantity) {return new ShoppingSource(8,2,List.of(new ShoppingIngredient(4L,quantity,1)));}
    ShoppingAggregationResult output(String quantity,boolean incompatible,List<ShoppingAggregationResult.Warning> warnings) {
        return new ShoppingAggregationResult(List.of(new ShoppingAggregationResult.Group("Đậu",List.of(
                new ShoppingAggregationResult.Item(4L,"Đậu",new BigDecimal(quantity),1,"g","gam",incompatible)))),warnings);
    }
    void prepareOutput(String quantity) {
        week(List.of(entry(1,monday)));when(recipes.findShoppingSources(anyList())).thenReturn(Map.of(8L,recipe(BigDecimal.ONE)));
        when(aggregation.aggregate(anyList())).thenReturn(output(quantity,false,List.of()));
    }
    void persist() {
        when(lists.saveAndFlush(any())).thenAnswer(invocation -> {
            var list=invocation.getArgument(0,tech.mamxanh.shopping.entity.ShoppingListEntity.class);list.setId(20L);return list;
        });
        when(items.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<ShoppingListItemEntity> rows=invocation.getArgument(0);for(int i=0;i<rows.size();i++) rows.get(i).setId(30L+i);return rows;
        });
        when(checklists.get(20)).thenReturn(new ShoppingListResponse(20,"Đi chợ",null,null,List.of()));
    }
}
