package tech.mamxanh.shopping.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.service.IngredientCatalogLookupService;
import tech.mamxanh.shopping.dto.request.CreateShoppingItemRequest;
import tech.mamxanh.shopping.dto.request.CreateEmptyShoppingListRequest;
import tech.mamxanh.shopping.dto.request.RenameShoppingListRequest;
import tech.mamxanh.shopping.dto.request.UpdateShoppingItemRequest;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse.Item;
import tech.mamxanh.shopping.entity.ShoppingListEntity;
import tech.mamxanh.shopping.entity.ShoppingListItemEntity;
import tech.mamxanh.shopping.repository.ShoppingListRepository;
import tech.mamxanh.shopping.repository.ShoppingListItemRepository;

/** FR-53 checklist operations on persisted lists; source creation is coordinated separately. */
@Service
public class ShoppingListService {
    private final ShoppingListRepository lists;
    private final ShoppingListItemRepository items;
    private final CurrentUserService users;
    private final IngredientCatalogLookupService catalog;
    private final Clock clock;
    private final tech.mamxanh.shopping.repository.ShoppingRecipeContributionRepository contributions;
    private final tech.mamxanh.recipe.service.RecipeService recipes;
    private final EntityManager entityManager;

    public ShoppingListService(ShoppingListRepository lists, ShoppingListItemRepository items,
            CurrentUserService users, IngredientCatalogLookupService catalog, Clock clock,
            tech.mamxanh.shopping.repository.ShoppingRecipeContributionRepository contributions,
            tech.mamxanh.recipe.service.RecipeService recipes, EntityManager entityManager) {
        this.lists = lists;
        this.items = items;
        this.users = users;
        this.catalog = catalog;
        this.clock = clock;
        this.contributions = contributions;
        this.recipes = recipes;
        this.entityManager = entityManager;
    }

    @Transactional
    public ShoppingListResponse createEmpty(CreateEmptyShoppingListRequest request) {
        long owner = users.requireActiveMember().id();
        String name = normalizeListName(request.name());
        var list = new ShoppingListEntity();
        list.setUserId(owner);
        list.setKind("MANUAL");
        list.setName(name);
        lists.saveAndFlush(list);
        entityManager.refresh(list);
        return new ShoppingListResponse(list.getId(), list.getName(), list.getCreatedAt(), list.getUpdatedAt(), List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<ShoppingListResponse.Summary> list(int page, int size) {
        long owner = users.requireActiveMember().id();
        if (page < 0 || size < 1 || size > 50) throw invalid("page phải >= 0; size từ 1 đến 50.");
        var result = lists.findByUserId(owner, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));
        return PageResponse.of(result.map(l -> new ShoppingListResponse.Summary(l.getId(), l.getName(),
                l.getCreatedAt(), l.getUpdatedAt())).getContent(), page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse get(long listId) {
        var list = owned(listId, false);
        return new ShoppingListResponse(list.getId(), list.getName(), list.getCreatedAt(), list.getUpdatedAt(),
                resolve(items.findByShoppingListIdOrderByIdAsc(listId)));
    }

    @Transactional
    public ShoppingListResponse.Summary rename(long listId, RenameShoppingListRequest request) {
        var list = owned(listId, true);
        String name = normalizeListName(request.name());
        list.setName(name);
        touch(list);
        return new ShoppingListResponse.Summary(list.getId(), list.getName(), list.getCreatedAt(), list.getUpdatedAt());
    }

    @Transactional
    public void deleteList(long listId) {
        var list = owned(listId, true);
        // Contributions reference checklist rows with NO ACTION; remove them before
        // the list's owned items cascade, without touching source recipes or meal plans.
        contributions.deleteAllByShoppingListId(listId);
        lists.delete(list);
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> options() {
        users.requireActiveMember();
        return catalog.findActiveUnits();
    }

    @Transactional
    public Item add(long listId, CreateShoppingItemRequest request) {
        var list = owned(listId, true);
        String name = request.name() == null ? "" : request.name().replace('\u00a0', ' ').strip();
        if (name.isBlank() || name.length() > 200 || name.codePoints().anyMatch(c -> c == '<' || c == '>' || Character.isISOControl(c))) {
            throw invalid("Tên mặt hàng phải là văn bản thuần, không rỗng và tối đa 200 ký tự.");
        }
        validateQuantity(request.quantity());
        requireActiveUnit(request.unitId());
        var item = new ShoppingListItemEntity();
        item.setShoppingListId(listId);
        item.setCustomIngredientName(name);
        item.setQuantity(request.quantity());
        item.setUnitId(request.unitId());
        item.setBought(false);
        items.save(item);
        touch(list);
        return resolve(List.of(item)).getFirst();
    }

    @Transactional
    public Item update(long listId, long itemId, UpdateShoppingItemRequest request) {
        var list = owned(listId, true);
        var item = item(listId, itemId);
        validateQuantity(request.quantity());
        // A previously used inactive unit can be retained, but cannot be selected anew.
        if (!item.getUnitId().equals(request.unitId())) requireActiveUnit(request.unitId());
        item.setQuantity(request.quantity());
        item.setUnitId(request.unitId());
        touch(list);
        return resolve(List.of(item)).getFirst();
    }

    @Transactional
    public Item setBought(long listId, long itemId, boolean bought) {
        var list = owned(listId, true);
        var item = item(listId, itemId);
        if (item.isBought() != bought) {
            item.setBought(bought);
            touch(list);
        }
        return resolve(List.of(item)).getFirst();
    }

    @Transactional
    public void delete(long listId, long itemId) {
        var list = owned(listId, true);
        var row = item(listId, itemId);
        detachSources(listId, java.util.Set.of(row.getId()));
        items.delete(row);
        touch(list);
    }

    @Transactional
    public void clearBought(long listId) {
        var list = owned(listId, true);
        var boughtIds = items.findByShoppingListIdOrderByIdAsc(listId).stream().filter(ShoppingListItemEntity::isBought)
                .map(ShoppingListItemEntity::getId).collect(Collectors.toSet());
        detachSources(listId, boughtIds);
        if (items.deleteByShoppingListIdAndBoughtTrue(listId) > 0) touch(list);
    }

    private ShoppingListEntity owned(long id, boolean lock) {
        long owner = users.requireActiveMember().id();
        if (id < 1) throw invalid("listId phải > 0.");
        // Same response for missing and foreign resources; never disclose their contents.
        return (lock ? lists.lockOwned(id, owner) : lists.findByIdAndUserId(id, owner))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private ShoppingListItemEntity item(long listId, long itemId) {
        if (itemId < 1) throw invalid("itemId phải > 0.");
        return items.findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private void requireActiveUnit(Integer id) {
        if (id == null || id < 1 || catalog.findActiveUnits().stream().noneMatch(u -> u.id().equals(id))) {
            throw invalid("Chọn đơn vị đang hoạt động trong danh mục.");
        }
    }

    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0 || quantity.stripTrailingZeros().scale() > 2
                || quantity.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw invalid("Số lượng phải > 0, tối đa 99999999,99 và không quá 2 chữ số thập phân.");
        }
    }

    private void touch(ShoppingListEntity list) { list.setUpdatedAt(LocalDateTime.now(clock)); }

    private String normalizeListName(String value) {
        String name = value == null ? "" : value.replace('\u00a0', ' ').strip();
        if (name.isBlank() || name.length() > 200 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid("Tên danh sách không được để trống và tối đa 200 ký tự.");
        }
        return name;
    }

    private void detachSources(long listId, java.util.Set<Long> ids) {
        var rows = contributions.findByShoppingListIdOrderByIdAsc(listId);
        rows.stream().filter(c -> c.getShoppingListItemId()!=null && ids.contains(c.getShoppingListItemId())).forEach(c -> c.setShoppingListItemId(null));
        contributions.saveAllAndFlush(rows); // keep recipe addition markers, even after all its items are removed
    }

    private List<Item> resolve(List<ShoppingListItemEntity> rows) {
        var sourceRows = rows.isEmpty() ? List.<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity>of()
                : contributions.findByShoppingListIdOrderByIdAsc(rows.getFirst().getShoppingListId());
        var sourceRecipes = sourceRows.isEmpty() ? Map.<Long,tech.mamxanh.recipe.service.RecipeService.ShoppingReference>of()
                : recipes.findShoppingReferences(sourceRows.stream().map(c -> c.getRecipeId()).distinct().toList());
        Map<Long, IngredientResponse> ingredients = catalog.findIngredientReferences(rows.stream()
                .map(ShoppingListItemEntity::getIngredientId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(IngredientResponse::id, Function.identity()));
        Map<Integer, UnitResponse> units = catalog.findUnitReferences(java.util.stream.Stream.concat(rows.stream()
                .map(ShoppingListItemEntity::getUnitId), sourceRows.stream().map(c -> c.getUnitId())).distinct().toList())
                .stream().collect(Collectors.toMap(UnitResponse::id, Function.identity()));
        return rows.stream().map(row -> {
            var ingredient = ingredients.get(row.getIngredientId());
            var unit = units.get(row.getUnitId());
            return new Item(row.getId(), row.getIngredientId(),
                    ingredient == null ? row.getCustomIngredientName() : ingredient.name(),
                    ingredient == null ? "Nhóm khác" : ingredient.ingredientGroup(),
                    ingredient == null ? null : ingredient.imageUrl(), row.getQuantity(), row.getUnitId(),
                    unit == null ? null : unit.code(), unit == null ? null : unit.name(), row.isBought(),
                    sourceRows.stream().filter(c -> row.getId().equals(c.getShoppingListItemId())).map(c -> {
                        var recipe = sourceRecipes.get(c.getRecipeId()); var originalUnit = units.get(c.getUnitId());
                        return new ShoppingListResponse.Source(c.getRecipeId(), recipe == null ? null : recipe.title(),
                                recipe != null && recipe.available(), c.getQuantity(), c.getUnitId(),
                                originalUnit == null ? null : originalUnit.code());
                    }).toList());
        }).toList();
    }

    private AppException invalid(String detail) { return new AppException(ErrorCode.VALIDATION_FAILED, detail); }
}
