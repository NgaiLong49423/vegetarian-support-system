package tech.mamxanh.recipe.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.CurrentUserService.CurrentUser;
import tech.mamxanh.auth.service.MemberFollowService;
import tech.mamxanh.auth.service.MemberProfileService;
import tech.mamxanh.auth.dto.request.UpdateProfileRequest;
import tech.mamxanh.auth.dto.response.MemberProfileResponse;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.mealplan.dto.request.AddMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.request.ApplyAiWeeklyMenuRequest;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.service.MealPlanReadService;
import tech.mamxanh.mealplan.service.MealPlanService;
import tech.mamxanh.nutrition.dto.request.SaveDietaryPreferencesRequest;
import tech.mamxanh.nutrition.dto.request.SaveNutritionProfileRequest;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse;
import tech.mamxanh.nutrition.dto.response.PublicIngredientDetailResponse;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.nutrition.service.IngredientCatalogService;
import tech.mamxanh.nutrition.service.NutritionProfileService;
import tech.mamxanh.recipe.dto.request.CommentCreateRequest;
import tech.mamxanh.recipe.dto.request.CreateRecipeRequest;
import tech.mamxanh.recipe.dto.request.UpdateRecipePostRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.dto.response.CreateRecipeResponse;
import tech.mamxanh.recipe.dto.response.CommentResponse;
import tech.mamxanh.recipe.dto.response.RecipeDetailResponse;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.shopping.dto.request.CreateEmptyShoppingListRequest;
import tech.mamxanh.shopping.dto.request.CreateShoppingItemRequest;
import tech.mamxanh.shopping.dto.request.RenameShoppingListRequest;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.service.ShoppingListService;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tier 2 Integration Tests covering all 28 Domain Tools in AiAssistantToolRegistry.
 * Verifies contract, input mapping, status handling, and domain service delegation.
 */
@ExtendWith(MockitoExtension.class)
class AiAssistantToolIntegrationTest {

    @Mock private RecipePostService recipePosts;
    @Mock private RecipeService recipeService;
    @Mock private IngredientCatalogService ingredientCatalog;
    @Mock private RecipeReactionService reactions;
    @Mock private RecipeCommentService comments;
    @Mock private MemberFollowService follows;
    @Mock private MemberProfileService profiles;
    @Mock private DietaryPreferenceService dietaryPreferences;
    @Mock private NutritionProfileService nutritionProfiles;
    @Mock private MealPlanReadService mealPlanReads;
    @Mock private MealPlanService mealPlans;
    @Mock private ShoppingListService shoppingLists;
    @Mock private ApprovedAiKnowledgeRetriever knowledge;
    @Mock private CurrentUserService users;
    @Mock private SubscriptionEntitlementService entitlements;

    private Validator validator;
    private ObjectMapper mapper;
    private AiAssistantToolRegistry registry;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        mapper = new ObjectMapper();
        registry = new AiAssistantToolRegistry(
                recipePosts, recipeService, ingredientCatalog, reactions, comments,
                follows, profiles, dietaryPreferences, nutritionProfiles, mealPlanReads,
                mealPlans, shoppingLists, knowledge, validator, mapper, users, entitlements
        );
    }

    // --- Cụm 1: Tri thức & Q&A (Tool 1..5) ---

    @Test
    @DisplayName("Tool 1: search_public_recipes executes and returns recipe candidates")
    void tool1_searchPublicRecipes() {
        when(recipePosts.searchPublished("dau hu", 0, 6)).thenReturn(
                new RecipePostService.RecipePageResponse(List.of(
                        new RecipePostResponse(1L, 10L, "Chef", null, "Đậu hũ sốt cà", null, null, null, null, null, 1, 1, 1, null, "PUBLISHED", List.of(), List.of(), 0, 0, null, 0)
                ), 0, 6, 1, 1)
        );
        var result = registry.execute("search_public_recipes", Map.of("query", "dau hu"));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.sources()).isNotEmpty();
    }

    @Test
    @DisplayName("Tool 2: get_recipe_detail retrieves published recipe details")
    void tool2_getRecipeDetail() {
        RecipeDetailResponse mockDetail = new RecipeDetailResponse(
                42L, "Đậu hũ chiên sả", "Mô tả", "Hướng dẫn", "MAIN", "Món chính",
                "VEGAN", "Thuần chay", "EASY", "Dễ", 2, 10, 20, null,
                LocalDateTime.now(), null, List.of(), true, List.of(), null, null, List.of()
        );
        when(recipeService.getPublishedRecipe(42L)).thenReturn(mockDetail);
        var result = registry.execute("get_recipe_detail", Map.of("recipeId", 42L));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.result()).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("Tool 3: get_ingredient_detail reads public vegetarian ingredient")
    void tool3_getIngredientDetail() {
        PublicIngredientDetailResponse mockIng = new PublicIngredientDetailResponse(
                5L, "Nấm đùi gà", "Nấm", null, null
        );
        when(ingredientCatalog.getPublicIngredient(5L)).thenReturn(mockIng);
        var result = registry.execute("get_ingredient_detail", Map.of("ingredientId", 5L));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.result()).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("Tool 4: navigate_to_page maps allowed target routes")
    void tool4_navigateToPage() {
        var result = registry.execute("navigate_to_page", Map.of("page", "EXPLORE"));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.result()).isEqualTo(Map.of("workflowUrl", "/kham-pha"));

        assertThatThrownBy(() -> registry.execute("navigate_to_page", Map.of("page", "HACK_PAGE")))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("Tool 4 opens a published recipe detail using its real search result ID")
    void tool4_navigateToPublishedRecipeDetail() {
        when(recipeService.getPublishedRecipe(56L)).thenReturn(mock(RecipeDetailResponse.class));

        var result = registry.execute("navigate_to_page", Map.of("page", "RECIPE_DETAIL", "recipeId", 56L));

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.result()).isEqualTo(Map.of("workflowUrl", "/cong-thuc/id/56"));
        verify(recipeService).getPublishedRecipe(56L);
    }

    @Test
    @DisplayName("Tool 4 requires a positive integer public recipe ID for recipe detail navigation")
    void tool4_rejectsInvalidRecipeDetailId() {
        assertThatThrownBy(() -> registry.execute("navigate_to_page", Map.of("page", "RECIPE_DETAIL")))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> registry.execute("navigate_to_page", Map.of("page", "RECIPE_DETAIL", "recipeId", 0L)))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> registry.execute("navigate_to_page", Map.of("page", "EXPLORE", "recipeId", 56L)))
                .isInstanceOf(AppException.class);
        verify(recipeService, never()).getPublishedRecipe(56L);
    }

    @Test
    @DisplayName("Tool 5: search_approved_knowledge searches policy corpus")
    void tool5_searchApprovedKnowledge() {
        when(knowledge.retrieve("chinh sach")).thenReturn(List.of(
                new AiChatResponse.Source("MAMXANH_KB", "scope-policy", "Scope Policy", "Summary", null)
        ));
        var result = registry.execute("search_approved_knowledge", Map.of("query", "chinh sach"));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.sources()).isNotEmpty();
    }

    // --- Cụm 2: Hồ sơ người dùng & Dinh dưỡng (Tool 6..11) ---

    @Test
    @DisplayName("Tool 6 & 7: get_own_profile and update_own_profile")
    void tool6_7_ownProfile() {
        MemberProfileResponse profile = new MemberProfileResponse(7L, "Nguyễn Văn A", null, "Bio", "2026-09");
        when(profiles.getOwnProfile()).thenReturn(profile);
        when(profiles.updateOwnProfile(any(UpdateProfileRequest.class))).thenReturn(profile);

        var getRes = registry.execute("get_own_profile", Map.of());
        assertThat(getRes.status()).isEqualTo("SUCCEEDED");

        var updateRes = registry.execute("update_own_profile", Map.of("displayName", "Nguyễn Văn A", "bio", "Bio"));
        assertThat(updateRes.status()).isEqualTo("SUCCEEDED");
    }

    @Test
    @DisplayName("Tool 8 & 9: get_dietary_preferences and update_dietary_preferences")
    void tool8_9_dietaryPreferences() {
        DietaryPreferencesResponse pref = new DietaryPreferencesResponse(null, null, null, null, null, null, null, null);
        when(dietaryPreferences.getOwnPreferences()).thenReturn(pref);
        when(dietaryPreferences.saveOwnPreferences(any(SaveDietaryPreferencesRequest.class))).thenReturn(pref);

        var getRes = registry.execute("get_dietary_preferences", Map.of());
        assertThat(getRes.status()).isEqualTo("SUCCEEDED");

        var updateRes = registry.execute("update_dietary_preferences", Map.of(
                "vegetarianType", "VEGAN",
                "avoid", Map.of("noneConfirmed", true, "items", List.of()),
                "dislike", Map.of("noneConfirmed", true, "items", List.of())
        ));
        assertThat(updateRes.status()).isEqualTo("SUCCEEDED");
    }

    @Test
    @DisplayName("Tool 10 & 11: get_health_profile and update_health_profile")
    void tool10_11_healthProfile() {
        NutritionProfileResponse nut = new NutritionProfileResponse(true, true, List.of(), null, null);
        when(nutritionProfiles.getOwnProfile()).thenReturn(nut);
        when(nutritionProfiles.saveOwnProfile(any(SaveNutritionProfileRequest.class))).thenReturn(nut);

        var getRes = registry.execute("get_health_profile", Map.of());
        assertThat(getRes.status()).isEqualTo("SUCCEEDED");

        var updateRes = registry.execute("update_health_profile", Map.of(
                "dateOfBirth", "1995-05-20",
                "biologicalSex", "MALE",
                "heightCm", 170.0,
                "weightKg", 65.0,
                "activityLevel", "MODERATE",
                "nutritionGoal", "MAINTAIN",
                "pregnant", false,
                "breastfeeding", false,
                "therapeuticDietRequired", false,
                "consentAccepted", true
        ));
        assertThat(updateRes.status()).isEqualTo("SUCCEEDED");
    }

    // --- Cụm 3: Tương tác cộng đồng & Reaction (Tool 12..14) ---

    @Test
    @DisplayName("Tool 12: set_recipe_reaction updates reaction")
    void tool12_setRecipeReaction() {
        when(reactions.set(eq(10L), eq(tech.mamxanh.recipe.entity.RecipeReactionType.LIKE))).thenReturn(
                new tech.mamxanh.recipe.dto.response.RecipeReactionResponse(10L, tech.mamxanh.recipe.entity.RecipeReactionType.LIKE, 1, 0, 1, java.math.BigDecimal.valueOf(100))
        );
        var result = registry.execute("set_recipe_reaction", Map.of("recipeId", 10L, "reaction", "LIKE"));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        verify(reactions).set(10L, tech.mamxanh.recipe.entity.RecipeReactionType.LIKE);
    }

    @Test
    @DisplayName("Tool 13: create_recipe_comment adds comment to recipe")
    void tool13_createRecipeComment() {
        CommentResponse commentResp = mock(CommentResponse.class);
        when(comments.create(eq(10L), any(CommentCreateRequest.class))).thenReturn(commentResp);

        var result = registry.execute("create_recipe_comment", Map.of("recipeId", 10L, "content", "Ngon tuyệt"));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        verify(comments).create(eq(10L), any(CommentCreateRequest.class));
    }

    @Test
    @DisplayName("Tool 14: follow_expert toggles follow state")
    void tool14_followExpert() {
        var result = registry.execute("follow_expert", Map.of("expertId", 99L, "follow", true));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        verify(follows).follow(99L);
    }

    // --- Cụm 4: Kế hoạch bữa ăn & Thực đơn tuần (Tool 15..17) ---

    @Test
    @DisplayName("Tool 15: get_meal_plan retrieves meal plan week")
    void tool15_getMealPlan() {
        MealPlanWeekResponse week = new MealPlanWeekResponse(LocalDate.of(2026, 11, 2), List.of());
        when(mealPlanReads.getWeek(LocalDate.of(2026, 11, 2))).thenReturn(week);

        var result = registry.execute("get_meal_plan", Map.of("weekStartDate", "2026-11-02"));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
    }

    @Test
    @DisplayName("Tool 16: apply_weekly_menu applies full weekly menu")
    void tool16_applyWeeklyMenu() {
        MealPlanWeekResponse week = new MealPlanWeekResponse(LocalDate.of(2026, 11, 2), List.of());
        when(mealPlans.applyAiMenu(eq(LocalDate.of(2026, 11, 2)), any(ApplyAiWeeklyMenuRequest.class))).thenReturn(week);

        List<Map<String, Object>> slots = new java.util.ArrayList<>();
        for (int day = 0; day < 7; day++) {
            slots.add(Map.of("dayOffset", day, "mealType", "BREAKFAST", "recipeId", 1L));
            slots.add(Map.of("dayOffset", day, "mealType", "LUNCH", "recipeId", 1L));
            slots.add(Map.of("dayOffset", day, "mealType", "DINNER", "recipeId", 1L));
        }

        var result = registry.execute("apply_weekly_menu", Map.of(
                "weekStartDate", "2026-11-02",
                "menu", Map.of("slots", slots)
        ));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        verify(mealPlans).applyAiMenu(eq(LocalDate.of(2026, 11, 2)), any(ApplyAiWeeklyMenuRequest.class));
    }

    @Test
    @DisplayName("Tool 17: add_meal_plan_slot adds entry to specific slot")
    void tool17_addMealPlanSlot() {
        MealPlanWeekResponse.Entry entry = new MealPlanWeekResponse.Entry(1L, LocalDate.of(2026, 11, 4), "LUNCH", BigDecimal.ONE, 10L, "Cơm", null, "MAIN", 20, false, null);
        when(mealPlans.addEntry(eq(LocalDate.of(2026, 11, 2)), any(AddMealPlanEntryRequest.class))).thenReturn(entry);

        var result = registry.execute("add_meal_plan_slot", Map.of(
                "date", "2026-11-04",
                "mealType", "LUNCH",
                "recipeId", 10L
        ));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        verify(mealPlans).addEntry(eq(LocalDate.of(2026, 11, 2)), eq(new AddMealPlanEntryRequest(LocalDate.of(2026, 11, 4), "LUNCH", 10L)));
    }

    // --- Cụm 5: Giỏ đi chợ & Mua sắm (Tool 18..25) ---

    @Test
    @DisplayName("Tool 18..25: Shopping list complete suite")
    void tool18_to_25_shoppingListSuite() {
        ShoppingListResponse.Summary summary = mock(ShoppingListResponse.Summary.class);
        ShoppingListResponse detail = mock(ShoppingListResponse.class);
        ShoppingListResponse.Item item = mock(ShoppingListResponse.Item.class);

        when(shoppingLists.list(0, 20)).thenReturn(new PageResponse<>(List.of(summary), 0, 20, 1, 1));
        when(shoppingLists.get(1L)).thenReturn(detail);
        when(shoppingLists.createEmpty(any(CreateEmptyShoppingListRequest.class))).thenReturn(detail);
        when(shoppingLists.rename(eq(1L), any(RenameShoppingListRequest.class))).thenReturn(summary);
        when(shoppingLists.add(eq(1L), any(CreateShoppingItemRequest.class))).thenReturn(item);
        when(shoppingLists.setBought(1L, 100L, true)).thenReturn(item);

        assertThat(registry.execute("list_shopping_lists", Map.of()).status()).isEqualTo("SUCCEEDED");
        assertThat(registry.execute("get_shopping_list", Map.of("listId", 1L)).status()).isEqualTo("SUCCEEDED");
        assertThat(registry.execute("create_shopping_list", Map.of("name", "Chợ mới")).status()).isEqualTo("SUCCEEDED");
        assertThat(registry.execute("rename_shopping_list", Map.of("listId", 1L, "name", "Tên mới")).status()).isEqualTo("SUCCEEDED");
        assertThat(registry.execute("add_shopping_item", Map.of("listId", 1L, "name", "Đậu hũ", "quantity", 2, "unitId", 1L)).status()).isEqualTo("SUCCEEDED");
        assertThat(registry.execute("set_shopping_item_bought", Map.of("listId", 1L, "itemId", 100L, "bought", true)).status()).isEqualTo("SUCCEEDED");
        assertThat(registry.execute("delete_shopping_item", Map.of("listId", 1L, "itemId", 100L)).status()).isEqualTo("SUCCEEDED");
        assertThat(registry.execute("delete_shopping_list", Map.of("listId", 1L)).status()).isEqualTo("SUCCEEDED");

        verify(shoppingLists).delete(1L, 100L);
        verify(shoppingLists).deleteList(1L);
    }

    // --- Cụm 6: Công thức Chuyên gia (Tool 26..28) ---

    @Test
    @DisplayName("Tool 26..28: Expert authoring tools verify role and execution")
    void tool26_to_28_expertTools() {
        CurrentUser expertUser = new CurrentUser(88L, Role.EXPERT);
        when(users.requireActiveMember()).thenReturn(expertUser);

        CreateRecipeResponse createResp = new CreateRecipeResponse(500L, "Món mới", "DRAFT", LocalDateTime.now());
        when(recipeService.publish(any(CreateRecipeRequest.class))).thenReturn(createResp);
        when(recipePosts.update(eq(500L), any(UpdateRecipePostRequest.class))).thenReturn(null);

        var createRes = registry.execute("create_recipe_post", Map.of(
                "title", "Món mới thơm ngon", "description", "Mô tả công thức mới", "dishCategory", "SOUP",
                "vegetarianType", "VEGAN", "difficulty", "EASY", "prepTimeMinutes", 10,
                "cookTimeMinutes", 20, "servings", 2, "instructions", "Nấu sôi kỹ trong vòng mười phút cho chín đều.",
                "ingredients", List.of(Map.of("ingredientId", 1L, "unitId", 1L, "quantity", 100))
        ));
        assertThat(createRes.status()).isEqualTo("SUCCEEDED");

        var updateRes = registry.execute("update_own_recipe_post", Map.of(
                "recipeId", 500L,
                "recipe", Map.of(
                        "title", "Món đã sửa", "description", "Mô tả sửa", "dishCategory", "SOUP",
                        "vegetarianType", "VEGAN", "difficulty", "EASY", "prepTimeMin", 10,
                        "cookTimeMin", 20, "servings", 2, "instructions", "Nấu sôi nhẹ trong vòng mười phút cho ngấm gia vị.",
                        "ingredients", List.of(Map.of("ingredientId", 1L, "unitId", 1L, "quantity", 120))
                )
        ));
        assertThat(updateRes.status()).isEqualTo("SUCCEEDED");

        var deleteRes = registry.execute("delete_own_recipe_post", Map.of("recipeId", 500L));
        assertThat(deleteRes.status()).isEqualTo("SUCCEEDED");
        verify(recipePosts).delete(500L);
    }

    @Test
    @DisplayName("Unregistered tool name throws AI_RESPONSE_INVALID exception")
    void unregisteredToolThrowsAppException() {
        assertThatThrownBy(() -> registry.execute("unknown_hack_tool", Map.of()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("definitionsFor returns exact tool count per role: Guest=5, Customer=25, Expert=28")
    void roleScopingCountsMatchArchitectureContract() {
        assertThat(registry.definitionsFor("GUEST")).hasSize(5);
        assertThat(registry.definitionsFor("CUSTOMER")).hasSize(25);
        assertThat(registry.definitionsFor("EXPERT")).hasSize(28);
    }
}
