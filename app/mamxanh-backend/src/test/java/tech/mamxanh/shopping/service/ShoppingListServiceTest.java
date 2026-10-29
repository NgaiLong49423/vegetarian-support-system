package tech.mamxanh.shopping.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.nutrition.dto.response.*;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.service.IngredientCatalogLookupService;
import tech.mamxanh.shopping.dto.request.*;
import tech.mamxanh.shopping.entity.*;
import tech.mamxanh.shopping.repository.*;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {
    @Mock ShoppingListRepository lists;
    @Mock ShoppingListItemRepository items;
    @Mock CurrentUserService users;
    @Mock IngredientCatalogLookupService catalog;
    @Mock ShoppingRecipeContributionRepository contributions;
    @Mock tech.mamxanh.recipe.service.RecipeService recipes;
    @Mock tech.mamxanh.shopping.service.ShoppingAggregationService aggregation;
    final Clock clock = Clock.fixed(Instant.parse("2026-10-17T03:00:00Z"), ZoneOffset.UTC);
    ShoppingListService service;
    ShoppingListEntity list;
    ShoppingListItemEntity item;
    final UnitResponse roll = new UnitResponse(7, "cuộn", "cuộn", MeasurementDimension.COUNT, BigDecimal.ONE, true);

    @BeforeEach void setup() {
        service = new ShoppingListService(lists, items, users, catalog, clock, contributions, recipes,
                org.mockito.Mockito.mock(jakarta.persistence.EntityManager.class), aggregation);
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(17, Role.CUSTOMER));
        list = new ShoppingListEntity(); list.setId(10L); list.setUserId(17L); list.setName("Đi chợ");
        item = new ShoppingListItemEntity(); item.setId(3L); item.setShoppingListId(10L);
        item.setCustomIngredientName("Khăn giấy"); item.setUnitId(7); item.setQuantity(new BigDecimal("2"));
    }

    void mutation() { when(lists.lockOwned(10, 17)).thenReturn(Optional.of(list)); }
    void existingItem() { when(items.findByIdAndShoppingListId(3L, 10L)).thenReturn(Optional.of(item)); }
    void references() {
        when(catalog.findIngredientReferences(anyCollection())).thenReturn(List.of());
        when(catalog.findUnitReferences(anyCollection())).thenReturn(List.of(roll));
    }

    @Test void readsOnlyOwnedListsAndBatchesReferencesIncludingInactiveEntries() {
        when(lists.findByIdAndUserId(10L, 17L)).thenReturn(Optional.of(list));
        item.setIngredientId(8L);
        when(items.findByShoppingListIdOrderByIdAsc(10L)).thenReturn(List.of(item));
        when(catalog.findIngredientReferences(List.of(8L))).thenReturn(List.of(
                new IngredientResponse(8L, "Đậu phụ", "Đậu", null, "/ingredients/dau-phu.webp", null, null,
                        false, false, null)));
        when(catalog.findUnitReferences(List.of(7))).thenReturn(List.of(roll));
        var response = service.get(10);
        assertThat(response.items()).singleElement().satisfies(i -> {
            assertThat(i.name()).isEqualTo("Đậu phụ"); assertThat(i.ingredientGroup()).isEqualTo("Đậu");
            assertThat(i.imageUrl()).isEqualTo("/ingredients/dau-phu.webp");
        });
    }

    @Test void listUsesOwnerAndBoundedStablePaging() {
        when(lists.findByUserId(eq(17L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(list)));
        assertThat(service.list(0, 20).content()).singleElement().satisfies(s -> assertThat(s.id()).isEqualTo(10));
        verify(lists).findByUserId(eq(17L), argThat(p -> p.getSort().toString().equals("updatedAt: DESC,id: DESC")));
    }

    @Test void renamesOnlyOwnedListAndTrimsName() {
        mutation();
        var result = service.rename(10, new RenameShoppingListRequest("  Đi chợ cuối tuần  "));
        assertThat(result.name()).isEqualTo("Đi chợ cuối tuần");
        assertThat(result.updatedAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(list.getName()).isEqualTo("Đi chợ cuối tuần");
    }

    @ParameterizedTest @ValueSource(strings = {"", "   ", "\u00a0\u00a0", "Tên\nẩn"})
    void rejectsBlankOrControlCharactersInListName(String name) {
        mutation();
        assertThatThrownBy(() -> service.rename(10, new RenameShoppingListRequest(name)))
                .isInstanceOf(AppException.class);
        assertThat(list.getName()).isEqualTo("Đi chợ");
    }

    @Test void deleteListRemovesContributionRowsBeforeDeletingOwnedList() {
        mutation();
        var order = inOrder(contributions, lists);
        service.deleteList(10);
        order.verify(contributions).deleteAllByShoppingListId(10);
        order.verify(lists).delete(list);
    }

    @Test void rejectsBadPagingBeforeQuerying() {
        assertThatThrownBy(() -> service.list(-1, 20)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.list(0, 0)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.list(0, 51)).isInstanceOf(AppException.class);
        verifyNoInteractions(lists, items, catalog);
    }

    @Test void foreignListStopsBeforeReadingOrWritingItems() {
        assertThatThrownBy(() -> service.get(10)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.delete(10, 3)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.deleteList(10)).isInstanceOf(AppException.class);
        verifyNoInteractions(items, catalog);
        verifyNoInteractions(contributions);
        verify(lists, never()).delete(any(ShoppingListEntity.class));
    }

    @Test void authenticationFailureStopsAllDataAccess() {
        when(users.requireActiveMember()).thenThrow(new AppException(ErrorCode.UNAUTHENTICATED));
        assertThatThrownBy(() -> service.options()).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.add(10, new CreateShoppingItemRequest("Khăn", BigDecimal.ONE, 7)))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(lists, items, catalog);
    }

    @Test void addsTrimmedManualItemUnboughtAndTouchesList() {
        mutation(); references(); when(catalog.findActiveUnits()).thenReturn(List.of(roll));
        when(items.saveAndFlush(any())).thenAnswer(call -> { ShoppingListItemEntity saved = call.getArgument(0); saved.setId(44L); return saved; });
        var result = service.add(10, new CreateShoppingItemRequest("  Khăn giấy lau bếp  ", new BigDecimal("2"), 7));
        assertThat(result.name()).isEqualTo("Khăn giấy lau bếp");
        assertThat(result.bought()).isFalse(); assertThat(result.ingredientId()).isNull();
        assertThat(list.getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
        verify(items).saveAndFlush(argThat(i -> i.getShoppingListId() == 10 && i.getIngredientId() == null));
    }

    @Test void addsExactCatalogIngredientByMergingIntoItsExistingChecklistItem() {
        mutation(); references();
        var ingredient = new IngredientResponse(8L, "Đậu phụ", "Đậu", null, null, null, false, true);
        when(catalog.findActiveIngredientsByIds(List.of(8L))).thenReturn(List.of(ingredient));
        when(catalog.findActiveIngredientUnits(8L)).thenReturn(List.of(new IngredientUnitOptionResponse(
                7, "cuộn", "cuộn", MeasurementDimension.COUNT, BigDecimal.ONE, false)));
        item.setIngredientId(8L);
        when(items.findByShoppingListIdOrderByIdAsc(10L)).thenReturn(List.of(item));
        var sourceRows = new ArrayList<ShoppingRecipeContributionEntity>();
        when(contributions.findByShoppingListIdOrderByIdAsc(10L)).thenAnswer(call -> sourceRows);
        doAnswer(call -> { sourceRows.add(call.getArgument(0)); return call.getArgument(0); })
                .when(contributions).saveAndFlush(any());
        when(catalog.findIngredientReferences(List.of(8L))).thenReturn(List.of(ingredient));
        when(aggregation.aggregate(anyList())).thenAnswer(call -> aggregate(call.getArgument(0)));

        var result = service.add(10, new CreateShoppingItemRequest("Đậu phụ", new BigDecimal("3"), 7, 8L));

        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.quantity()).isEqualByComparingTo("5");
        assertThat(result.ingredientId()).isEqualTo(8L);
        verify(items).save(item);
        assertThat(sourceRows).singleElement().satisfies(source -> {
            assertThat(source.getSourceKind()).isEqualTo("MANUAL");
            assertThat(source.getShoppingListItemId()).isEqualTo(3L);
            assertThat(source.getQuantity()).isEqualByComparingTo("3");
        });
    }

    @ParameterizedTest @ValueSource(strings = {"", "   ", "\u00a0\u00a0", "<script>alert(1)</script>", "Tên\nẩn"})
    void rejectsBlankOrMarkupAndControlCharacters(String name) {
        mutation();
        assertThatThrownBy(() -> service.add(10, new CreateShoppingItemRequest(name, BigDecimal.ONE, 7)))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(items, catalog);
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "0.0000001", "1000000000000"})
    void rejectsInvalidOrUnrepresentableQuantities(String amount) {
        mutation();
        assertThatThrownBy(() -> service.add(10, new CreateShoppingItemRequest("Khăn", new BigDecimal(amount), 7)))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(items, catalog);
    }

    @Test void rejectsInactiveOrUnknownUnitWithoutSaving() {
        mutation(); when(catalog.findActiveUnits()).thenReturn(List.of());
        assertThatThrownBy(() -> service.add(10, new CreateShoppingItemRequest("Khăn", BigDecimal.ONE, 7)))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(items);
    }

    @Test void quantityUpdatePreservesIdentityAndBoughtStateAndRetainsExistingUnit() {
        mutation(); existingItem(); references(); item.setBought(true); item.setIngredientId(8L);
        when(catalog.findActiveIngredientUnits(8L)).thenReturn(List.of(new IngredientUnitOptionResponse(
                7, "cuộn", "cuộn", MeasurementDimension.COUNT, BigDecimal.ONE, false)));
        when(aggregation.aggregate(anyList())).thenReturn(new tech.mamxanh.shopping.dto.response.ShoppingAggregationResult(
                List.of(new tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Group("Khác", List.of(
                        new tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item(8L, "Khăn giấy",
                                new BigDecimal("3.5"), 7, "cuộn", "cuộn", false)))), List.of()));
        var result = service.update(10, 3, new UpdateShoppingItemRequest(new BigDecimal("3.5"), 7));
        assertThat(result.quantity()).isEqualByComparingTo("3.5"); assertThat(result.bought()).isTrue();
        assertThat(item.getIngredientId()).isEqualTo(8); verify(catalog, never()).findActiveUnits();
    }

    @Test void changingUnitValidatesTheNewSelection() {
        mutation(); existingItem(); when(catalog.findActiveUnits()).thenReturn(List.of(roll));
        assertThatThrownBy(() -> service.update(10, 3, new UpdateShoppingItemRequest(BigDecimal.ONE, 99)))
                .isInstanceOf(AppException.class);
        assertThat(item.getUnitId()).isEqualTo(7); assertThat(list.getUpdatedAt()).isNull();
    }

    @Test void editingTotalKeepsRecipeMinimumAndReducesUnknownBucketBeforeManualAmount() {
        mutation();
        item.setIngredientId(8L); item.setUnitId(1); item.setQuantity(new BigDecimal("70")); item.setBought(true);
        when(items.findByIdAndShoppingListId(3L, 10L)).thenReturn(Optional.of(item));
        when(catalog.findActiveIngredientUnits(8L)).thenReturn(List.of(
                new IngredientUnitOptionResponse(1, "g", "gram", MeasurementDimension.MASS, BigDecimal.ONE, false)));
        var required = contribution("RECIPE", 3L, "40"); required.setRecipeId(9L);
        var unknownA = contribution("UNKNOWN", 3L, "10"); unknownA.setRecipeId(9L);
        var unknownB = contribution("UNKNOWN", 3L, "10"); unknownB.setRecipeId(9L);
        var manual = contribution("MANUAL", 3L, "10");
        var rows = new ArrayList<>(List.of(required, unknownA, unknownB, manual));
        when(contributions.findByShoppingListIdOrderByIdAsc(10L)).thenAnswer(call -> rows);
        doAnswer(call -> { rows.removeAll(call.getArgument(0)); return null; }).when(contributions).deleteAll(anyList());
        when(recipes.findShoppingReferences(List.of(9L))).thenReturn(Map.of(9L,
                new tech.mamxanh.recipe.service.RecipeService.ShoppingReference(9L, "Món đậu", true, null)));
        when(catalog.findIngredientReferences(List.of(8L))).thenReturn(List.of(
                new IngredientResponse(8L, "Đậu phụ", "Đậu", null, null, null, false, true)));
        when(catalog.findUnitReferences(List.of(1))).thenReturn(List.of(
                new UnitResponse(1, "g", "gram", MeasurementDimension.MASS, BigDecimal.ONE, true)));
        when(aggregation.aggregate(anyList())).thenAnswer(call -> aggregate(call.getArgument(0)));

        var result = service.update(10, 3, new UpdateShoppingItemRequest(new BigDecimal("45"), 1));

        assertThat(item.getQuantity()).isEqualByComparingTo("45");
        assertThat(rows).containsExactly(required, manual);
        assertThat(manual.getQuantity()).isEqualByComparingTo("5");
        assertThat(result.requiredQuantity()).isEqualByComparingTo("40");
        assertThat(result.manualQuantity()).isEqualByComparingTo("5");
        assertThat(item.isBought()).isTrue();
    }

    @Test void increasingTotalDoesNotIncreaseTheUnresolvedContributionBucket() {
        var rows = prepareRecipeBackedIngredientSources();

        var result = service.update(10, 3, new UpdateShoppingItemRequest(new BigDecimal("80"), 1));

        assertThat(item.getQuantity()).isEqualByComparingTo("80");
        assertThat(rows).filteredOn(source -> "UNKNOWN".equals(source.getSourceKind()))
                .extracting(ShoppingRecipeContributionEntity::getQuantity).containsExactly(new BigDecimal("10"), new BigDecimal("10"));
        assertThat(rows).filteredOn(source -> "MANUAL".equals(source.getSourceKind())).singleElement()
                .satisfies(source -> assertThat(source.getQuantity()).isEqualByComparingTo("20"));
        assertThat(result.requiredQuantity()).isEqualByComparingTo("40");
        assertThat(result.manualQuantity()).isEqualByComparingTo("20");
    }

    @Test void reducingTotalToRecipeMinimumRemovesUnknownAndManualContributions() {
        var rows = prepareRecipeBackedIngredientSources();
        doAnswer(call -> { rows.removeAll(call.getArgument(0)); return null; }).when(contributions).deleteAll(anyList());

        var result = service.update(10, 3, new UpdateShoppingItemRequest(new BigDecimal("40"), 1));

        assertThat(item.getQuantity()).isEqualByComparingTo("40");
        assertThat(rows).extracting(ShoppingRecipeContributionEntity::getSourceKind).containsExactly("RECIPE");
        assertThat(result.requiredQuantity()).isEqualByComparingTo("40");
        assertThat(result.manualQuantity()).isNull();
    }

    @Test void editingAndRemovingStandaloneManualSourceRecalculatesOrDeletesItsChecklistRow() {
        mutation(); references();
        var customItem = new ShoppingListItemEntity(); customItem.setId(3L); customItem.setShoppingListId(10L);
        customItem.setCustomIngredientName("Khăn giấy"); customItem.setUnitId(7); customItem.setQuantity(new BigDecimal("2"));
        var manual = contribution("MANUAL", 3L, "2"); manual.setUnitId(7); manual.setIngredientId(null);
        var rows = new ArrayList<>(List.of(manual));
        when(contributions.findByShoppingListIdOrderByIdAsc(10L)).thenAnswer(call -> rows);
        when(items.findByIdAndShoppingListId(3L, 10L)).thenReturn(Optional.of(customItem));
        doAnswer(call -> { rows.remove(call.getArgument(0)); return null; }).when(contributions).delete(manual);

        var updated = service.updateManualSource(10, manual.getSourceId(),
                new UpdateShoppingItemRequest(new BigDecimal("3"), 7));
        assertThat(updated.quantity()).isEqualByComparingTo("3");
        assertThat(customItem.getQuantity()).isEqualByComparingTo("3");
        assertThat(customItem.isBought()).isFalse();

        service.deleteManualSource(10, manual.getSourceId());
        verify(items).deleteById(3L);
        assertThat(list.getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test void explicitBoughtAssignmentIsIdempotentAndDoesNotOverwriteQuantity() {
        mutation(); existingItem(); references();
        service.setBought(10, 3, true);
        var timestamp = list.getUpdatedAt();
        service.setBought(10, 3, true);
        assertThat(list.getUpdatedAt()).isEqualTo(timestamp);
        assertThat(item.getQuantity()).isEqualByComparingTo("2");
        service.setBought(10, 3, false); assertThat(item.isBought()).isFalse();
    }

    @Test void wrongItemListCannotBeMutated() {
        mutation();
        assertThatThrownBy(() -> service.setBought(10, 3, true)).isInstanceOf(AppException.class);
        verify(items, never()).delete(any(ShoppingListItemEntity.class));
    }

    @Test void deletesOnlyTheRequestedOwnedItem() {
        mutation(); existingItem(); service.delete(10, 3); verify(items).delete(item);
        assertThat(list.getUpdatedAt()).isNotNull();
    }

    @Test void cleanupChangesTimestampOnlyWhenRowsWereRemoved() {
        mutation(); item.setBought(true);
        when(items.findByShoppingListIdOrderByIdAsc(10L)).thenReturn(List.of(), List.of(item));
        service.clearBought(10); assertThat(list.getUpdatedAt()).isNull();
        service.clearBought(10); assertThat(list.getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
        verify(items).deleteAll(List.of(item));
    }

    @Test void invalidIdsAreRejected() {
        assertThatThrownBy(() -> service.get(0)).isInstanceOf(AppException.class);
        mutation(); assertThatThrownBy(() -> service.delete(10, -1)).isInstanceOf(AppException.class);
        verifyNoInteractions(items);
    }

    private static ShoppingRecipeContributionEntity contribution(String kind, long itemId, String quantity) {
        var row = new ShoppingRecipeContributionEntity();
        row.setShoppingListId(10L); row.setShoppingListItemId(itemId); row.setSourceKind(kind);
        row.setSourceId(UUID.randomUUID()); row.setIngredientId(8L); row.setUnitId(1);
        row.setQuantity(new BigDecimal(quantity)); row.setExpectedIngredientCount(1);
        if ("RECIPE".equals(kind) || "UNKNOWN".equals(kind)) row.setRecipeId(9L);
        return row;
    }

    private List<ShoppingRecipeContributionEntity> prepareRecipeBackedIngredientSources() {
        mutation();
        item.setIngredientId(8L); item.setUnitId(1); item.setQuantity(new BigDecimal("70"));
        when(items.findByIdAndShoppingListId(3L, 10L)).thenReturn(Optional.of(item));
        when(catalog.findActiveIngredientUnits(8L)).thenReturn(List.of(new IngredientUnitOptionResponse(
                1, "g", "gram", MeasurementDimension.MASS, BigDecimal.ONE, false)));
        var required = contribution("RECIPE", 3L, "40");
        var unknownA = contribution("UNKNOWN", 3L, "10");
        var unknownB = contribution("UNKNOWN", 3L, "10");
        var manual = contribution("MANUAL", 3L, "10");
        var rows = new ArrayList<>(List.of(required, unknownA, unknownB, manual));
        when(contributions.findByShoppingListIdOrderByIdAsc(10L)).thenAnswer(call -> rows);
        when(recipes.findShoppingReferences(List.of(9L))).thenReturn(Map.of(9L,
                new tech.mamxanh.recipe.service.RecipeService.ShoppingReference(9L, "Món đậu", true, null)));
        when(catalog.findIngredientReferences(List.of(8L))).thenReturn(List.of(
                new IngredientResponse(8L, "Đậu phụ", "Đậu", null, null, null, false, true)));
        when(catalog.findUnitReferences(List.of(1))).thenReturn(List.of(
                new UnitResponse(1, "g", "gram", MeasurementDimension.MASS, BigDecimal.ONE, true)));
        when(aggregation.aggregate(anyList())).thenAnswer(call -> aggregate(call.getArgument(0)));
        return rows;
    }

    private static tech.mamxanh.shopping.dto.response.ShoppingAggregationResult aggregate(
            List<ShoppingAggregationLine> lines) {
        BigDecimal quantity = lines.stream().map(ShoppingAggregationLine::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        var indices = java.util.stream.IntStream.range(0, lines.size()).boxed().toList();
        return new tech.mamxanh.shopping.dto.response.ShoppingAggregationResult(List.of(
                new tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Group("Đậu", List.of(
                        new tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item(
                                8L, "Đậu phụ", quantity, 1, "g", "gram", false, indices)))), List.of());
    }
}
