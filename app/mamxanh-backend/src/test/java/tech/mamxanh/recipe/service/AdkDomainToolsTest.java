package tech.mamxanh.recipe.service;

import com.google.adk.tools.FunctionTool;
import com.google.adk.tools.Annotations.Schema;
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
import tech.mamxanh.mealplan.dto.request.AddMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.service.MealPlanReadService;
import tech.mamxanh.mealplan.service.MealPlanService;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.nutrition.service.IngredientCatalogService;
import tech.mamxanh.nutrition.service.NutritionProfileService;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.shopping.service.ShoppingListService;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.ObjectMapper;

import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdkDomainToolsTest {

    @Mock
    private RecipePostService recipePosts;
    @Mock
    private RecipeService recipeService;
    @Mock
    private IngredientCatalogService ingredientCatalog;
    @Mock
    private RecipeReactionService reactions;
    @Mock
    private RecipeCommentService comments;
    @Mock
    private MemberFollowService follows;
    @Mock
    private MemberProfileService profiles;
    @Mock
    private DietaryPreferenceService dietaryPreferences;
    @Mock
    private NutritionProfileService nutritionProfiles;
    @Mock
    private MealPlanReadService mealPlanReads;
    @Mock
    private MealPlanService mealPlans;
    @Mock
    private ShoppingListService shoppingLists;
    @Mock
    private ApprovedAiKnowledgeRetriever knowledge;
    @Mock
    private Validator validator;
    @Mock
    private CurrentUserService users;
    @Mock
    private SubscriptionEntitlementService entitlements;

    private ObjectMapper mapper;
    private AiAssistantToolRegistry registry;
    private AdkDomainTools domainTools;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        registry = new AiAssistantToolRegistry(
                recipePosts, recipeService, ingredientCatalog, reactions, comments,
                follows, profiles, dietaryPreferences, nutritionProfiles, mealPlanReads,
                mealPlans, mock(tech.mamxanh.mealplan.service.AiMenuService.class), shoppingLists, knowledge, validator, mapper, users, entitlements
        );
        domainTools = new AdkDomainTools(registry);
        domainTools.initDeclarations();
    }

    @Test
    @DisplayName("Guest role is scoped to exactly 5 public tools")
    void guestRoleIsScopedToExactly5PublicTools() {
        List<String> names = domainTools.getToolNamesForRole("GUEST");
        assertThat(names).hasSize(5);
        assertThat(names).containsExactly(
                "search_public_recipes",
                "get_recipe_detail",
                "get_ingredient_detail",
                "navigate_to_page",
                "search_approved_knowledge"
        );

        List<FunctionTool> tools = domainTools.createToolsForRole("GUEST");
        assertThat(tools).hasSize(5);
        assertThat(tools.stream().map(FunctionTool::name).toList()).containsExactlyElementsOf(names);

        List<AiAssistantToolRegistry.ToolDefinition> definitions = domainTools.definitionsFor("GUEST");
        assertThat(definitions).hasSize(5);
    }

    @Test
    @DisplayName("Recipe detail navigation declares an optional recipe ID for the ADK model")
    void navigateToPageDeclaresOptionalRecipeId() throws NoSuchMethodException {
        var declaration = domainTools.getDeclaration("navigate_to_page");
        var method = AdkDomainTools.class.getMethod("navigate_to_page", String.class, Long.class);
        var recipeId = method.getParameters()[1].getAnnotation(Schema.class);

        assertThat(declaration).isNotNull();
        assertThat(recipeId).isNotNull();
        assertThat(recipeId.name()).isEqualTo("recipeId");
        assertThat(recipeId.optional()).isTrue();
    }

    @Test
    @DisplayName("Customer role includes ADK candidate selection and AI menu preview tools")
    void customerRoleIsScopedToExactly25Tools() {
        List<String> names = domainTools.getToolNamesForRole("CUSTOMER");
        assertThat(names).hasSize(28);
        assertThat(names).contains(
                "search_public_recipes",
                "get_recipe_detail",
                "get_ingredient_detail",
                "navigate_to_page",
                "search_approved_knowledge",
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
                "get_ai_menu_candidates",
                "create_ai_menu_preview",
                "apply_ai_menu_preview",
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

        // Does NOT contain expert tools
        assertThat(names).doesNotContain("create_recipe_post", "update_own_recipe_post", "delete_own_recipe_post");

        List<FunctionTool> tools = domainTools.createToolsForRole("CUSTOMER");
        assertThat(tools).hasSize(28);

        List<AiAssistantToolRegistry.ToolDefinition> definitions = domainTools.definitionsFor("CUSTOMER");
        assertThat(definitions).hasSize(28);
    }

    @Test
    @DisplayName("Expert role includes customer and expert tools")
    void expertRoleIsScopedToExactly28Tools() {
        List<String> names = domainTools.getToolNamesForRole("EXPERT");
        assertThat(names).hasSize(32);
        assertThat(names).contains(
                "create_recipe_post",
                "update_own_recipe_post",
                "delete_own_recipe_post"
        );

        List<FunctionTool> tools = domainTools.createToolsForRole("EXPERT");
        assertThat(tools).hasSize(32);

        List<AiAssistantToolRegistry.ToolDefinition> definitions = domainTools.definitionsFor("EXPERT");
        assertThat(definitions).hasSize(32);
    }

    @Test
    @DisplayName("add_meal_plan_slot correctly calculates Monday week start and adds entry via MealPlanService")
    void addMealPlanSlotCalculatesWeekStartAndAddsEntry() {
        // Wednesday 2026-11-04 -> Monday is 2026-11-02
        LocalDate mealDate = LocalDate.of(2026, 11, 4);
        LocalDate mondayWeekStart = LocalDate.of(2026, 11, 2);

        MealPlanWeekResponse.Entry mockEntry = new MealPlanWeekResponse.Entry(
                101L, mealDate, "LUNCH", BigDecimal.ONE, 42L, "Canh chua chay", "/img/canhchua.jpg", "SOUP", 30, false, null
        );

        when(mealPlans.addEntry(eq(mondayWeekStart), any(AddMealPlanEntryRequest.class)))
                .thenReturn(mockEntry);

        Map<String, Object> result = domainTools.add_meal_plan_slot("2026-11-04", "LUNCH", 42L);

        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("SUCCEEDED");
        assertThat(result.get("message")).isEqualTo("Đã thêm món vào kế hoạch ăn uống.");
        assertThat(result.get("data")).isNotNull();

        verify(mealPlans).addEntry(eq(mondayWeekStart), eq(new AddMealPlanEntryRequest(mealDate, "LUNCH", 42L)));
    }

    @Test
    @DisplayName("Shopping list tools require SHOPPING_LIST consent category")
    void shoppingListToolsRequireShoppingListConsentCategory() {
        List<AiAssistantToolRegistry.ToolDefinition> definitions = domainTools.definitionsFor("CUSTOMER");

        List<String> shoppingTools = List.of(
                "list_shopping_lists", "get_shopping_list", "create_shopping_list",
                "rename_shopping_list", "add_shopping_item", "set_shopping_item_bought",
                "delete_shopping_item", "delete_shopping_list"
        );

        for (var def : definitions) {
            if (shoppingTools.contains(def.name())) {
                assertThat(def.consentCategories())
                        .as("Tool %s must require SHOPPING_LIST consent", def.name())
                        .contains(AiChatRequest.ConsentCategory.SHOPPING_LIST);
            }
        }
    }

    @Test
    @DisplayName("FR-21 compliance: Expert role does not require PRO entitlement for recipe tools")
    void expertRoleDoesNotRequireProEntitlement() {
        CurrentUser expertMember = new CurrentUser(7L, Role.EXPERT);
        when(users.requireActiveMember()).thenReturn(expertMember);

        // Delete recipe post checks expert role only
        registry.execute("delete_own_recipe_post", Map.of("recipeId", 99L));

        verify(recipePosts).delete(99L);
        // Entitlements.requirePro must NEVER be called
        verify(entitlements, never()).requirePro(any(Long.class));
    }
}
