package tech.mamxanh.nutrition.service;

import org.springframework.stereotype.Component;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/** One server-side evaluator for personal hard constraints, vegetarian rules, and soft dislikes. */
@Component
public class DietaryConstraintEvaluator {
    private static final Map<String, String> ALIASES = aliases();

    public Assessment evaluate(RecipePostResponse recipe, DietaryPreferencesResponse profile,
            Map<Long, IngredientResponse> catalog) {
        return evaluate(recipe, profile.vegetarianType() == null ? null : profile.vegetarianType().name(),
                ids(profile.avoid().items()), exactNames(profile.avoid().items()),
                ids(profile.dislike().items()), groups(profile.dislike().items()), catalog);
    }

    public Assessment evaluate(RecipePostResponse recipe, String vegetarianType,
            List<Long> avoidIds, List<String> avoidNames, List<Long> dislikeIds, List<String> dislikeNames,
            Map<Long, IngredientResponse> catalog) {
        return evaluate(recipe, vegetarianType, new HashSet<>(avoidIds), exactCanonicalNames(avoidNames),
                new HashSet<>(dislikeIds), canonicalNames(dislikeNames), catalog);
    }

    private Assessment evaluate(RecipePostResponse recipe, String vegetarianType,
            Set<Long> avoidIds, Set<String> avoidNames, Set<Long> dislikeIds, Set<String> dislikeNames,
            Map<Long, IngredientResponse> catalog) {
        List<IngredientStatus> lines = new ArrayList<>();
        List<String> unverified = new ArrayList<>();
        for (RecipePostResponse.Ingredient ingredient : recipe.ingredients()) {
            IngredientResponse metadata = ingredient.ingredientId() == null ? null : catalog.get(ingredient.ingredientId());
            List<Conflict> conflicts = new ArrayList<>();
            if (matchesExactly(ingredient, avoidIds, avoidNames)) conflicts.add(Conflict.ALLERGEN_OR_AVOID);
            if (matchesWithAliases(ingredient, dislikeIds, dislikeNames)) conflicts.add(Conflict.DISLIKE);
            if (ingredient.ingredientId() == null || metadata == null) {
                unverified.add(ingredient.name());
                conflicts.add(Conflict.UNVERIFIED);
            }
            lines.add(new IngredientStatus(ingredient.ingredientId(), ingredient.name(),
                    metadata == null ? null : metadata.ingredientGroup(), List.copyOf(conflicts)));
        }
        boolean typeCompatible = recipeTypeCompatible(recipe.vegetarianType(), vegetarianType);
        if (!typeCompatible) {
            lines.add(new IngredientStatus(null, "Loại ăn chay của công thức", null, List.of(Conflict.VEGETARIAN_TYPE)));
        }
        return new Assessment(recipe.id(), recipe.title(), typeCompatible,
                lines.stream().anyMatch(line -> line.conflicts().contains(Conflict.ALLERGEN_OR_AVOID)),
                !typeCompatible,
                lines.stream().anyMatch(line -> line.conflicts().contains(Conflict.DISLIKE)),
                List.copyOf(unverified), List.copyOf(lines), false, null);
    }

    public static boolean recipeTypeCompatible(String recipeType, String preferenceType) {
        if (preferenceType == null) return true;
        if (recipeType == null) return false;
        return switch (preferenceType) {
            case "VEGAN" -> "VEGAN".equals(recipeType);
            case "LACTO" -> "VEGAN".equals(recipeType) || "LACTO".equals(recipeType);
            case "OVO" -> "VEGAN".equals(recipeType) || "OVO".equals(recipeType);
            case "LACTO_OVO" -> Set.of("VEGAN", "LACTO", "OVO", "LACTO_OVO").contains(recipeType);
            default -> false;
        };
    }

    public static int dislikeCount(RecipePostResponse recipe, DietaryPreferencesResponse profile) {
        Set<Long> ids = ids(profile.dislike().items());
        Set<String> names = groups(profile.dislike().items());
        return (int) recipe.ingredients().stream().filter(ingredient -> matchesWithAliases(ingredient, ids, names)).count();
    }

    public static int dislikeCount(RecipePostResponse recipe, List<Long> ids, List<String> names) {
        Set<Long> idSet = new HashSet<>(ids);
        Set<String> nameSet = canonicalNames(names);
        return (int) recipe.ingredients().stream().filter(ingredient -> matchesWithAliases(ingredient, idSet, nameSet)).count();
    }

    public record Assessment(long recipeId, String recipeTitle, boolean recipeTypeCompatible,
            boolean hasAllergenOrAvoidConflict, boolean hasVegetarianConflict, boolean hasDislikedIngredients,
            List<String> unverifiedIngredients, List<IngredientStatus> ingredients,
            boolean requiresRiskAcknowledgement, String assessmentToken) {
        public Assessment withAssessmentToken(String token) {
            return new Assessment(recipeId, recipeTitle, recipeTypeCompatible, hasAllergenOrAvoidConflict,
                    hasVegetarianConflict, hasDislikedIngredients, unverifiedIngredients, ingredients,
                    hasAllergenOrAvoidConflict || hasVegetarianConflict || !unverifiedIngredients.isEmpty(), token);
        }
    }

    public record IngredientStatus(Long ingredientId, String name, String ingredientGroup, List<Conflict> conflicts) { }
    public enum Conflict { ALLERGEN_OR_AVOID, VEGETARIAN_TYPE, DISLIKE, UNVERIFIED }

    private static boolean matchesExactly(RecipePostResponse.Ingredient ingredient, Set<Long> ids, Set<String> names) {
        return (ingredient.ingredientId() != null && ids.contains(ingredient.ingredientId()))
                || names.contains(key(ingredient.name()));
    }

    private static boolean matchesWithAliases(RecipePostResponse.Ingredient ingredient, Set<Long> ids, Set<String> names) {
        if (matchesExactly(ingredient, ids, names)) return true;
        String key = key(ingredient.name());
        String group = ALIASES.get(key);
        return group != null && names.contains("@" + group);
    }

    private static Set<Long> ids(List<DietaryPreferencesResponse.PreferenceItem> items) {
        return items.stream().map(DietaryPreferencesResponse.PreferenceItem::ingredientId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static Set<String> groups(List<DietaryPreferencesResponse.PreferenceItem> items) {
        Set<String> result = new HashSet<>();
        for (var item : items) {
            String value = key(item.name());
            result.add(value);
            String group = ALIASES.get(value);
            if (group != null) result.add("@" + group);
        }
        return result;
    }

    private static Set<String> exactNames(List<DietaryPreferencesResponse.PreferenceItem> items) {
        return items.stream().map(DietaryPreferencesResponse.PreferenceItem::name)
                .map(DietaryConstraintEvaluator::key).collect(Collectors.toSet());
    }

    private static Set<String> exactCanonicalNames(List<String> values) {
        return values.stream().map(DietaryConstraintEvaluator::key).collect(Collectors.toSet());
    }

    private static Set<String> canonicalNames(List<String> values) {
        Set<String> result = new HashSet<>();
        for (String value : values) {
            String normalized = key(value);
            result.add(normalized);
            String group = ALIASES.get(normalized);
            if (group != null) result.add("@" + group);
        }
        return result;
    }

    private static String key(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
        return normalized;
    }

    private static Map<String, String> aliases() {
        Map<String, String> result = new HashMap<>();
        add(result, "SOY", "đậu nành", "đậu tương", "đậu hũ", "đậu phụ", "tàu hũ", "tàu hũ ky",
                "váng đậu", "sữa đậu nành", "nước tương", "tempeh", "miso", "tofu", "soy", "soy milk", "soy sauce");
        add(result, "PEANUT", "đậu phộng", "lạc", "đậu phộng rang", "bơ đậu phộng", "dầu đậu phộng", "peanut", "peanut butter");
        add(result, "DAIRY", "sữa bò", "sữa tươi", "sữa đặc", "bơ sữa", "phô mai", "pho mát",
                "phô mai cheddar", "phô mai cottage", "phô mai cream cheese", "phô mai mozzarella",
                "phô mai parmesan", "phô mai xanh", "kem sữa", "whey", "casein", "milk", "cheese");
        add(result, "EGG", "trứng", "trứng gà", "trứng vịt", "lòng đỏ trứng", "lòng trắng trứng", "egg", "egg yolk", "egg white");
        add(result, "HONEY", "mật ong", "honey");
        add(result, "FISH", "cá", "cá hồi", "cá ngừ", "nước mắm", "fish", "fish sauce");
        add(result, "MEAT", "thịt", "thịt bò", "thịt heo", "thịt gà", "thịt lợn", "ham", "beef", "pork", "chicken");
        return Map.copyOf(result);
    }

    private static void add(Map<String, String> map, String group, String... names) {
        for (String name : names) map.put(key(name), group);
    }
}
