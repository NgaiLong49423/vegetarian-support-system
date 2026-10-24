package tech.mamxanh.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.recipe.service.RecipeService;
import tech.mamxanh.shopping.dto.request.AddRecipeToShoppingListRequest;
import tech.mamxanh.shopping.dto.request.CreateShoppingItemRequest;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult;
import tech.mamxanh.shopping.entity.ShoppingListEntity;
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
                checklists, entityManager, Clock.systemUTC());
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

        assertThatThrownBy(() -> service.addRecipe(7, request(11L, null, null, null)))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(aggregation, items, contributions, entityManager);
    }

    @Test
    void skipsRecipeContributionWhenAlreadyPresentAndNoManualItemsAreRequested() {
        ownedList();
        source(11, List.of(new RecipeService.ShoppingIngredient(3L, BigDecimal.TEN, 1)));
        var existing = new ShoppingRecipeContributionEntity();
        existing.setRecipeId(11L);
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(existing));

        var result = service.addRecipe(7, request(11L, null, null, null));

        assertThat(result).isNotNull();
        verify(checklists).get(7L);
        verifyNoInteractions(aggregation, items, entityManager);
    }

    @Test
    void rejectsRecipeQuantitiesThatRoundToZeroOrExceedStorageLimit() {
        ownedList();
        source(11, List.of(new RecipeService.ShoppingIngredient(3L, new BigDecimal("0.0001"), 1)));
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, 1, null, null)))
                .isInstanceOf(AppException.class);

        source(11, List.of(new RecipeService.ShoppingIngredient(3L, new BigDecimal("100000000"), 1)));
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(items.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(aggregation.aggregate(org.mockito.ArgumentMatchers.anyList())).thenReturn(new ShoppingAggregationResult(
                List.of(new ShoppingAggregationResult.Group("Đậu", List.of(new ShoppingAggregationResult.Item(
                        3L, "Đậu", new BigDecimal("100000000"), 1, "g", "gram", false, List.of(0))))), List.of()));
        assertThatThrownBy(() -> service.addRecipe(7, request(11L, null, null, null)))
                .isInstanceOf(AppException.class);
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
