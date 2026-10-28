package tech.mamxanh.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.shopping.dto.request.UpdateShoppingRecipeSourceServingsRequest;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.entity.ShoppingListEntity;
import tech.mamxanh.shopping.entity.ShoppingListItemEntity;
import tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity;
import tech.mamxanh.shopping.repository.ShoppingListItemRepository;
import tech.mamxanh.shopping.repository.ShoppingListRepository;
import tech.mamxanh.shopping.repository.ShoppingRecipeContributionRepository;

@ExtendWith(MockitoExtension.class)
class ShoppingRecipeSourceServiceTest {
    @Mock CurrentUserService users;
    @Mock ShoppingListRepository lists;
    @Mock ShoppingListItemRepository items;
    @Mock ShoppingRecipeContributionRepository contributions;
    @Mock ShoppingAggregationService aggregation;
    @Mock ShoppingListService checklists;

    private ShoppingRecipeSourceService service;
    private ShoppingListEntity list;

    @BeforeEach
    void setUp() {
        service = new ShoppingRecipeSourceService(users, lists, items, contributions, aggregation, checklists,
                Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC));
        list = new ShoppingListEntity();
        list.setId(7L);
        when(users.lockActiveMember()).thenReturn(new CurrentUserService.CurrentUser(17, Role.CUSTOMER));
    }

    @Test
    void updateServingsRejectsInvalidOwnershipAndMissingOrLegacySource() {
        assertThatThrownBy(() -> service.updateServings(7, UUID.randomUUID(), null)).isInstanceOf(AppException.class);
        when(lists.lockOwned(7L, 17L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateServings(7, UUID.randomUUID(), request("2"))).isInstanceOf(AppException.class);
        when(lists.lockOwned(7L, 17L)).thenReturn(Optional.of(list));
        assertThatThrownBy(() -> service.updateServings(7, null, request("2"))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.updateServings(7, UUID.randomUUID(), request("0.55"))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.updateServings(7, UUID.randomUUID(), request("51"))).isInstanceOf(AppException.class);
        UUID sourceId = UUID.randomUUID();
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.updateServings(7, sourceId, request("2"))).isInstanceOf(AppException.class);
        var row = source(sourceId, 3L, "10", "2");
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(row));
        row.setServings(null);
        assertThatThrownBy(() -> service.updateServings(7, sourceId, request("3"))).isInstanceOf(AppException.class);
    }

    @Test
    void updateServingsScalesRecipeContributionAndPreservesManualAmount() {
        UUID sourceId = UUID.randomUUID();
        var recipe = source(sourceId, 3L, "10", "2");
        var manual = new ShoppingRecipeContributionEntity();
        manual.setSourceKind("MANUAL"); manual.setSourceId(UUID.randomUUID()); manual.setShoppingListItemId(3L);
        manual.setIngredientId(9L); manual.setUnitId(1); manual.setQuantity(new BigDecimal("5"));
        var item = item(3L, "15");
        owned();
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(recipe, manual));
        when(items.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(item));
        when(aggregation.aggregate(anyList())).thenReturn(aggregate(1, "15"), aggregate(1, "25"));
        when(checklists.get(7L)).thenReturn(emptyResponse());

        service.updateServings(7, sourceId, request("4"));

        assertThat(recipe.getQuantity()).isEqualByComparingTo("20.000000");
        assertThat(recipe.getServings()).isEqualByComparingTo("4");
        assertThat(item.getQuantity()).isEqualByComparingTo("25.000000");
        verify(contributions).saveAll(List.of(recipe));
    }

    @Test
    void updateRecipeServingsDistributesAcrossOccurrencesAndRejectsIncompleteOrInvalidRequests() {
        owned();
        assertThatThrownBy(() -> service.updateRecipeServings(7, 0, request("2"))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.updateRecipeServings(7, 9, null)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.updateRecipeServings(7, 9, request("0.55"))).isInstanceOf(AppException.class);
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.updateRecipeServings(7, 9, request("2"))).isInstanceOf(AppException.class);

        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        var a = source(first, 3L, "10", "1"); a.setExpectedIngredientCount(1); a.setRecipeId(9L);
        var b = source(second, 4L, "20", "3"); b.setExpectedIngredientCount(1); b.setRecipeId(9L);
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(a, b));
        assertThatThrownBy(() -> service.updateRecipeServings(7, 9, request("0.5"))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.updateRecipeServings(7, 9, request("501"))).isInstanceOf(AppException.class);
        when(items.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(item(3L,"10"), item(4L,"20")));
        when(aggregation.aggregate(anyList())).thenReturn(aggregate(1,"10"), aggregate(1,"20"),
                aggregate(1,"11"), aggregate(1,"20.666667"));
        when(checklists.get(7L)).thenReturn(emptyResponse());
        service.updateRecipeServings(7, 9, request("4.2"));
        assertThat(a.getServings().add(b.getServings())).isEqualByComparingTo("4.2");
        assertThat(a.getServings()).isGreaterThanOrEqualTo(new BigDecimal("0.5"));
    }

    @Test
    void removeRecipeDeletesOrReaggregatesAffectedItemsAndRejectsBrokenSources() {
        owned();
        assertThatThrownBy(() -> service.removeRecipe(7, 0)).isInstanceOf(AppException.class);
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.removeRecipe(7, 9)).isInstanceOf(AppException.class);

        UUID sourceId = UUID.randomUUID();
        var removed = source(sourceId, 3L, "10", "2"); removed.setRecipeId(9L); removed.setExpectedIngredientCount(2);
        var removedSecond = source(sourceId, 4L, "10", "2"); removedSecond.setRecipeId(9L); removedSecond.setExpectedIngredientCount(2);
        var retained = new ShoppingRecipeContributionEntity();
        retained.setSourceKind("MANUAL"); retained.setShoppingListId(7L); retained.setShoppingListItemId(4L);
        retained.setIngredientId(8L); retained.setUnitId(1); retained.setQuantity(new BigDecimal("5"));
        var retainedRecipeSource = source(UUID.randomUUID(), 4L, "10", "2");
        retainedRecipeSource.setRecipeId(10L); retainedRecipeSource.setExpectedIngredientCount(1);
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(removed, removedSecond), List.of(retained, retainedRecipeSource));
        var itemOnlyFromRemoved = item(3L,"10");
        var sharedItem = item(4L,"15"); sharedItem.setBought(true);
        when(items.findByIdAndShoppingListId(3L,7L)).thenReturn(Optional.of(itemOnlyFromRemoved));
        when(items.findByIdAndShoppingListId(4L,7L)).thenReturn(Optional.of(sharedItem));
        when(aggregation.aggregate(anyList())).thenReturn(aggregate(1,"15"));
        when(checklists.get(7L)).thenReturn(emptyResponse());

        service.removeRecipe(7, 9);

        verify(items).delete(itemOnlyFromRemoved);
        verify(items).save(sharedItem);
        assertThat(sharedItem.getQuantity()).isEqualByComparingTo("15");
        assertThat(sharedItem.isBought()).isFalse();
        verify(contributions).deleteAll(List.of(removed, removedSecond));
        verify(contributions).flush();
    }

    @Test
    void removeRecipeRejectsIncompleteAndUnsafeAggregation() {
        owned();
        UUID sourceId = UUID.randomUUID();
        var incomplete = source(sourceId, 3L, "10", "2"); incomplete.setExpectedIngredientCount(2);
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(incomplete));
        assertThatThrownBy(() -> service.removeRecipe(7, 9)).isInstanceOf(AppException.class);

        var recipe = source(sourceId,3L,"10","2"); recipe.setExpectedIngredientCount(1); recipe.setRecipeId(9L);
        var manual = new ShoppingRecipeContributionEntity(); manual.setSourceKind("MANUAL"); manual.setShoppingListItemId(3L);
        manual.setIngredientId(4L); manual.setUnitId(1); manual.setQuantity(BigDecimal.ONE);
        when(contributions.findByShoppingListIdOrderByIdAsc(7L)).thenReturn(List.of(recipe), List.of(manual));
        when(items.findByIdAndShoppingListId(3L,7L)).thenReturn(Optional.of(item(3L,"11")));
        when(aggregation.aggregate(anyList())).thenReturn(new ShoppingAggregationResult(List.of(), List.of()));
        assertThatThrownBy(() -> service.removeRecipe(7, 9)).isInstanceOf(AppException.class);
    }

    private void owned() {
        when(lists.lockOwned(7L,17L)).thenReturn(Optional.of(list));
    }

    private static UpdateShoppingRecipeSourceServingsRequest request(String servings) {
        return new UpdateShoppingRecipeSourceServingsRequest(new BigDecimal(servings));
    }

    private static ShoppingRecipeContributionEntity source(UUID sourceId, long itemId, String quantity, String servings) {
        var row = new ShoppingRecipeContributionEntity();
        row.setShoppingListId(7L); row.setSourceKind("RECIPE"); row.setSourceId(sourceId); row.setRecipeId(9L);
        row.setIngredientId(4L); row.setUnitId(1); row.setShoppingListItemId(itemId);
        row.setQuantity(new BigDecimal(quantity)); row.setServings(new BigDecimal(servings));
        row.setExpectedIngredientCount(1);
        return row;
    }

    private static ShoppingListItemEntity item(long id, String quantity) {
        var item = new ShoppingListItemEntity(); item.setId(id); item.setShoppingListId(7L);
        item.setIngredientId(4L); item.setUnitId(1); item.setQuantity(new BigDecimal(quantity)); item.setBought(true);
        return item;
    }

    private static ShoppingAggregationResult aggregate(int unit, String quantity) {
        return new ShoppingAggregationResult(List.of(new ShoppingAggregationResult.Group("Nguyên liệu", List.of(
                new ShoppingAggregationResult.Item(4L, "Nguyên liệu", new BigDecimal(quantity), unit, "g", "gram", false, List.of(0))))), List.of());
    }

    private static ShoppingListResponse emptyResponse() {
        return new ShoppingListResponse(7L, "Đi chợ", null, null, List.of());
    }
}
