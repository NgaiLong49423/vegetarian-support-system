package tech.mamxanh.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

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

    @BeforeEach void setup() {
        service = new RecipeShoppingListService(users, recipes, aggregation, lists, items, contributions,
                checklists, entityManager, Clock.fixed(Instant.parse("2026-10-24T05:00:00Z"), ZoneOffset.UTC));
        when(users.lockActiveMember()).thenReturn(new CurrentUserService.CurrentUser(17, Role.CUSTOMER));
        list = new ShoppingListEntity();
        list.setId(10L);
        list.setUserId(17L);
        when(lists.lockOwned(10L, 17L)).thenReturn(Optional.of(list));
        when(checklists.get(10L)).thenReturn(new ShoppingListResponse(10L, "Đi chợ", null, null, List.of()));
    }

    @Test void addsSelectedRecipeIngredientsAtRequestedServingsAndPersistsContribution() {
        var source = new RecipeService.ShoppingSource(8, 2, List.of(
                new RecipeService.ShoppingIngredient(4L, BigDecimal.ONE, 1),
                new RecipeService.ShoppingIngredient(5L, BigDecimal.TEN, 1)));
        when(recipes.findShoppingSources(List.of(8L))).thenReturn(Map.of(8L, source));
        when(contributions.findByShoppingListIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(items.findByShoppingListIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(aggregation.aggregate(anyList())).thenReturn(result(2, List.of(0)));
        when(items.save(any())).thenAnswer(call -> {
            ShoppingListItemEntity saved = call.getArgument(0);
            saved.setId(80L);
            return saved;
        });

        var response = service.addRecipe(10L, new AddRecipeToShoppingListRequest(8L, 4, List.of(4L), null));

        @SuppressWarnings("unchecked") ArgumentCaptor<List<ShoppingAggregationLine>> lines = ArgumentCaptor.forClass(List.class);
        verify(aggregation).aggregate(lines.capture());
        assertThat(lines.getValue()).singleElement().satisfies(line -> {
            assertThat(line.ingredientId()).isEqualTo(4L);
            assertThat(line.quantity()).isEqualByComparingTo("2");
        });
        ArgumentCaptor<List<ShoppingRecipeContributionEntity>> added = ArgumentCaptor.forClass(List.class);
        verify(contributions, times(2)).saveAllAndFlush(added.capture());
        assertThat(added.getAllValues().getLast()).singleElement().satisfies(row -> {
            assertThat(row.getShoppingListId()).isEqualTo(10L);
            assertThat(row.getRecipeId()).isEqualTo(8L);
            assertThat(row.getIngredientId()).isEqualTo(4L);
            assertThat(row.getQuantity()).isEqualByComparingTo("2");
            assertThat(row.getShoppingListItemId()).isEqualTo(80L);
        });
        assertThat(list.getUpdatedAt()).isNotNull();
        assertThat(response.warnings()).isEmpty();
        verify(entityManager).flush();
    }

    @Test void existingRecipeContributionWithoutManualItemsIsIdempotent() {
        when(recipes.findShoppingSources(List.of(8L))).thenReturn(Map.of(8L,
                new RecipeService.ShoppingSource(8, 2, List.of(new RecipeService.ShoppingIngredient(4L, BigDecimal.ONE, 1)))));
        var existing = new ShoppingRecipeContributionEntity();
        existing.setRecipeId(8L);
        when(contributions.findByShoppingListIdOrderByIdAsc(10L)).thenReturn(List.of(existing));

        var response = service.addRecipe(10L, new AddRecipeToShoppingListRequest(8L, null, null, null));

        assertThat(response.warnings()).isEmpty();
        verifyNoInteractions(aggregation, items, entityManager);
        verify(contributions, never()).saveAllAndFlush(anyList());
        verify(checklists).get(10L);
    }

    @Test void duplicateRecipeCanStillAddManualItemsWithoutDuplicatingItsIngredients() {
        when(recipes.findShoppingSources(List.of(8L))).thenReturn(Map.of(8L,
                new RecipeService.ShoppingSource(8, 2, List.of(new RecipeService.ShoppingIngredient(4L, BigDecimal.ONE, 1)))));
        var existing = new ShoppingRecipeContributionEntity();
        existing.setRecipeId(8L);
        when(contributions.findByShoppingListIdOrderByIdAsc(10L)).thenReturn(List.of(existing));
        when(items.findByShoppingListIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(aggregation.aggregate(List.of())).thenReturn(new ShoppingAggregationResult(List.of(), List.of()));
        var manual = new CreateShoppingItemRequest("Táo", BigDecimal.ONE, 2);

        service.addRecipe(10L, new AddRecipeToShoppingListRequest(8L, null, List.of(4L), List.of(manual)));

        verify(aggregation).aggregate(List.of());
        verify(checklists).add(10L, manual);
        verify(contributions, never()).saveAllAndFlush(anyList());
        verify(items).deleteAll(List.of());
        verify(entityManager).flush();
    }

    private ShoppingAggregationResult result(int quantity, List<Integer> sourceIndices) {
        return new ShoppingAggregationResult(List.of(new ShoppingAggregationResult.Group("Đậu", List.of(
                new ShoppingAggregationResult.Item(4L, "Đậu phụ", BigDecimal.valueOf(quantity), 1,
                        "g", "gam", false, sourceIndices)))), List.of());
    }
}
