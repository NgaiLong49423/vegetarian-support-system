package tech.mamxanh.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.recipe.service.RecipeService;
import tech.mamxanh.shopping.dto.request.AddRecipeToShoppingListRequest;
import tech.mamxanh.shopping.dto.request.CreateShoppingItemRequest;
import tech.mamxanh.shopping.dto.request.ShoppingAggregationLine;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.entity.ShoppingListEntity;
import tech.mamxanh.shopping.entity.ShoppingListItemEntity;
import tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity;
import tech.mamxanh.shopping.repository.ShoppingListItemRepository;
import tech.mamxanh.shopping.repository.ShoppingListRepository;
import tech.mamxanh.shopping.repository.ShoppingRecipeContributionRepository;

@ExtendWith(MockitoExtension.class)
class RecipeShoppingListServiceTest {
    @Mock CurrentUserService users;
    @Mock RecipeService recipes;
    @Mock ShoppingAggregationService aggregation;
    @Mock ShoppingListRepository lists;
    @Mock ShoppingListItemRepository items;
    @Mock ShoppingRecipeContributionRepository contributions;
    @Mock ShoppingListService checklists;
    @Mock EntityManager entityManager;

    RecipeShoppingListService service;
    ShoppingListEntity list;

    @BeforeEach
    void setUp() {
        service = new RecipeShoppingListService(users, recipes, aggregation, lists, items, contributions,
                checklists, entityManager, Clock.fixed(Instant.parse("2026-10-24T05:00:00Z"), ZoneOffset.UTC));
        list = new ShoppingListEntity();
        list.setId(7L);
        list.setUserId(17L);
        when(users.lockActiveMember()).thenReturn(new CurrentUserService.CurrentUser(17, Role.CUSTOMER));
    }

    @Test
    void rejectsNullOrInvalidRecipeRequestBeforeLookingUpAList() {
        assertThatThrownBy(() -> service.addRecipe(7, null)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.addRecipe(7, request(0L, null, null, null))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, 51, null, null))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.createFromRecipe(null)).isInstanceOf(AppException.class);
        verifyNoInteractions(lists, recipes);
    }

    @Test
    void rejectsInvalidOrUnownedListWithoutLookingUpRecipeSources() {
        assertThatThrownBy(() -> service.addRecipe(0, request(11L, null, null, null))).isInstanceOf(AppException.class);
        when(lists.lockOwned(7L, 17L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, null, null, null))).isInstanceOf(AppException.class);
        verifyNoInteractions(recipes);
    }

    @Test
    void rejectsMissingRecipeAndInvalidPublishedServings() {
        ownedList();
        when(recipes.findShoppingSources(List.of(11L))).thenReturn(Map.of());
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, null, null, null))).isInstanceOf(AppException.class);
        when(recipes.findShoppingSources(List.of(12L))).thenReturn(Map.of(12L,
                new RecipeService.ShoppingSource(12, 0, List.of())));
        assertThatThrownBy(() -> service.addRecipe(7, request(12L, null, null, null))).isInstanceOf(AppException.class);
    }

    @Test
    void rejectsRepeatedUnknownAndEmptyIngredientSelections() {
        ownedList();
        source(11, List.of(new RecipeService.ShoppingIngredient(3L, BigDecimal.TEN, 1)));
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, null, List.of(3L, 3L), null)))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, null, List.of(999L), null)))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, null, List.of(), null)))
                .isInstanceOf(AppException.class);
    }

    @Test
    void requiresManualItemsWhenRecipeHasNoIngredients() {
        ownedList();
        source(11, List.of());
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, null, null, null))).isInstanceOf(AppException.class);
        verifyNoInteractions(aggregation, items, contributions, entityManager);
    }

    @Test
    void skipsRecipeContributionWhenAlreadyPresentAndNoManualItemsAreRequested() {
        ownedList();
        source(11, List.of(new RecipeService.ShoppingIngredient(3L, BigDecimal.TEN, 1)));
        var existing = new ShoppingRecipeContributionEntity();
        existing.setRecipeId(11L);
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(existing));
        when(checklists.get(7L)).thenReturn(new ShoppingListResponse(7L, "Đi chợ", null, null, List.of()));
        var result = service.addRecipe(7, request(11L, null, null, null));
        assertThat(result).isNotNull();
        verify(checklists).get(7L);
        verifyNoInteractions(aggregation, items, entityManager);
        verify(contributions, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsRecipeQuantitiesThatRoundToZeroOrExceedStorageLimit() {
        ownedList();
        source(11, List.of(new RecipeService.ShoppingIngredient(3L, new BigDecimal("0.0001"), 1)));
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, 1, null, null))).isInstanceOf(AppException.class);

        source(11, List.of(new RecipeService.ShoppingIngredient(3L, new BigDecimal("100000000"), 1)));
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(items.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(aggregation.aggregate(anyList())).thenReturn(new ShoppingAggregationResult(
                List.of(new ShoppingAggregationResult.Group("Đậu", List.of(new ShoppingAggregationResult.Item(
                        3L, "Đậu", new BigDecimal("100000000"), 1, "g", "gram", false, List.of(0))))), List.of()));
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, null, null, null))).isInstanceOf(AppException.class);
    }

    @Test
    void addsOnlySelectedRecipeIngredientsAtRequestedServingsAndPersistsContribution() {
        ownedList();
        source(8, List.of(new RecipeService.ShoppingIngredient(4L, BigDecimal.ONE, 1),
                new RecipeService.ShoppingIngredient(5L, BigDecimal.TEN, 1)));
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(items.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(checklists.get(7L)).thenReturn(new ShoppingListResponse(7L, "Đi chợ", null, null, List.of()));
        when(aggregation.aggregate(anyList())).thenReturn(result(2, List.of(0)));
        when(items.save(any())).thenAnswer(call -> {
            ShoppingListItemEntity saved = call.getArgument(0);
            saved.setId(80L);
            return saved;
        });

        var response = service.addRecipe(7L, request(8L, 4, List.of(4L), null));

        @SuppressWarnings("unchecked") ArgumentCaptor<List<ShoppingAggregationLine>> lines = ArgumentCaptor.forClass(List.class);
        verify(aggregation).aggregate(lines.capture());
        assertThat(lines.getValue()).singleElement().satisfies(line -> {
            assertThat(line.ingredientId()).isEqualTo(4L);
            assertThat(line.quantity()).isEqualByComparingTo("2");
        });
        ArgumentCaptor<List<ShoppingRecipeContributionEntity>> added = ArgumentCaptor.forClass(List.class);
        verify(contributions, times(2)).saveAllAndFlush(added.capture());
        assertThat(added.getAllValues().getLast()).singleElement().satisfies(row -> {
            assertThat(row.getShoppingListId()).isEqualTo(7L);
            assertThat(row.getRecipeId()).isEqualTo(8L);
            assertThat(row.getIngredientId()).isEqualTo(4L);
            assertThat(row.getQuantity()).isEqualByComparingTo("2");
            assertThat(row.getShoppingListItemId()).isEqualTo(80L);
        });
        assertThat(list.getUpdatedAt()).isNotNull();
        assertThat(response.warnings()).isEmpty();
        verify(entityManager).flush();
    }

    @Test
    void duplicateRecipeCanStillAddManualItemsWithoutDuplicatingItsIngredients() {
        ownedList();
        source(8, List.of(new RecipeService.ShoppingIngredient(4L, BigDecimal.ONE, 1)));
        var existing = new ShoppingRecipeContributionEntity();
        existing.setRecipeId(8L);
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(existing));
        when(items.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(checklists.get(7L)).thenReturn(new ShoppingListResponse(7L, "Đi chợ", null, null, List.of()));
        when(aggregation.aggregate(List.of())).thenReturn(new ShoppingAggregationResult(List.of(), List.of()));
        var manual = new CreateShoppingItemRequest("Táo", BigDecimal.ONE, 2);

        service.addRecipe(7L, request(8L, null, List.of(4L), List.of(manual)));

        verify(aggregation).aggregate(List.of());
        verify(checklists).add(7L, manual);
        verify(contributions, never()).saveAllAndFlush(anyList());
        verify(items).deleteAll(List.of());
        verify(entityManager).flush();
    }

    private ShoppingAggregationResult result(int quantity, List<Integer> sourceIndices) {
        return new ShoppingAggregationResult(List.of(new ShoppingAggregationResult.Group("Đậu", List.of(
                new ShoppingAggregationResult.Item(4L, "Đậu phụ", BigDecimal.valueOf(quantity), 1,
                        "g", "gam", false, sourceIndices)))), List.of());
    }

    private void ownedList() {
        when(lists.lockOwned(7L, 17L)).thenReturn(Optional.of(list));
    }

    private void source(long id, List<RecipeService.ShoppingIngredient> ingredients) {
        when(recipes.findShoppingSources(List.of(id))).thenReturn(Map.of(id,
                new RecipeService.ShoppingSource(id, 2, ingredients)));
    }

    private AddRecipeToShoppingListRequest request(Long recipeId, Integer servings,
            List<Long> selectedIds, List<CreateShoppingItemRequest> manualItems) {
        return new AddRecipeToShoppingListRequest(recipeId, servings, selectedIds, manualItems);
    }
}
