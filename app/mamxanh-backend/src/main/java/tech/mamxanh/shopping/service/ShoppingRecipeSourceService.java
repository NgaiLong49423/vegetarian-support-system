package tech.mamxanh.shopping.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.shopping.dto.request.ShoppingAggregationLine;
import tech.mamxanh.shopping.dto.request.UpdateShoppingRecipeSourceServingsRequest;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.repository.ShoppingListItemRepository;
import tech.mamxanh.shopping.repository.ShoppingListRepository;
import tech.mamxanh.shopping.repository.ShoppingRecipeContributionRepository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShoppingRecipeSourceService {
    private final CurrentUserService users;
    private final ShoppingListRepository lists;
    private final ShoppingListItemRepository items;
    private final ShoppingRecipeContributionRepository contributions;
    private final ShoppingAggregationService aggregation;
    private final ShoppingListService checklists;
    private final Clock clock;

    public ShoppingRecipeSourceService(CurrentUserService users, ShoppingListRepository lists,
            ShoppingListItemRepository items, ShoppingRecipeContributionRepository contributions,
            ShoppingAggregationService aggregation, ShoppingListService checklists, Clock clock) {
        this.users = users; this.lists = lists; this.items = items; this.contributions = contributions;
        this.aggregation = aggregation; this.checklists = checklists; this.clock = clock;
    }

    @Transactional
    public ShoppingListResponse updateServings(long listId, UUID sourceId, UpdateShoppingRecipeSourceServingsRequest request) {
        var member = users.lockActiveMember();
        var list = lists.lockOwned(listId, member.id()).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (sourceId == null || request == null || request.servings() == null
                || request.servings().compareTo(new BigDecimal("50")) > 0
                || request.servings().multiply(BigDecimal.TEN).stripTrailingZeros().scale() > 0) {
            throw invalid("Khẩu phần phải là bội số 0,1 trong khoảng 0,5 đến 50.");
        }
        var rows = contributions.findByShoppingListIdOrderByIdAsc(listId).stream()
                .filter(row -> "RECIPE".equals(row.getSourceKind()) && sourceId.equals(row.getSourceId())).toList();
        if (rows.isEmpty()) throw new AppException(ErrorCode.NOT_FOUND);
        BigDecimal oldServings = rows.getFirst().getServings();
        if (oldServings == null || oldServings.signum() <= 0) {
            throw invalid("Danh sách này chưa lưu khẩu phần gốc nên không thể tự tính lại.");
        }
        if (request.servings().compareTo(oldServings) == 0) return checklists.get(listId);

        var linkedIds = rows.stream().map(row -> row.getShoppingListItemId()).filter(java.util.Objects::nonNull).distinct().toList();
        var allSources = contributions.findByShoppingListIdOrderByIdAsc(listId);
        var linkedItems = items.findByShoppingListIdOrderByIdAsc(listId).stream()
                .filter(item -> linkedIds.contains(item.getId())).toList();
        var before = linkedItems.stream().collect(java.util.stream.Collectors.toMap(
                item -> item.getId(), item -> quantitiesForItem(item.getId(), allSources), (a,b) -> a));
        BigDecimal ratio = request.servings().divide(oldServings, 12, java.math.RoundingMode.HALF_UP);
        rows.forEach(row -> {
            row.setQuantity(row.getQuantity().multiply(ratio).setScale(6, java.math.RoundingMode.HALF_UP));
            row.setServings(request.servings());
        });
        contributions.saveAll(rows);
        for (var item : linkedItems) {
            var after = quantitiesForItem(item.getId(), allSources);
            var prior = before.get(item.getId());
            if (!prior.containsKey(item.getUnitId()) || !after.containsKey(item.getUnitId()))
                throw invalid("Đổi khẩu phần không thể cập nhật an toàn vì đơn vị dòng mua đã được thay đổi. Hãy chọn lại đơn vị tương thích trước.");
            BigDecimal delta = after.get(item.getUnitId()).subtract(prior.get(item.getUnitId()));
            BigDecimal quantity = item.getQuantity().add(delta).setScale(6, java.math.RoundingMode.HALF_UP);
            if (quantity.signum() <= 0 || quantity.compareTo(new BigDecimal("999999999999.999999")) > 0)
                throw invalid("Định lượng sau khi đổi khẩu phần vượt giới hạn lưu trữ.");
            item.setQuantity(quantity);
            item.setBought(false);
        }
        list.setUpdatedAt(LocalDateTime.now(clock));
        return checklists.get(listId);
    }

    @Transactional
    public ShoppingListResponse updateRecipeServings(long listId, long recipeId,
            UpdateShoppingRecipeSourceServingsRequest request) {
        var member = users.lockActiveMember();
        var list = lists.lockOwned(listId, member.id()).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (recipeId < 1 || request == null || request.servings() == null
                || request.servings().multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw invalid("Khẩu phần phải là bội số 0,5 trong khoảng 0,5 đến 500.");
        }
        var allSources = contributions.findByShoppingListIdOrderByIdAsc(listId);
        var recipeRows = allSources.stream().filter(row -> "RECIPE".equals(row.getSourceKind())
                && java.util.Objects.equals(row.getRecipeId(), recipeId)).toList();
        if (recipeRows.isEmpty()) throw new AppException(ErrorCode.NOT_FOUND);
        Map<UUID, List<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity>> sourceGroups = recipeRows.stream()
                .collect(Collectors.groupingBy(tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getSourceId,
                        LinkedHashMap::new, Collectors.toList()));
        Map<UUID, BigDecimal> oldServings = new LinkedHashMap<>();
        for (var group : sourceGroups.entrySet()) {
            Integer expectedCount = group.getValue().getFirst().getExpectedIngredientCount();
            if (expectedCount == null || group.getValue().size() != expectedCount
                    || group.getValue().stream().anyMatch(row -> row.getShoppingListItemId() == null
                            || !expectedCount.equals(row.getExpectedIngredientCount()))) {
                throw new AppException(ErrorCode.NOT_FOUND);
            }
            var values = group.getValue().stream().map(tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getServings)
                    .distinct().toList();
            if (values.size() != 1 || values.getFirst() == null || values.getFirst().signum() <= 0)
                throw invalid("Một số lần xuất hiện của món chưa có khẩu phần gốc để tính lại an toàn.");
            oldServings.put(group.getKey(), values.getFirst());
        }
        BigDecimal oldTotal = oldServings.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (request.servings().compareTo(new BigDecimal("0.5")) < 0
                || request.servings().compareTo(new BigDecimal("500")) > 0)
            throw invalid("Tổng khẩu phần phải từ 0,5 đến 500 theo bước 0,5.");
        if (request.servings().compareTo(oldTotal) == 0) return checklists.get(listId);

        var linkedIds = recipeRows.stream().map(row -> row.getShoppingListItemId())
                .filter(java.util.Objects::nonNull).distinct().toList();
        var linkedItems = items.findByShoppingListIdOrderByIdAsc(listId).stream()
                .filter(item -> linkedIds.contains(item.getId())).toList();
        var before = linkedItems.stream().collect(Collectors.toMap(
                item -> item.getId(), item -> quantitiesForItem(item.getId(), allSources), (a, b) -> a));
        Map<UUID, BigDecimal> adjustedServings = distributeServings(request.servings(), oldServings);
        var updatedQuantities = new java.util.IdentityHashMap<
                tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity, BigDecimal>();
        var maximumQuantity = new BigDecimal("999999999999.999999");
        for (var entry : sourceGroups.entrySet()) {
            BigDecimal previous = oldServings.get(entry.getKey());
            BigDecimal updated = adjustedServings.get(entry.getKey());
            BigDecimal ratio = updated.divide(previous, 12, RoundingMode.HALF_UP);
            for (var row : entry.getValue()) {
                BigDecimal quantity = row.getQuantity().multiply(ratio).setScale(6, RoundingMode.HALF_UP);
                if (quantity.signum() <= 0 || quantity.compareTo(maximumQuantity) > 0)
                    throw invalid("Định lượng sau khi đổi khẩu phần vượt giới hạn lưu trữ.");
                updatedQuantities.put(row, quantity);
            }
        }
        var updatedItemQuantities = new LinkedHashMap<Long, BigDecimal>();
        for (var item : linkedItems) {
            var after = quantitiesForItem(item.getId(), allSources, updatedQuantities);
            var prior = before.get(item.getId());
            if (!prior.containsKey(item.getUnitId()) || !after.containsKey(item.getUnitId()))
                throw invalid("Đổi khẩu phần không thể cập nhật an toàn vì đơn vị dòng mua đã được thay đổi. Hãy chọn lại đơn vị tương thích trước.");
            BigDecimal delta = after.get(item.getUnitId()).subtract(prior.get(item.getUnitId()));
            BigDecimal quantity = item.getQuantity().add(delta).setScale(6, RoundingMode.HALF_UP);
            if (quantity.signum() <= 0 || quantity.compareTo(maximumQuantity) > 0)
                throw invalid("Định lượng sau khi đổi khẩu phần vượt giới hạn lưu trữ.");
            updatedItemQuantities.put(item.getId(), quantity);
        }
        for (var row : recipeRows) row.setQuantity(updatedQuantities.get(row));
        for (var entry : sourceGroups.entrySet()) {
            BigDecimal updated = adjustedServings.get(entry.getKey());
            entry.getValue().forEach(row -> row.setServings(updated));
        }
        contributions.saveAll(recipeRows);
        for (var item : linkedItems) {
            item.setQuantity(updatedItemQuantities.get(item.getId()));
            item.setBought(false);
        }
        list.setUpdatedAt(LocalDateTime.now(clock));
        return checklists.get(listId);
    }

    @Transactional
    public ShoppingListResponse removeRecipe(long listId, long recipeId) {
        var member = users.lockActiveMember();
        var list = lists.lockOwned(listId, member.id()).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (recipeId < 1) throw new AppException(ErrorCode.NOT_FOUND);
        var allSources = contributions.findByShoppingListIdOrderByIdAsc(listId);
        var recipeRows = allSources.stream().filter(row -> "RECIPE".equals(row.getSourceKind())
                && java.util.Objects.equals(row.getRecipeId(), recipeId)).toList();
        if (recipeRows.isEmpty()) throw new AppException(ErrorCode.NOT_FOUND);

        var sourceGroups = recipeRows.stream().collect(Collectors.groupingBy(
                tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity::getSourceId,
                LinkedHashMap::new, Collectors.toList()));
        for (var group : sourceGroups.values()) {
            Integer expected = group.getFirst().getExpectedIngredientCount();
            if (expected == null || group.size() != expected
                    || group.stream().anyMatch(row -> row.getShoppingListItemId() == null
                            || !expected.equals(row.getExpectedIngredientCount()))) {
                throw new AppException(ErrorCode.NOT_FOUND);
            }
        }

        var affectedItemIds = recipeRows.stream().map(row -> row.getShoppingListItemId())
                .filter(java.util.Objects::nonNull).distinct().toList();
        contributions.deleteAll(recipeRows);
        contributions.flush();
        var remainingSources = contributions.findByShoppingListIdOrderByIdAsc(listId);
        for (Long itemId : affectedItemIds) {
            var item = items.findByIdAndShoppingListId(itemId, listId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
            var linked = remainingSources.stream().filter(row -> itemId.equals(row.getShoppingListItemId())).toList();
            if (linked.isEmpty()) {
                items.delete(item);
                continue;
            }
            var lines = linked.stream().map(row -> new ShoppingAggregationLine(
                    row.getIngredientId(), null, row.getQuantity(), row.getUnitId())).toList();
            var outputs = aggregation.aggregate(lines).groups().stream().flatMap(group -> group.items().stream()).toList();
            if (outputs.size() != 1 || outputs.getFirst().incompatibleUnits()
                    || outputs.getFirst().quantity().signum() <= 0) {
                throw invalid("Không thể cập nhật nguyên liệu an toàn sau khi gỡ công thức.");
            }
            item.setQuantity(outputs.getFirst().quantity());
            item.setUnitId(outputs.getFirst().unitId());
            item.setBought(false);
            items.save(item);
        }
        list.setUpdatedAt(LocalDateTime.now(clock));
        return checklists.get(listId);
    }

    private Map<UUID, BigDecimal> distributeServings(BigDecimal requested,
            Map<UUID, BigDecimal> previous) {
        var entries = previous.entrySet().stream().toList();
        int count = entries.size();
        long targetMicros = requested.movePointRight(6).longValueExact();
        if (targetMicros < count)
            throw invalid("Tổng khẩu phần không đủ để phân bổ lượng dương cho mỗi lần xuất hiện.");
        BigInteger[] weights = entries.stream()
                .map(entry -> BigInteger.valueOf(entry.getValue().movePointRight(6).longValueExact()))
                .toArray(BigInteger[]::new);
        long[] allocated = new long[count];
        BigInteger[] remainders = new BigInteger[count];
        var active = new java.util.ArrayList<Integer>();
        for (int index = 0; index < count; index++) active.add(index);
        long remainingMicros = targetMicros;
        while (!active.isEmpty()) {
            BigInteger totalWeight = active.stream().map(index -> weights[index])
                    .reduce(BigInteger.ZERO, BigInteger::add);
            long availableMicros = remainingMicros;
            Integer belowMinimum = active.stream()
                    .filter(index -> BigInteger.valueOf(availableMicros).multiply(weights[index])
                            .compareTo(totalWeight) < 0)
                    .findFirst().orElse(null);
            if (belowMinimum != null) {
                allocated[belowMinimum] = 1;
                remainingMicros--;
                active.remove(belowMinimum);
                continue;
            }
            long baseTotal = 0;
            for (int index : active) {
                BigInteger numerator = BigInteger.valueOf(remainingMicros).multiply(weights[index]);
                BigInteger[] division = numerator.divideAndRemainder(totalWeight);
                allocated[index] = division[0].longValueExact();
                remainders[index] = division[1];
                baseTotal += allocated[index];
            }
            long remainingUnits = remainingMicros - baseTotal;
            active.sort(java.util.Comparator.<Integer, BigInteger>comparing(index -> remainders[index]).reversed()
                    .thenComparingInt(Integer::intValue));
            for (int index = 0; index < remainingUnits; index++) allocated[active.get((int) index)]++;
            break;
        }
        var result = new LinkedHashMap<UUID, BigDecimal>();
        for (int index = 0; index < count; index++) {
            result.put(entries.get(index).getKey(), BigDecimal.valueOf(allocated[index], 6));
        }
        return result;
    }

    private java.util.Map<Integer, BigDecimal> quantitiesForItem(Long itemId,
            List<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity> sources) {
        return quantitiesForItem(itemId, sources, Map.of());
    }

    private java.util.Map<Integer, BigDecimal> quantitiesForItem(Long itemId,
            List<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity> sources,
            Map<tech.mamxanh.shopping.entity.ShoppingRecipeContributionEntity, BigDecimal> updatedQuantities) {
        var lines = sources.stream().filter(row -> itemId.equals(row.getShoppingListItemId()))
                .map(row -> new ShoppingAggregationLine(row.getIngredientId(), null,
                        updatedQuantities.getOrDefault(row, row.getQuantity()), row.getUnitId())).toList();
        if (lines.isEmpty()) return java.util.Map.of();
        return aggregation.aggregate(lines).groups().stream().flatMap(group -> group.items().stream())
                .collect(java.util.stream.Collectors.toMap(
                        tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item::unitId,
                        tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item::quantity,
                        BigDecimal::add));
    }

    private AppException invalid(String message) { return new AppException(ErrorCode.VALIDATION_FAILED, message); }
}
