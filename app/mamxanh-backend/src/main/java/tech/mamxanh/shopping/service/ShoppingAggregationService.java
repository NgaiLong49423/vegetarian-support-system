package tech.mamxanh.shopping.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.service.IngredientCatalogLookupService;
import tech.mamxanh.nutrition.service.IngredientConversionLookupService;
import tech.mamxanh.shopping.dto.request.ShoppingAggregationLine;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Group;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Warning;

/**
 * FR-54. Internal calculation boundary; the FR-53 caller must authenticate, check list ownership
 * and validate recipe references before supplying rows. No persistence, HTTP or external provider.
 * Quantities are already scaled by the caller to the desired recipe servings.
 */
@Service
public class ShoppingAggregationService {
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private final IngredientCatalogLookupService catalog;
    private final IngredientConversionLookupService conversions;

    public ShoppingAggregationService(IngredientCatalogLookupService catalog, IngredientConversionLookupService conversions) {
        this.catalog = catalog;
        this.conversions = conversions;
    }

    @Transactional(readOnly = true)
    public ShoppingAggregationResult aggregate(List<ShoppingAggregationLine> sources) {
        if (sources == null) throw new AppException(ErrorCode.VALIDATION_FAILED, "Thiếu danh sách nguyên liệu nguồn.");
        if (sources.isEmpty()) return new ShoppingAggregationResult(List.of(), List.of());
        var ingredientIds = sources.stream().filter(Objects::nonNull).map(ShoppingAggregationLine::ingredientId)
                .filter(Objects::nonNull).distinct().toList();
        var unitIds = sources.stream().filter(Objects::nonNull).map(ShoppingAggregationLine::unitId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, IngredientResponse> ingredients = index(catalog.findIngredientReferences(ingredientIds), IngredientResponse::id);
        Map<Integer, UnitResponse> units = index(catalog.findUnitReferences(unitIds), UnitResponse::id);
        // Active standard units can be output targets even when not present in the source rows.
        catalog.findActiveUnits().forEach(unit -> units.putIfAbsent(unit.id(), unit));
        Map<RateKey, BigDecimal> rates = new LinkedHashMap<>();
        for (var rate : conversions.findActiveConversions(ingredientIds)) {
            if (rate.active() && rate.gramsPerUnit() != null && rate.gramsPerUnit().signum() > 0) {
                rates.put(new RateKey(rate.ingredientId(), rate.unitId()), rate.gramsPerUnit());
            }
        }
        var warnings = new ArrayList<Warning>();
        Map<Identity, List<Row>> byIngredient = new LinkedHashMap<>();
        for (int i = 0; i < sources.size(); i++) {
            var source = sources.get(i);
            Row row = validate(source, i, ingredients, units, warnings);
            if (row != null) byIngredient.computeIfAbsent(row.identity(), ignored -> new ArrayList<>()).add(row);
        }
        Map<String, List<Item>> groups = new TreeMap<>();
        for (var rows : byIngredient.values()) {
            var combined = combine(rows, units.values(), rates, warnings);
            groups.computeIfAbsent(rows.getFirst().group(), ignored -> new ArrayList<>()).addAll(combined);
        }
        return new ShoppingAggregationResult(groups.entrySet().stream().map(entry -> new Group(entry.getKey(),
                entry.getValue().stream().sorted(Comparator.comparing(Item::name).thenComparing(Item::unitCode)).toList())).toList(), warnings);
    }

    private Row validate(ShoppingAggregationLine source, int index, Map<Long, IngredientResponse> ingredients,
            Map<Integer, UnitResponse> units, List<Warning> warnings) {
        if (source == null || source.quantity() == null || source.quantity().signum() <= 0) {
            warnings.add(new Warning(index, "INVALID_QUANTITY", "Bỏ qua dòng thiếu định lượng hoặc có số lượng không dương."));
            return null;
        }
        UnitResponse unit = units.get(source.unitId());
        if (unit == null) {
            warnings.add(new Warning(index, "UNKNOWN_UNIT", "Bỏ qua dòng không có đơn vị chuẩn hợp lệ."));
            return null;
        }
        if (source.ingredientId() != null) {
            var ingredient = ingredients.get(source.ingredientId());
            if (ingredient == null) {
                warnings.add(new Warning(index, "UNKNOWN_INGREDIENT", "Bỏ qua nguyên liệu không tồn tại trong danh mục."));
                return null;
            }
            return new Row(new Identity(ingredient.id(), null), ingredient.name(), ingredient.ingredientGroup(), unit, source.quantity(), index);
        }
        String name = source.customName() == null ? "" : source.customName().replace('\u00a0', ' ').strip();
        if (name.isBlank() || name.length() > 200 || name.codePoints().anyMatch(c -> c == '<' || c == '>' || Character.isISOControl(c))) {
            warnings.add(new Warning(index, "INVALID_MANUAL_NAME", "Bỏ qua mặt hàng thủ công có tên không hợp lệ."));
            return null;
        }
        String normalizedName = name.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return new Row(new Identity(null, normalizedName), name, "Nhóm khác", unit, source.quantity(), index);
    }

    private List<Item> combine(List<Row> rows, Collection<UnitResponse> units, Map<RateKey, BigDecimal> rates, List<Warning> warnings) {
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        for (var row : rows) {
            String family = row.identity().ingredientId() == null ? "UNIT:" + row.unit().id() : family(row.unit());
            var target = standardTarget(family, row.unit(), units);
            BigDecimal quantity = row.quantity().multiply(factor(row.unit())).divide(factor(target));
            var bucket = buckets.computeIfAbsent(family, ignored -> new Bucket(target));
            bucket.quantity = bucket.quantity.add(quantity);
            bucket.rows.add(row);
        }
        if (rows.getFirst().identity().ingredientId() != null && buckets.size() > 1) {
            mergeWithOfficialRates(buckets, units, rates, warnings);
        }
        boolean incompatible = buckets.size() > 1;
        var first = rows.getFirst();
        return buckets.values().stream().map(bucket -> new Item(first.identity().ingredientId(), first.name(),
                bucket.quantity, bucket.unit.id(), bucket.unit.code(), bucket.unit.name(), incompatible,
                bucket.rows.stream().map(Row::sourceIndex).toList())).toList();
    }

    private void mergeWithOfficialRates(Map<String, Bucket> buckets, Collection<UnitResponse> units,
            Map<RateKey, BigDecimal> rates, List<Warning> warnings) {
        UnitResponse massUnit = units.stream().filter(unit -> family(unit).equals("MASS"))
                .min(Comparator.comparing(this::factor).thenComparing(UnitResponse::id)).orElse(null);
        if (massUnit == null) return;
        List<String> convertible = new ArrayList<>();
        BigDecimal totalGrams = BigDecimal.ZERO;
        for (var entry : buckets.entrySet()) {
            BigDecimal grams = grams(entry.getValue(), rates, units, warnings);
            if (grams != null) { convertible.add(entry.getKey()); totalGrams = totalGrams.add(grams); }
        }
        if (convertible.size() < 2) return;
        var merged = new Bucket(massUnit);
        merged.quantity = totalGrams.divide(factor(massUnit));
        convertible.forEach(key -> { merged.rows.addAll(buckets.remove(key).rows); });
        buckets.put("MASS", merged);
    }

    private BigDecimal grams(Bucket bucket, Map<RateKey, BigDecimal> rates, Collection<UnitResponse> units, List<Warning> warnings) {
        if (family(bucket.unit).equals("MASS")) return bucket.quantity.multiply(factor(bucket.unit));
        BigDecimal gramsPerBase = null;
        var first = bucket.rows.getFirst();
        var candidates = family(bucket.unit).equals("VOLUME")
                ? units.stream().filter(unit -> family(unit).equals("VOLUME")).toList() : List.of(bucket.unit);
        for (var unit : candidates) {
            BigDecimal rate = rates.get(new RateKey(first.identity().ingredientId(), unit.id()));
            if (rate == null) continue;
            BigDecimal candidate = rate.divide(factor(unit));
            if (gramsPerBase != null && gramsPerBase.compareTo(candidate) != 0) {
                warnings.add(new Warning(first.sourceIndex(), "CONFLICTING_CONVERSION", "Giữ riêng dòng vì các quy tắc quy đổi đang mâu thuẫn."));
                return null;
            }
            gramsPerBase = candidate;
        }
        return gramsPerBase == null ? null : bucket.quantity.multiply(factor(bucket.unit)).multiply(gramsPerBase);
    }

    private UnitResponse standardTarget(String family, UnitResponse original, Collection<UnitResponse> units) {
        if (!family.equals("MASS") && !family.equals("VOLUME")) return original;
        return units.stream().filter(unit -> family(unit).equals(family))
                .min(Comparator.comparing(this::factor).thenComparing(UnitResponse::id)).orElse(original);
    }

    /** Only the explicitly approved g/kg and ml/L pairs have automatic within-family conversion. */
    private String family(UnitResponse unit) {
        String code = unit.code().toLowerCase(Locale.ROOT);
        if (unit.dimension() == MeasurementDimension.MASS && (code.equals("g") || code.equals("kg"))) return "MASS";
        if (unit.dimension() == MeasurementDimension.VOLUME && (code.equals("ml") || code.equals("l"))) return "VOLUME";
        return "UNIT:" + unit.id();
    }

    private BigDecimal factor(UnitResponse unit) {
        String code = unit.code().toLowerCase(Locale.ROOT);
        return (family(unit).equals("MASS") && code.equals("kg")) || (family(unit).equals("VOLUME") && code.equals("l"))
                ? THOUSAND : BigDecimal.ONE;
    }

    private static <K, V> Map<K, V> index(List<V> values, Function<V, K> key) {
        return values.stream().collect(Collectors.toMap(key, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
    }
    private record Identity(Long ingredientId, String manualName) { }
    private record RateKey(Long ingredientId, Integer unitId) { }
    private record Row(Identity identity, String name, String group, UnitResponse unit, BigDecimal quantity, int sourceIndex) { }
    private static final class Bucket {
        final UnitResponse unit;
        final List<Row> rows = new ArrayList<>();
        BigDecimal quantity = BigDecimal.ZERO;
        Bucket(UnitResponse unit) { this.unit = unit; }
    }
}
