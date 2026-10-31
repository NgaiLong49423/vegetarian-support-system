package tech.mamxanh.recipe.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tech.mamxanh.auth.dto.request.UpdateProfileRequest;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.MemberFollowService;
import tech.mamxanh.auth.service.MemberProfileService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.dto.request.ApplyAiWeeklyMenuRequest;
import tech.mamxanh.mealplan.service.MealPlanReadService;
import tech.mamxanh.mealplan.service.MealPlanService;
import tech.mamxanh.nutrition.dto.request.SaveDietaryPreferencesRequest;
import tech.mamxanh.nutrition.dto.request.SaveNutritionProfileRequest;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.nutrition.service.NutritionProfileService;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.CommentCreateRequest;
import tech.mamxanh.recipe.dto.request.CreateRecipeRequest;
import tech.mamxanh.recipe.dto.request.UpdateRecipePostRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse.Source;
import tech.mamxanh.shopping.dto.request.CreateEmptyShoppingListRequest;
import tech.mamxanh.shopping.dto.request.CreateShoppingItemRequest;
import tech.mamxanh.shopping.dto.request.RenameShoppingListRequest;
import tech.mamxanh.shopping.service.ShoppingListService;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Explicit, role-scoped AI capabilities. Each implementation delegates to an existing domain service. */
@Component
public class AiAssistantToolRegistry {
    private final RecipePostService recipePosts;
    private final RecipeService recipeService;
    private final RecipeReactionService reactions;
    private final RecipeCommentService comments;
    private final MemberFollowService follows;
    private final MemberProfileService profiles;
    private final DietaryPreferenceService dietaryPreferences;
    private final NutritionProfileService nutritionProfiles;
    private final MealPlanReadService mealPlanReads;
    private final MealPlanService mealPlans;
    private final ShoppingListService shoppingLists;
    private final ApprovedAiKnowledgeRetriever knowledge;
    private final Validator validator;
    private final ObjectMapper mapper;
    private final CurrentUserService users;
    private final SubscriptionEntitlementService entitlements;

    public AiAssistantToolRegistry(RecipePostService recipePosts,
            RecipeService recipeService, RecipeReactionService reactions, RecipeCommentService comments,
            MemberFollowService follows, MemberProfileService profiles, DietaryPreferenceService dietaryPreferences,
            NutritionProfileService nutritionProfiles, MealPlanReadService mealPlanReads, MealPlanService mealPlans,
            ShoppingListService shoppingLists, ApprovedAiKnowledgeRetriever knowledge, Validator validator,
            ObjectMapper mapper, CurrentUserService users, SubscriptionEntitlementService entitlements) {
        this.recipePosts = recipePosts;
        this.recipeService = recipeService;
        this.reactions = reactions;
        this.comments = comments;
        this.follows = follows;
        this.profiles = profiles;
        this.dietaryPreferences = dietaryPreferences;
        this.nutritionProfiles = nutritionProfiles;
        this.mealPlanReads = mealPlanReads;
        this.mealPlans = mealPlans;
        this.shoppingLists = shoppingLists;
        this.knowledge = knowledge;
        this.validator = validator;
        this.mapper = mapper;
        this.users = users;
        this.entitlements = entitlements;
    }

    public List<ToolDefinition> definitionsFor(String role) {
        boolean member = "CUSTOMER".equals(role) || "EXPERT".equals(role);
        boolean expert = "EXPERT".equals(role);
        var definitions = new java.util.ArrayList<ToolDefinition>();
        definitions.add(def("search_public_recipes", "Search current public Mâm Xanh recipes; returns real recipe IDs and source titles.",
                "{\"query\":\"string, max 120 chars\"}", false, false, Set.of()));
        definitions.add(def("navigate_to_page", "Navigate the user to an existing Mâm Xanh page only when they explicitly ask to open, go to, or switch to that page. Use only one page key from the allowed list.",
                "{\"page\":\"HOME|EXPLORE|MEAL_PLAN|SHOPPING_LIST|AI_PLANS|AI_CREDIT_TOP_UP|PROFILE|DIETARY_PREFERENCES|HEALTH_PROFILE|RECIPE_CREATE|EXPERT_DIRECTORY|COMMUNITY|TRANSACTION_HISTORY\"}", false, false, Set.of()));
        definitions.add(def("search_approved_knowledge", "Search only approved Mâm Xanh knowledge and policies; never use the Internet.",
                "{\"query\":\"string\"}", false, false, Set.of()));
        if (!member) return List.copyOf(definitions);

        definitions.add(def("get_own_profile", "Read the authenticated member's own basic profile.", "{}", false, false, Set.of(AiChatRequest.ConsentCategory.BASIC_PROFILE)));
        definitions.add(def("update_own_profile", "Update only the authenticated member's display name and bio.", "{\"displayName\":\"string\",\"bio\":\"string|null\"}", true, false, Set.of(AiChatRequest.ConsentCategory.BASIC_PROFILE)));
        definitions.add(def("get_dietary_preferences", "Read the authenticated member's own vegetarian and avoid/dislike preferences.", "{}", false, false, Set.of(AiChatRequest.ConsentCategory.DIETARY_PREFERENCES)));
        definitions.add(def("update_dietary_preferences", "Update the authenticated member's own dietary preferences.", "SaveDietaryPreferencesRequest", true, false, Set.of(AiChatRequest.ConsentCategory.DIETARY_PREFERENCES)));
        definitions.add(def("get_health_profile", "Read the authenticated member's own health profile and nutrition eligibility.", "{}", false, false, Set.of(AiChatRequest.ConsentCategory.HEALTH_PROFILE)));
        definitions.add(def("update_health_profile", "Save the authenticated member's own health profile; in-app health-data consent remains required.", "SaveNutritionProfileRequest", true, false, Set.of(AiChatRequest.ConsentCategory.HEALTH_PROFILE)));
        definitions.add(def("set_recipe_reaction", "Like, dislike, or clear the authenticated member's reaction to a published recipe.", "{\"recipeId\":1,\"reaction\":\"LIKE|DISLIKE|NONE\"}", true, false, Set.of()));
        definitions.add(def("create_recipe_comment", "Create a comment on a published recipe for the authenticated member.", "{\"recipeId\":1,\"content\":\"text\",\"parentCommentId\":null}", true, false, Set.of()));
        definitions.add(def("follow_expert", "Follow or unfollow an eligible expert account.", "{\"expertId\":1,\"follow\":true}", true, false, Set.of()));
        definitions.add(def("get_meal_plan", "Read the authenticated member's meal plan for one week.", "{\"weekStartDate\":\"YYYY-MM-DD\"}", false, false, Set.of()));
        definitions.add(def("apply_weekly_menu", "Apply a complete 21-slot weekly menu to the authenticated member's own meal plan.", "ApplyAiWeeklyMenuRequest plus weekStartDate", true, false, Set.of()));
        definitions.add(def("list_shopping_lists", "List the authenticated member's own shopping lists.", "{}", false, false, Set.of()));
        definitions.add(def("get_shopping_list", "Read one shopping list owned by the authenticated member.", "{\"listId\":1}", false, false, Set.of()));
        definitions.add(def("create_shopping_list", "Create an empty shopping list for the authenticated member.", "{\"name\":\"string\"}", true, false, Set.of()));
        definitions.add(def("rename_shopping_list", "Rename a shopping list owned by the authenticated member.", "{\"listId\":1,\"name\":\"string\"}", true, false, Set.of()));
        definitions.add(def("add_shopping_item", "Add a quantity-backed item to a shopping list owned by the authenticated member.", "{\"listId\":1,\"name\":\"string\",\"quantity\":1,\"unitId\":1,\"ingredientId\":null}", true, false, Set.of()));
        definitions.add(def("set_shopping_item_bought", "Mark an owned shopping item bought or not bought.", "{\"listId\":1,\"itemId\":1,\"bought\":true}", true, false, Set.of()));
        definitions.add(def("delete_shopping_item", "Delete a shopping item from a list owned by the authenticated member.", "{\"listId\":1,\"itemId\":1}", true, true, Set.of()));
        definitions.add(def("delete_shopping_list", "Permanently delete a shopping list owned by the authenticated member.", "{\"listId\":1}", true, true, Set.of()));
        if (expert) {
            definitions.add(def("create_recipe_post", "Create and publish a RecipePost as the authenticated expert. Always requires explicit confirmation.", "CreateRecipeRequest", true, true, Set.of()));
            definitions.add(def("update_own_recipe_post", "Update a RecipePost owned by the authenticated expert.", "{\"recipeId\":1,\"recipe\":UpdateRecipePostRequest}", true, false, Set.of()));
            definitions.add(def("delete_own_recipe_post", "Permanently delete a RecipePost owned by the authenticated expert. Always requires explicit confirmation.", "{\"recipeId\":1}", true, true, Set.of()));
        }
        return List.copyOf(definitions);
    }

    public ToolResult execute(String tool, Map<String, Object> arguments) {
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        return switch (tool) {
            case "search_public_recipes" -> searchRecipes(text(args, "query"));
            case "navigate_to_page" -> navigate(text(args, "page"));
            case "search_approved_knowledge" -> searchKnowledge(text(args, "query"));
            case "get_own_profile" -> data("Đã đọc hồ sơ tài khoản của bạn.", profiles.getOwnProfile());
            case "update_own_profile" -> data("Đã cập nhật hồ sơ tài khoản của bạn.", profiles.updateOwnProfile(validated(args, UpdateProfileRequest.class)));
            case "get_dietary_preferences" -> data("Đã đọc tùy chọn ăn uống của bạn.", dietaryPreferences.getOwnPreferences());
            case "update_dietary_preferences" -> data("Đã cập nhật tùy chọn ăn uống của bạn.", dietaryPreferences.saveOwnPreferences(validated(args, SaveDietaryPreferencesRequest.class)));
            case "get_health_profile" -> data("Đã đọc hồ sơ dinh dưỡng của bạn.", nutritionProfiles.getOwnProfile());
            case "update_health_profile" -> data("Đã lưu hồ sơ dinh dưỡng của bạn.", nutritionProfiles.saveOwnProfile(validated(args, SaveNutritionProfileRequest.class)));
            case "set_recipe_reaction" -> setReaction(args);
            case "create_recipe_comment" -> createComment(args);
            case "follow_expert" -> followExpert(args);
            case "get_meal_plan" -> data("Đã đọc thực đơn tuần.", mealPlanReads.getWeek(LocalDate.parse(text(args, "weekStartDate"))));
            case "apply_weekly_menu" -> applyWeeklyMenu(args);
            case "list_shopping_lists" -> data("Đã đọc danh sách mua sắm của bạn.", shoppingLists.list(0, 20));
            case "get_shopping_list" -> data("Đã đọc danh sách mua sắm.", shoppingLists.get(longValue(args, "listId")));
            case "create_shopping_list" -> data("Đã tạo danh sách mua sắm.", shoppingLists.createEmpty(validated(args, CreateEmptyShoppingListRequest.class)));
            case "rename_shopping_list" -> renameShoppingList(args);
            case "add_shopping_item" -> addShoppingItem(args);
            case "set_shopping_item_bought" -> setShoppingItemBought(args);
            case "delete_shopping_item" -> deleteShoppingItem(args);
            case "delete_shopping_list" -> deleteShoppingList(args);
            case "create_recipe_post" -> { requireExpertPro(); yield data("Đã công khai bài công thức.", recipeService.publish(validated(args, CreateRecipeRequest.class))); }
            case "update_own_recipe_post" -> { requireExpertPro(); yield updateRecipePost(args); }
            case "delete_own_recipe_post" -> { requireExpertPro(); yield deleteRecipePost(args); }
            default -> throw new AppException(ErrorCode.AI_RESPONSE_INVALID, "AI Assistant đã yêu cầu công cụ không được đăng ký.");
        };
    }

    private ToolResult searchRecipes(String query) {
        var page = recipePosts.searchPublished(query, 0, 6);
        List<Source> sources = page.items().stream().map(recipe -> new Source("MAMXANH_RECIPE",
                Long.toString(recipe.id()), recipe.title(), recipe.description(), null)).toList();
        return new ToolResult("SUCCEEDED", page.items().isEmpty() ? "Không tìm thấy công thức phù hợp trong danh mục đã truy vấn."
                : "Đã tra cứu công thức công khai hiện có trên Mâm Xanh.", page.items(), sources);
    }

    private ToolResult searchKnowledge(String query) {
        var matches = knowledge.retrieve(query);
        return new ToolResult("SUCCEEDED", matches.isEmpty()
                ? "Không tìm thấy bằng chứng phù hợp trong corpus nội bộ đã duyệt."
                : "Đã tìm trong tài liệu nội bộ được duyệt.", matches, matches);
    }

    private ToolResult navigate(String page) {
        String url = switch (page) {
            case "HOME" -> "/trang-chu";
            case "EXPLORE" -> "/kham-pha";
            case "MEAL_PLAN" -> "/ke-hoach?view=for-you";
            case "SHOPPING_LIST" -> "/di-cho";
            case "AI_PLANS" -> "/goi-ai";
            case "AI_CREDIT_TOP_UP" -> "/nap-ai-credit";
            case "PROFILE" -> "/ho-so";
            case "DIETARY_PREFERENCES" -> "/ho-so/so-thich-an-uong";
            case "HEALTH_PROFILE" -> "/ho-so/dinh-duong";
            case "RECIPE_CREATE" -> "/dang-cong-thuc";
            case "EXPERT_DIRECTORY" -> "/chuyen-gia";
            case "COMMUNITY" -> "/cong-dong";
            case "TRANSACTION_HISTORY" -> "/giao-dich";
            default -> throw new AppException(ErrorCode.AI_RESPONSE_INVALID, "AI yêu cầu trang không nằm trong danh sách cho phép.");
        };
        return data("Đang mở trang " + page + " trên Mâm Xanh.", Map.of("workflowUrl", url));
    }

    private ToolResult setReaction(Map<String, Object> args) {
        long recipeId = longValue(args, "recipeId");
        String reaction = text(args, "reaction");
        return data("Đã cập nhật tương tác công thức.", "NONE".equals(reaction) ? reactions.remove(recipeId)
                : reactions.set(recipeId, tech.mamxanh.recipe.entity.RecipeReactionType.valueOf(reaction)));
    }

    private ToolResult createComment(Map<String, Object> args) {
        long recipeId = longValue(args, "recipeId");
        var request = validated(args, CommentCreateRequest.class);
        return data("Đã tạo bình luận.", comments.create(recipeId, request));
    }

    private ToolResult followExpert(Map<String, Object> args) {
        long expertId = longValue(args, "expertId");
        boolean follow = Boolean.TRUE.equals(args.get("follow"));
        return data(follow ? "Đã theo dõi chuyên gia." : "Đã bỏ theo dõi chuyên gia.",
                follow ? follows.follow(expertId) : follows.unfollow(expertId));
    }

    private ToolResult applyWeeklyMenu(Map<String, Object> args) {
        LocalDate weekStartDate = LocalDate.parse(text(args, "weekStartDate"));
        @SuppressWarnings("unchecked") Map<String, Object> menu = (Map<String, Object>) args.get("menu");
        return data("Đã áp dụng thực đơn tuần.", mealPlans.applyAiMenu(weekStartDate, validated(menu, ApplyAiWeeklyMenuRequest.class)));
    }

    private ToolResult renameShoppingList(Map<String, Object> args) {
        long listId = longValue(args, "listId");
        return data("Đã đổi tên danh sách mua sắm.", shoppingLists.rename(listId,
                validated(Map.of("name", text(args, "name")), RenameShoppingListRequest.class)));
    }

    private ToolResult addShoppingItem(Map<String, Object> args) {
        long listId = longValue(args, "listId");
        return data("Đã thêm mặt hàng vào danh sách mua sắm.", shoppingLists.add(listId, validated(args, CreateShoppingItemRequest.class)));
    }

    private ToolResult setShoppingItemBought(Map<String, Object> args) {
        long listId = longValue(args, "listId");
        long itemId = longValue(args, "itemId");
        boolean bought = Boolean.TRUE.equals(args.get("bought"));
        return data("Đã cập nhật trạng thái mặt hàng.", shoppingLists.setBought(listId, itemId, bought));
    }

    private ToolResult deleteShoppingItem(Map<String, Object> args) {
        shoppingLists.delete(longValue(args, "listId"), longValue(args, "itemId"));
        return data("Đã xóa mặt hàng khỏi danh sách mua sắm.", Map.of("deleted", true));
    }

    private ToolResult deleteShoppingList(Map<String, Object> args) {
        shoppingLists.deleteList(longValue(args, "listId"));
        return data("Đã xóa danh sách mua sắm.", Map.of("deleted", true));
    }

    private ToolResult updateRecipePost(Map<String, Object> args) {
        long recipeId = longValue(args, "recipeId");
        @SuppressWarnings("unchecked") Map<String, Object> recipe = (Map<String, Object>) args.get("recipe");
        return data("Đã cập nhật bài công thức của bạn.", recipePosts.update(recipeId, validated(recipe, UpdateRecipePostRequest.class)));
    }

    private ToolResult deleteRecipePost(Map<String, Object> args) {
        long recipeId = longValue(args, "recipeId");
        recipePosts.delete(recipeId);
        return data("Đã xóa bài công thức của bạn.", Map.of("deleted", true, "recipeId", recipeId));
    }

    private void requireExpertPro() {
        var expert = users.requireActiveMember();
        if (!"EXPERT".equals(expert.role().name())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Chỉ chuyên gia mới được quản lý bài công thức.");
        }
        entitlements.requirePro(expert.id());
    }

    private ToolResult data(String summary, Object value) {
        return new ToolResult("SUCCEEDED", summary,
                mapper.convertValue(value, new TypeReference<Map<String, Object>>() { }), List.of());
    }

    private <T> T validated(Map<String, Object> values, Class<T> type) {
        T value = mapper.convertValue(values == null ? Map.of() : values, type);
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) throw new AppException(ErrorCode.VALIDATION_FAILED,
                violations.iterator().next().getMessage());
        return value;
    }

    private static long longValue(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value instanceof Number number && number.longValue() > 0) return number.longValue();
        throw new AppException(ErrorCode.VALIDATION_FAILED, "Thiếu mã tài nguyên hợp lệ.");
    }

    private static String text(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value instanceof String string ? string.strip() : "";
    }

    private static ToolDefinition def(String name, String description, String inputSchema,
            boolean write, boolean alwaysConfirm, Set<AiChatRequest.ConsentCategory> consent) {
        return new ToolDefinition(name, description, inputSchema, write, alwaysConfirm, consent);
    }

    public record ToolDefinition(String name, String description, String inputSchema, boolean write,
            boolean alwaysConfirm, Set<AiChatRequest.ConsentCategory> consentCategories) { }

    public record ToolResult(String status, String summary, Object result, List<Source> sources) { }
}
