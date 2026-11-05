package tech.mamxanh.recipe.service;

import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.FunctionDeclaration;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import tech.mamxanh.recipe.service.AiAssistantToolRegistry.ToolDefinition;
import tech.mamxanh.recipe.service.AiAssistantToolRegistry.ToolResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standardized Google ADK FunctionTools for Mâm Xanh AI Assistant.
 * Provides 28 domain capabilities scoped into 3 tiers:
 * - GUEST: 5 public tools
 * - CUSTOMER: 25 tools (5 public + 20 member tools)
 * - EXPERT: 28 tools (25 customer + 3 expert tools)
 */
@Component
public class AdkDomainTools {

    public static final List<String> GUEST_TOOL_NAMES = List.of(
            "search_public_recipes",
            "get_recipe_detail",
            "get_ingredient_detail",
            "navigate_to_page",
            "search_approved_knowledge"
    );

    public static final List<String> MEMBER_TOOL_NAMES = List.of(
            "get_own_profile",
            "update_own_profile",
            "get_dietary_preferences",
            "update_dietary_preferences",
            "get_health_profile",
            "update_health_profile",
            "set_recipe_reaction",
            "create_recipe_comment",
            "follow_expert",
            "get_meal_plan",
            "apply_weekly_menu",
            "add_meal_plan_slot",
            "list_shopping_lists",
            "get_shopping_list",
            "create_shopping_list",
            "rename_shopping_list",
            "add_shopping_item",
            "set_shopping_item_bought",
            "delete_shopping_item",
            "delete_shopping_list"
    );

    public static final List<String> EXPERT_TOOL_NAMES = List.of(
            "create_recipe_post",
            "update_own_recipe_post",
            "delete_own_recipe_post"
    );

    private final AiAssistantToolRegistry registry;
    private final Map<String, FunctionDeclaration> declarationCache = new ConcurrentHashMap<>();

    public AdkDomainTools(AiAssistantToolRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void initDeclarations() {
        for (String name : getAllToolNames()) {
            try {
                FunctionTool tool = FunctionTool.create(this, name);
                tool.declaration().ifPresent(decl -> declarationCache.put(name, decl));
            } catch (Exception ignored) {
            }
        }
    }

    public List<String> getAllToolNames() {
        var all = new ArrayList<String>(GUEST_TOOL_NAMES);
        all.addAll(MEMBER_TOOL_NAMES);
        all.addAll(EXPERT_TOOL_NAMES);
        return Collections.unmodifiableList(all);
    }

    public List<String> getToolNamesForRole(String role) {
        boolean member = "CUSTOMER".equals(role) || "EXPERT".equals(role);
        boolean expert = "EXPERT".equals(role);
        var tools = new ArrayList<String>(GUEST_TOOL_NAMES);
        if (member) {
            tools.addAll(MEMBER_TOOL_NAMES);
        }
        if (expert) {
            tools.addAll(EXPERT_TOOL_NAMES);
        }
        return Collections.unmodifiableList(tools);
    }

    public List<ToolDefinition> definitionsFor(String role) {
        return registry.definitionsFor(role);
    }

    public FunctionDeclaration getDeclaration(String toolName) {
        return declarationCache.computeIfAbsent(toolName, name -> {
            try {
                return FunctionTool.create(this, name).declaration().orElse(null);
            } catch (Exception e) {
                return null;
            }
        });
    }

    public List<FunctionTool> createToolsForRole(String role) {
        List<String> names = getToolNamesForRole(role);
        List<FunctionTool> tools = new ArrayList<>(names.size());
        for (String name : names) {
            tools.add(FunctionTool.create(this, name));
        }
        return Collections.unmodifiableList(tools);
    }

    public ToolResult execute(String tool, Map<String, Object> arguments) {
        return registry.execute(tool, arguments);
    }

    private Map<String, Object> wrapResult(String tool, Map<String, Object> args) {
        ToolResult result = registry.execute(tool, args);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", result.status());
        map.put("message", result.summary());
        if (result.result() != null) {
            map.put("data", result.result());
        }
        if (result.sources() != null && !result.sources().isEmpty()) {
            map.put("sources", result.sources());
        }
        return map;
    }

    // ==========================================
    // Tier 1: GUEST Tools (5 Public Tools)
    // ==========================================

    @Schema(description = "Search current public Mâm Xanh recipes; returns real recipe IDs and source titles.")
    public Map<String, Object> search_public_recipes(
            @Schema(name = "query", description = "Recipe search text, max 120 chars") String query) {
        return wrapResult("search_public_recipes", Map.of("query", query == null ? "" : query));
    }

    @Schema(description = "Read published recipe details including ingredients, steps, and nutrition.")
    public Map<String, Object> get_recipe_detail(
            @Schema(name = "recipeId", description = "ID of published recipe to read") long recipeId) {
        return wrapResult("get_recipe_detail", Map.of("recipeId", recipeId));
    }

    @Schema(description = "Read public vegetarian ingredient details including name, group, and nutritional information.")
    public Map<String, Object> get_ingredient_detail(
            @Schema(name = "ingredientId", description = "ID of vegetarian ingredient to read") long ingredientId) {
        return wrapResult("get_ingredient_detail", Map.of("ingredientId", ingredientId));
    }

    @Schema(description = "Navigate the user to an existing Mâm Xanh page only when they explicitly ask to open, go to, or switch to that page. To open a specific public recipe, use RECIPE_DETAIL and the recipeId returned by search_public_recipes.")
    public Map<String, Object> navigate_to_page(
            @Schema(name = "page", description = "Allowed page key: HOME|EXPLORE|MEAL_PLAN|SHOPPING_LIST|AI_PLANS|AI_CREDIT_TOP_UP|PROFILE|DIETARY_PREFERENCES|HEALTH_PROFILE|RECIPE_CREATE|EXPERT_DIRECTORY|COMMUNITY|TRANSACTION_HISTORY|RECIPE_DETAIL") String page,
            @Schema(name = "recipeId", description = "Required only for RECIPE_DETAIL; use an ID returned by search_public_recipes", optional = true) Long recipeId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("page", page == null ? "" : page);
        if (recipeId != null) args.put("recipeId", recipeId);
        return wrapResult("navigate_to_page", args);
    }

    @Schema(description = "Search only approved Mâm Xanh knowledge and policies; never use the Internet.")
    public Map<String, Object> search_approved_knowledge(
            @Schema(name = "query", description = "Query text") String query) {
        return wrapResult("search_approved_knowledge", Map.of("query", query == null ? "" : query));
    }

    // ==========================================
    // Tier 2: CUSTOMER Tools (20 Member Tools)
    // ==========================================

    @Schema(description = "Read the authenticated member's own basic profile.")
    public Map<String, Object> get_own_profile() {
        return wrapResult("get_own_profile", Map.of());
    }

    @Schema(description = "Update only the authenticated member's display name and bio.")
    public Map<String, Object> update_own_profile(
            @Schema(name = "displayName", description = "New display name") String displayName,
            @Schema(name = "bio", description = "New biography text", optional = true) String bio) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (displayName != null) args.put("displayName", displayName);
        if (bio != null) args.put("bio", bio);
        return wrapResult("update_own_profile", args);
    }

    @Schema(description = "Read the authenticated member's own vegetarian and avoid/dislike preferences.")
    public Map<String, Object> get_dietary_preferences() {
        return wrapResult("get_dietary_preferences", Map.of());
    }

    @Schema(description = "Update the authenticated member's own dietary preferences.")
    public Map<String, Object> update_dietary_preferences(
            @Schema(name = "vegetarianType", description = "Vegetarian diet type") String vegetarianType,
            @Schema(name = "dietaryRequirements", description = "List of dietary requirement codes", optional = true) List<String> dietaryRequirements,
            @Schema(name = "dislikedIngredients", description = "List of disliked ingredient IDs", optional = true) List<Long> dislikedIngredients,
            @Schema(name = "allergies", description = "List of allergy ingredient IDs", optional = true) List<Long> allergies) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (vegetarianType != null) args.put("vegetarianType", vegetarianType);
        if (dietaryRequirements != null) args.put("dietaryRequirements", dietaryRequirements);
        if (dislikedIngredients != null) args.put("dislikedIngredients", dislikedIngredients);
        if (allergies != null) args.put("allergies", allergies);
        return wrapResult("update_dietary_preferences", args);
    }

    @Schema(description = "Read the authenticated member's own health profile and nutrition eligibility.")
    public Map<String, Object> get_health_profile() {
        return wrapResult("get_health_profile", Map.of());
    }

    @Schema(description = "Save the authenticated member's own health profile; in-app health-data consent remains required.")
    public Map<String, Object> update_health_profile(
            @Schema(name = "age", description = "Age in years") Integer age,
            @Schema(name = "gender", description = "MALE|FEMALE|OTHER") String gender,
            @Schema(name = "heightCm", description = "Height in centimeters") Double heightCm,
            @Schema(name = "weightKg", description = "Weight in kilograms") Double weightKg,
            @Schema(name = "activityLevel", description = "SEDENTARY|LIGHT|MODERATE|VERY_ACTIVE|EXTRA_ACTIVE") String activityLevel,
            @Schema(name = "healthGoal", description = "LOSE_WEIGHT|MAINTAIN|GAIN_WEIGHT|GAIN_MUSCLE") String healthGoal) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (age != null) args.put("age", age);
        if (gender != null) args.put("gender", gender);
        if (heightCm != null) args.put("heightCm", heightCm);
        if (weightKg != null) args.put("weightKg", weightKg);
        if (activityLevel != null) args.put("activityLevel", activityLevel);
        if (healthGoal != null) args.put("healthGoal", healthGoal);
        return wrapResult("update_health_profile", args);
    }

    @Schema(description = "Like, dislike, or clear the authenticated member's reaction to a published recipe.")
    public Map<String, Object> set_recipe_reaction(
            @Schema(name = "recipeId", description = "Recipe ID") long recipeId,
            @Schema(name = "reaction", description = "Reaction type: LIKE|DISLIKE|NONE") String reaction) {
        return wrapResult("set_recipe_reaction", Map.of("recipeId", recipeId, "reaction", reaction == null ? "NONE" : reaction));
    }

    @Schema(description = "Create a comment on a published recipe for the authenticated member.")
    public Map<String, Object> create_recipe_comment(
            @Schema(name = "recipeId", description = "Recipe ID") long recipeId,
            @Schema(name = "content", description = "Comment text") String content,
            @Schema(name = "parentCommentId", description = "Parent comment ID for replies", optional = true) Long parentCommentId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("recipeId", recipeId);
        args.put("content", content == null ? "" : content);
        if (parentCommentId != null) args.put("parentCommentId", parentCommentId);
        return wrapResult("create_recipe_comment", args);
    }

    @Schema(description = "Follow or unfollow an eligible expert account.")
    public Map<String, Object> follow_expert(
            @Schema(name = "expertId", description = "Expert member ID") long expertId,
            @Schema(name = "follow", description = "true to follow, false to unfollow") boolean follow) {
        return wrapResult("follow_expert", Map.of("expertId", expertId, "follow", follow));
    }

    @Schema(description = "Read the authenticated member's meal plan for one week.")
    public Map<String, Object> get_meal_plan(
            @Schema(name = "weekStartDate", description = "Week start date (YYYY-MM-DD), must be Monday") String weekStartDate) {
        return wrapResult("get_meal_plan", Map.of("weekStartDate", weekStartDate == null ? "" : weekStartDate));
    }

    @Schema(description = "Apply a complete 21-slot weekly menu to the authenticated member's own meal plan.")
    public Map<String, Object> apply_weekly_menu(
            @Schema(name = "weekStartDate", description = "Week start date (YYYY-MM-DD), must be Monday") String weekStartDate,
            @Schema(name = "menu", description = "Weekly menu containing 21 slots") Map<String, Object> menu) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("weekStartDate", weekStartDate);
        if (menu != null) args.put("menu", menu);
        return wrapResult("apply_weekly_menu", args);
    }

    @Schema(description = "Add a single recipe entry to the authenticated member's meal plan on a given date and meal type.")
    public Map<String, Object> add_meal_plan_slot(
            @Schema(name = "date", description = "Date for the meal (YYYY-MM-DD)") String date,
            @Schema(name = "mealType", description = "Meal type: BREAKFAST|LUNCH|DINNER") String mealType,
            @Schema(name = "recipeId", description = "Recipe ID to add") long recipeId) {
        return wrapResult("add_meal_plan_slot", Map.of(
                "date", date == null ? "" : date,
                "mealType", mealType == null ? "" : mealType,
                "recipeId", recipeId));
    }

    @Schema(description = "List the authenticated member's own shopping lists.")
    public Map<String, Object> list_shopping_lists() {
        return wrapResult("list_shopping_lists", Map.of());
    }

    @Schema(description = "Read one shopping list owned by the authenticated member.")
    public Map<String, Object> get_shopping_list(
            @Schema(name = "listId", description = "Shopping list ID") long listId) {
        return wrapResult("get_shopping_list", Map.of("listId", listId));
    }

    @Schema(description = "Create an empty shopping list for the authenticated member.")
    public Map<String, Object> create_shopping_list(
            @Schema(name = "name", description = "Name of the shopping list") String name) {
        return wrapResult("create_shopping_list", Map.of("name", name == null ? "" : name));
    }

    @Schema(description = "Rename a shopping list owned by the authenticated member.")
    public Map<String, Object> rename_shopping_list(
            @Schema(name = "listId", description = "Shopping list ID") long listId,
            @Schema(name = "name", description = "New name of the shopping list") String name) {
        return wrapResult("rename_shopping_list", Map.of("listId", listId, "name", name == null ? "" : name));
    }

    @Schema(description = "Add a quantity-backed item to a shopping list owned by the authenticated member.")
    public Map<String, Object> add_shopping_item(
            @Schema(name = "listId", description = "Shopping list ID") long listId,
            @Schema(name = "name", description = "Item name") String name,
            @Schema(name = "quantity", description = "Item quantity") double quantity,
            @Schema(name = "unitId", description = "Unit ID", optional = true) Integer unitId,
            @Schema(name = "ingredientId", description = "Ingredient ID", optional = true) Long ingredientId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("listId", listId);
        args.put("name", name == null ? "" : name);
        args.put("quantity", quantity);
        if (unitId != null) args.put("unitId", unitId);
        if (ingredientId != null) args.put("ingredientId", ingredientId);
        return wrapResult("add_shopping_item", args);
    }

    @Schema(description = "Mark an owned shopping item bought or not bought.")
    public Map<String, Object> set_shopping_item_bought(
            @Schema(name = "listId", description = "Shopping list ID") long listId,
            @Schema(name = "itemId", description = "Shopping item ID") long itemId,
            @Schema(name = "bought", description = "true if bought, false otherwise") boolean bought) {
        return wrapResult("set_shopping_item_bought", Map.of("listId", listId, "itemId", itemId, "bought", bought));
    }

    @Schema(description = "Delete a shopping item from a list owned by the authenticated member.")
    public Map<String, Object> delete_shopping_item(
            @Schema(name = "listId", description = "Shopping list ID") long listId,
            @Schema(name = "itemId", description = "Shopping item ID") long itemId) {
        return wrapResult("delete_shopping_item", Map.of("listId", listId, "itemId", itemId));
    }

    @Schema(description = "Permanently delete a shopping list owned by the authenticated member.")
    public Map<String, Object> delete_shopping_list(
            @Schema(name = "listId", description = "Shopping list ID") long listId) {
        return wrapResult("delete_shopping_list", Map.of("listId", listId));
    }

    // ==========================================
    // Tier 3: EXPERT Tools (3 Expert Tools)
    // ==========================================

    @Schema(description = "Create and publish a RecipePost as the authenticated expert. Always requires explicit confirmation.")
    public Map<String, Object> create_recipe_post(
            @Schema(name = "recipe", description = "Recipe creation payload") Map<String, Object> recipe) {
        return wrapResult("create_recipe_post", recipe == null ? Map.of() : recipe);
    }

    @Schema(description = "Update a RecipePost owned by the authenticated expert.")
    public Map<String, Object> update_own_recipe_post(
            @Schema(name = "recipeId", description = "Recipe ID") long recipeId,
            @Schema(name = "recipe", description = "Recipe update payload") Map<String, Object> recipe) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("recipeId", recipeId);
        if (recipe != null) args.put("recipe", recipe);
        return wrapResult("update_own_recipe_post", args);
    }

    @Schema(description = "Permanently delete a RecipePost owned by the authenticated expert. Always requires explicit confirmation.")
    public Map<String, Object> delete_own_recipe_post(
            @Schema(name = "recipeId", description = "Recipe ID") long recipeId) {
        return wrapResult("delete_own_recipe_post", Map.of("recipeId", recipeId));
    }
}
