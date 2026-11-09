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
import tech.mamxanh.mealplan.dto.request.AddMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.request.ApplyAiWeeklyMenuRequest;
import tech.mamxanh.mealplan.dto.request.CreateAiMenuPreviewRequest;
import tech.mamxanh.mealplan.dto.request.ApplyAiMenuRequest;
import tech.mamxanh.mealplan.service.AiMenuService;
import tech.mamxanh.mealplan.service.MealPlanReadService;
import tech.mamxanh.mealplan.service.MealPlanService;
import tech.mamxanh.nutrition.dto.request.SaveDietaryPreferencesRequest;
import tech.mamxanh.nutrition.dto.request.SaveNutritionProfileRequest;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.nutrition.service.IngredientCatalogService;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Explicit, role-scoped AI capabilities. Each implementation delegates to an existing domain service. */
@Component
public class AiAssistantToolRegistry {
    private final RecipePostService recipePosts;
    private final RecipeService recipeService;
    private final IngredientCatalogService ingredientCatalog;
    private final RecipeReactionService reactions;
    private final RecipeCommentService comments;
    private final MemberFollowService follows;
    private final MemberProfileService profiles;
    private final DietaryPreferenceService dietaryPreferences;
    private final NutritionProfileService nutritionProfiles;
    private final MealPlanReadService mealPlanReads;
    private final MealPlanService mealPlans;
    private final AiMenuService aiMenus;
    private final ShoppingListService shoppingLists;
    private final ApprovedAiKnowledgeRetriever knowledge;
    private final Validator validator;
    private final ObjectMapper mapper;
    private final CurrentUserService users;
    private final SubscriptionEntitlementService entitlements;

    public AiAssistantToolRegistry(RecipePostService recipePosts,
            RecipeService recipeService, IngredientCatalogService ingredientCatalog,
            RecipeReactionService reactions, RecipeCommentService comments,
            MemberFollowService follows, MemberProfileService profiles, DietaryPreferenceService dietaryPreferences,
            NutritionProfileService nutritionProfiles, MealPlanReadService mealPlanReads, MealPlanService mealPlans, AiMenuService aiMenus,
            ShoppingListService shoppingLists, ApprovedAiKnowledgeRetriever knowledge, Validator validator,
            ObjectMapper mapper, CurrentUserService users, SubscriptionEntitlementService entitlements) {
        this.recipePosts = recipePosts;
        this.recipeService = recipeService;
        this.ingredientCatalog = ingredientCatalog;
        this.reactions = reactions;
        this.comments = comments;
        this.follows = follows;
        this.profiles = profiles;
        this.dietaryPreferences = dietaryPreferences;
        this.nutritionProfiles = nutritionProfiles;
        this.mealPlanReads = mealPlanReads;
        this.mealPlans = mealPlans;
        this.aiMenus = aiMenus;
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
                "{\"query\":\"string, max 120 chars\"}", RiskTier.READ_ONLY, Set.of()));
        definitions.add(def("get_recipe_detail", "Read published recipe details including ingredients, steps, and nutrition.",
                "{\"recipeId\":1}", RiskTier.READ_ONLY, Set.of()));
        definitions.add(def("get_ingredient_detail", "Read public vegetarian ingredient details including name, group, and nutritional information.",
                "{\"ingredientId\":1}", RiskTier.READ_ONLY, Set.of()));
        definitions.add(def("navigate_to_page", "Navigate the user to an existing Mâm Xanh page only when they explicitly ask to open, go to, or switch to that page. To open a specific public recipe, use page RECIPE_DETAIL and the recipeId returned by search_public_recipes.",
                "{\"page\":\"HOME|EXPLORE|MEAL_PLAN|SHOPPING_LIST|AI_PLANS|AI_CREDIT_TOP_UP|PROFILE|DIETARY_PREFERENCES|HEALTH_PROFILE|RECIPE_CREATE|EXPERT_DIRECTORY|COMMUNITY|TRANSACTION_HISTORY|RECIPE_DETAIL\",\"recipeId\":\"optional positive integer; required for RECIPE_DETAIL\"}", RiskTier.READ_ONLY, Set.of()));
        definitions.add(def("search_approved_knowledge", "Search only approved Mâm Xanh knowledge and policies; never use the Internet.",
                "{\"query\":\"string\"}", RiskTier.READ_ONLY, Set.of()));
        if (!member) return List.copyOf(definitions);

        definitions.add(def("get_own_profile", "Read the authenticated member's own basic profile.", "{}", RiskTier.READ_ONLY, Set.of(AiChatRequest.ConsentCategory.BASIC_PROFILE)));
        definitions.add(def("update_own_profile", "Update only the authenticated member's display name and bio.", "{\"displayName\":\"string\",\"bio\":\"string|null\"}", RiskTier.LOW_RISK_WRITE, Set.of(AiChatRequest.ConsentCategory.BASIC_PROFILE)));
        definitions.add(def("get_dietary_preferences", "Read the authenticated member's own vegetarian and avoid/dislike preferences.", "{}", RiskTier.READ_ONLY, Set.of(AiChatRequest.ConsentCategory.DIETARY_PREFERENCES)));
        definitions.add(def("update_dietary_preferences", "Update the authenticated member's own dietary preferences. Always requires explicit confirmation.", "SaveDietaryPreferencesRequest", RiskTier.HIGH_RISK_WRITE, Set.of(AiChatRequest.ConsentCategory.DIETARY_PREFERENCES)));
        definitions.add(def("get_health_profile", "Read the authenticated member's own health profile and nutrition eligibility.", "{}", RiskTier.READ_ONLY, Set.of(AiChatRequest.ConsentCategory.HEALTH_PROFILE)));
        definitions.add(def("update_health_profile", "Save the authenticated member's own health profile; in-app health-data consent remains required. Always requires explicit confirmation.", "SaveNutritionProfileRequest", RiskTier.HIGH_RISK_WRITE, Set.of(AiChatRequest.ConsentCategory.HEALTH_PROFILE)));
        definitions.add(def("set_recipe_reaction", "Like, dislike, or clear the authenticated member's reaction to a published recipe.", "{\"recipeId\":1,\"reaction\":\"LIKE|DISLIKE|NONE\"}", RiskTier.LOW_RISK_WRITE, Set.of()));
        definitions.add(def("create_recipe_comment", "Create a comment on a published recipe for the authenticated member.", "{\"recipeId\":1,\"content\":\"text\",\"parentCommentId\":null}", RiskTier.LOW_RISK_WRITE, Set.of()));
        definitions.add(def("follow_expert", "Follow or unfollow an eligible expert account.", "{\"expertId\":1,\"follow\":true}", RiskTier.LOW_RISK_WRITE, Set.of()));
        definitions.add(def("get_meal_plan", "Read the authenticated member's meal plan for one week.", "{\"weekStartDate\":\"YYYY-MM-DD\"}", RiskTier.READ_ONLY, Set.of()));
        definitions.add(def("get_ai_menu_candidates", "Load the authenticated member's eligible nutrition profile, dietary constraints, and trusted public recipe candidates for an AI menu. Call this first; do not invent recipe IDs.", "{\"request\":CreateAiMenuPreviewRequest}", RiskTier.READ_ONLY, Set.of(AiChatRequest.ConsentCategory.HEALTH_PROFILE, AiChatRequest.ConsentCategory.DIETARY_PREFERENCES)));
        definitions.add(def("create_ai_menu_preview", "Create a nutrition-aware preview from recipe IDs ranked by this ADK turn and returned by get_ai_menu_candidates. Ask for missing requirements; this does not write the meal plan.", "{\"request\":CreateAiMenuPreviewRequest,\"rankedRecipeIds\":[1,2]}", RiskTier.READ_ONLY, Set.of(AiChatRequest.ConsentCategory.HEALTH_PROFILE, AiChatRequest.ConsentCategory.DIETARY_PREFERENCES)));
        definitions.add(def("apply_ai_menu_preview", "Apply the previously generated preview in this session after explicit confirmation. Use its previewId; never invent meals.", "{\"previewId\":\"UUID\"}", RiskTier.HIGH_RISK_WRITE, Set.of()));
        definitions.add(def("apply_weekly_menu", "Apply a complete 21-slot weekly menu to the authenticated member's own meal plan. Always requires explicit confirmation.", "ApplyAiWeeklyMenuRequest plus weekStartDate", RiskTier.HIGH_RISK_WRITE, Set.of()));
        definitions.add(def("add_meal_plan_slot", "Add a single recipe entry to the authenticated member's meal plan on a given date and meal type.", "{\"date\":\"YYYY-MM-DD\",\"mealType\":\"BREAKFAST|LUNCH|DINNER\",\"recipeId\":1}", RiskTier.LOW_RISK_WRITE, Set.of()));
        definitions.add(def("list_shopping_lists", "List the authenticated member's own shopping lists.", "{}", RiskTier.READ_ONLY, Set.of(AiChatRequest.ConsentCategory.SHOPPING_LIST)));
        definitions.add(def("get_shopping_list", "Read one shopping list owned by the authenticated member.", "{\"listId\":1}", RiskTier.READ_ONLY, Set.of(AiChatRequest.ConsentCategory.SHOPPING_LIST)));
        definitions.add(def("create_shopping_list", "Create an empty shopping list for the authenticated member.", "{\"name\":\"string\"}", RiskTier.LOW_RISK_WRITE, Set.of(AiChatRequest.ConsentCategory.SHOPPING_LIST)));
        definitions.add(def("rename_shopping_list", "Rename a shopping list owned by the authenticated member.", "{\"listId\":1,\"name\":\"string\"}", RiskTier.LOW_RISK_WRITE, Set.of(AiChatRequest.ConsentCategory.SHOPPING_LIST)));
        definitions.add(def("add_shopping_item", "Add a quantity-backed item to a shopping list owned by the authenticated member.", "{\"listId\":1,\"name\":\"string\",\"quantity\":1,\"unitId\":1,\"ingredientId\":null}", RiskTier.LOW_RISK_WRITE, Set.of(AiChatRequest.ConsentCategory.SHOPPING_LIST)));
        definitions.add(def("set_shopping_item_bought", "Mark an owned shopping item bought or not bought.", "{\"listId\":1,\"itemId\":1,\"bought\":true}", RiskTier.LOW_RISK_WRITE, Set.of(AiChatRequest.ConsentCategory.SHOPPING_LIST)));
        definitions.add(def("delete_shopping_item", "Delete a shopping item from a list owned by the authenticated member. Always requires explicit confirmation.", "{\"listId\":1,\"itemId\":1}", RiskTier.HIGH_RISK_WRITE, Set.of(AiChatRequest.ConsentCategory.SHOPPING_LIST)));
        definitions.add(def("delete_shopping_list", "Permanently delete a shopping list owned by the authenticated member. Always requires explicit confirmation.", "{\"listId\":1}", RiskTier.HIGH_RISK_WRITE, Set.of(AiChatRequest.ConsentCategory.SHOPPING_LIST)));
        if (expert) {
            definitions.add(def("propose_recipe_form_patch", "Apply nothing yourself. Return this tool for explicit user requests to change the current Expert recipe form. Use the latest Form Context as the base. For a request to append or continue description/instructions, send only the exact new text in appendText so the server joins it to the latest field value; for other edits use fields with the complete resulting value. Do not send both for the same field. Include only requested fields and new ingredients, preserve all others, and ask if the intended result is unclear. Enum codes: dishCategory NOODLE_SOUP|STIR_FRY|HOT_POT|BRAISED|SOUP|FRIED|STEAMED|SALAD|ROLL|GRILLED|DESSERT; vegetarianType VEGAN|LACTO|OVO|LACTO_OVO; difficulty EASY|MEDIUM|HARD. For new ingredients use exact catalog names, positive quantities and valid unit codes. Frontend resolves and acknowledges the revision-bound patch.", "{\"fields\":{\"fieldName\":\"complete replacement value\"},\"appendText\":{\"description\":\"exact text to append\",\"instructions\":\"exact text to append\"},\"addIngredients\":[{\"name\":\"string\",\"quantity\":\"string\",\"unitCode\":\"string\"}]}", RiskTier.READ_ONLY, Set.of()));
            definitions.add(def("create_recipe_post", "Prepare to publish the authenticated expert's current recipe form. The server uses the form snapshot, never model-provided recipe data, and requires explicit confirmation.", "{}", RiskTier.HIGH_RISK_WRITE, Set.of()));
            definitions.add(def("update_own_recipe_post", "Update a RecipePost owned by the authenticated expert. Always requires explicit confirmation.", "{\"recipeId\":1,\"recipe\":UpdateRecipePostRequest}", RiskTier.HIGH_RISK_WRITE, Set.of()));
            definitions.add(def("delete_own_recipe_post", "Permanently delete a RecipePost owned by the authenticated expert. Always requires explicit confirmation.", "{\"recipeId\":1}", RiskTier.HIGH_RISK_WRITE, Set.of()));
        }
        return List.copyOf(definitions);
    }

    public ToolResult execute(String tool, Map<String, Object> arguments) {
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        return switch (tool) {
            case "search_public_recipes" -> searchRecipes(text(args, "query"));
            case "get_recipe_detail" -> data("Đã đọc chi tiết công thức.", recipeService.getPublishedRecipe(longValue(args, "recipeId")));
            case "get_ingredient_detail" -> data("Đã đọc chi tiết nguyên liệu.", ingredientCatalog.getPublicIngredient(longValue(args, "ingredientId")));
            case "navigate_to_page" -> navigate(args);
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
            case "get_ai_menu_candidates" -> {
                Object request = args.get("request");
                if (!(request instanceof Map<?, ?> map)) throw new AppException(ErrorCode.VALIDATION_FAILED);
                @SuppressWarnings("unchecked") Map<String, Object> values = (Map<String, Object>) map;
                yield data("Đã tải danh sách công thức phù hợp để ADK chọn; chưa tạo hoặc lưu thực đơn.",
                        aiMenus.getAdkPreviewCandidates(validated(values, CreateAiMenuPreviewRequest.class)));
            }
            case "create_ai_menu_preview" -> {
                Object request = args.get("request");
                if (!(request instanceof Map<?, ?> map)) throw new AppException(ErrorCode.VALIDATION_FAILED);
                @SuppressWarnings("unchecked") Map<String, Object> values = (Map<String, Object>) map;
                Object rawIds = args.get("rankedRecipeIds");
                if (!(rawIds instanceof List<?> ids)) throw new AppException(ErrorCode.VALIDATION_FAILED);
                List<Long> rankedIds = ids.stream().map(value -> {
                    if (!(value instanceof Number number)) throw new AppException(ErrorCode.VALIDATION_FAILED);
                    long id = number.longValue();
                    if (number.doubleValue() != id) throw new AppException(ErrorCode.VALIDATION_FAILED);
                    return id;
                }).toList();
                yield data("Đã tạo bản xem trước thực đơn; chưa lưu vào lịch ăn.",
                        aiMenus.createPreviewFromAdkSelection(validated(values, CreateAiMenuPreviewRequest.class), rankedIds));
            }
            case "apply_ai_menu_preview" -> {
                Object request = args.get("request");
                if (!(request instanceof Map<?, ?> map)) throw new AppException(ErrorCode.VALIDATION_FAILED);
                @SuppressWarnings("unchecked") Map<String, Object> values = (Map<String, Object>) map;
                yield data("Đã áp dụng thực đơn được xác nhận vào lịch ăn.", aiMenus.apply(validated(values, ApplyAiMenuRequest.class)));
            }
            case "apply_weekly_menu" -> applyWeeklyMenu(args);
            case "add_meal_plan_slot" -> addMealPlanSlot(args);
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

    private ToolResult navigate(Map<String, Object> args) {
        String page = text(args, "page");
        Long recipeId = optionalLongValue(args, "recipeId");
        if ("RECIPE_DETAIL".equals(page)) {
            if (recipeId == null) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Cần mã công thức công khai để mở trang chi tiết.");
            }
            recipeService.getPublishedRecipe(recipeId);
            String url = "/cong-thuc/id/" + recipeId;
            return data("Đang mở chi tiết công thức #" + recipeId + " trên Mâm Xanh.", Map.of("workflowUrl", url));
        }
        if (recipeId != null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Chỉ truyền mã công thức khi mở trang chi tiết công thức.");
        }
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

    private ToolResult addMealPlanSlot(Map<String, Object> args) {
        String dateStr = text(args, "date");
        if (dateStr.isEmpty()) {
            dateStr = text(args, "mealDate");
        }
        LocalDate date = LocalDate.parse(dateStr);
        String mealType = text(args, "mealType").toUpperCase(Locale.ROOT);
        long recipeId = longValue(args, "recipeId");
        LocalDate weekStartDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var added = mealPlans.addEntry(weekStartDate, new AddMealPlanEntryRequest(date, mealType, recipeId));
        return data("Đã thêm món vào kế hoạch ăn uống.", added);
    }

    public void validateRecipePublish(Map<String, Object> arguments) {
        requireExpertPro();
        validated(arguments, CreateRecipeRequest.class);
    }

    private void requireExpertPro() {
        var expert = users.requireActiveMember();
        if (!"EXPERT".equals(expert.role().name())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Chỉ chuyên gia mới được quản lý bài công thức.");
        }
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

    private static Long optionalLongValue(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) return null;
        if (value instanceof Number number) {
            try {
                long parsed = new java.math.BigDecimal(number.toString()).longValueExact();
                if (parsed > 0) return parsed;
            } catch (NumberFormatException | ArithmeticException ignored) {
                // Reject fractional, non-finite, out-of-range, or non-positive identifiers below.
            }
        }
        throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã công thức phải là số nguyên dương.");
    }

    private static String text(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value instanceof String string ? string.strip() : "";
    }

    public enum RiskTier {
        READ_ONLY,
        LOW_RISK_WRITE,
        HIGH_RISK_WRITE
    }

    private static ToolDefinition def(String name, String description, String inputSchema,
            RiskTier riskTier, Set<AiChatRequest.ConsentCategory> consent) {
        boolean write = riskTier != RiskTier.READ_ONLY;
        boolean alwaysConfirm = riskTier == RiskTier.HIGH_RISK_WRITE;
        return new ToolDefinition(name, description, inputSchema, write, alwaysConfirm, consent, riskTier);
    }

    private static ToolDefinition def(String name, String description, String inputSchema,
            boolean write, boolean alwaysConfirm, Set<AiChatRequest.ConsentCategory> consent) {
        RiskTier tier = alwaysConfirm ? RiskTier.HIGH_RISK_WRITE : (write ? RiskTier.LOW_RISK_WRITE : RiskTier.READ_ONLY);
        return new ToolDefinition(name, description, inputSchema, write, alwaysConfirm, consent, tier);
    }

    public record ToolDefinition(String name, String description, String inputSchema, boolean write,
            boolean alwaysConfirm, Set<AiChatRequest.ConsentCategory> consentCategories, RiskTier riskTier) {

        public ToolDefinition(String name, String description, String inputSchema, boolean write,
                boolean alwaysConfirm, Set<AiChatRequest.ConsentCategory> consentCategories) {
            this(name, description, inputSchema, write, alwaysConfirm, consentCategories,
                    alwaysConfirm ? RiskTier.HIGH_RISK_WRITE : (write ? RiskTier.LOW_RISK_WRITE : RiskTier.READ_ONLY));
        }

        public boolean isHighRisk() {
            return riskTier == RiskTier.HIGH_RISK_WRITE || alwaysConfirm;
        }
    }

    public record ToolResult(String status, String summary, Object result, List<Source> sources) { }
}
