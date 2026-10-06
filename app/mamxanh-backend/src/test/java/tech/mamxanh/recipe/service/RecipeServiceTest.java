package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.dto.request.CreateRecipeRequest;
import tech.mamxanh.recipe.dto.request.RecipeIngredientInput;
import tech.mamxanh.recipe.dto.request.RecipeMediaInput;
import tech.mamxanh.recipe.entity.RecipeAuthorReferenceEntity;
import tech.mamxanh.recipe.entity.RecipeCodes.Difficulty;
import tech.mamxanh.recipe.entity.RecipeCodes.DishCategory;
import tech.mamxanh.recipe.entity.RecipeCodes.VegetarianType;
import tech.mamxanh.recipe.entity.RecipeIngredientEntity;
import tech.mamxanh.recipe.entity.RecipeIngredientReferenceEntity;
import tech.mamxanh.recipe.entity.RecipePostEntity;
import tech.mamxanh.recipe.entity.RecipeUnitReferenceEntity;
import tech.mamxanh.recipe.repository.RecipeAuthorReferenceRepository;
import tech.mamxanh.recipe.repository.RecipeConversionRepository;
import tech.mamxanh.recipe.repository.RecipeIngredientReferenceRepository;
import tech.mamxanh.recipe.repository.RecipeIngredientRepository;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.RecipeUnitReferenceRepository;
import tech.mamxanh.recipe.service.RecipeValidationException.FieldError;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {
    @Mock private RecipePostRepository recipeRepository;
    @Mock private RecipeIngredientRepository ingredientRepository;
    @Mock private RecipeIngredientReferenceRepository ingredientReferenceRepository;
    @Mock private RecipeUnitReferenceRepository unitReferenceRepository;
    @Mock private RecipeConversionRepository conversionRepository;
    @Mock private RecipeAuthorReferenceRepository authorRepository;
    @Mock private Clock clock;
    @InjectMocks private RecipeService service;

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-01T00:00:00Z"));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishRejectsMissingUnauthenticatedAnonymousAndMalformedPrincipals() {
        assertUnauthorized(() -> service.publish(validRequest()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("7", "password"));
        assertUnauthorized(() -> service.publish(validRequest()));
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "guest",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertUnauthorized(() -> service.publish(validRequest()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-an-id", "password"));
        assertUnauthorized(() -> service.publish(validRequest()));
    }

    @Test
    void publishRequiresAnExistingActiveExpertAccount() {
        authenticate("7");
        when(authorRepository.findById(7L)).thenReturn(Optional.empty());
        assertUnauthorized(() -> service.publish(validRequest()));

        RecipeAuthorReferenceEntity inactive = author("LOCKED", "EXPERT");
        when(authorRepository.findById(7L)).thenReturn(Optional.of(inactive));
        assertAccessDenied(() -> service.publish(validRequest()));

        RecipeAuthorReferenceEntity nonExpert = author("ACTIVE", "CUSTOMER");
        when(authorRepository.findById(7L)).thenReturn(Optional.of(nonExpert));
        assertAccessDenied(() -> service.publish(validRequest()));
    }

    @Test
    void publishRejectsInvalidFieldsYoutubeAndMediaWithoutSaving() {
        authenticate("7");
        givenActiveExpert();
        CreateRecipeRequest invalid = request("  ", "x".repeat(2001), "short", 0, 0,
                "http://youtube.com/watch", List.of(validIngredient()), List.of(
                        new RecipeMediaInput(" ", "image/gif", false),
                        new RecipeMediaInput("https://image.test/a", "image/png", false)));
        assertThatThrownBy(() -> service.publish(invalid)).isInstanceOf(RecipeValidationException.class)
                .satisfies(error -> assertThat(((RecipeValidationException) error).errors())
                        .extracting(FieldError::field)
                        .contains("title", "instructions", "description", "prepTimeMinutes", "youtubeUrl",
                                "media", "media[0].mimeType", "media[0].blobUrl"));
        verify(recipeRepository, never()).save(any());
        verify(ingredientRepository, never()).saveAll(anyList());
    }

    @Test
    void publishRejectsYoutubeUrlsWithoutAValidVideoReference() {
        authenticate("7");
        givenActiveExpert();
        RecipeIngredientReferenceEntity mushroom = ingredient(1L, "Nấm", true);
        RecipeUnitReferenceEntity gram = unit(1, "g", "gram", "MASS", true);
        when(ingredientReferenceRepository.findAllByIdInAndStatus(List.of(1L), "ACTIVE"))
                .thenReturn(List.of(mushroom));
        when(unitReferenceRepository.findAllById(List.of(1))).thenReturn(List.of(gram));
        for (String url : List.of("https://example.com/watch?v=abc", "https://youtube.com/channel/x",
                "https://youtube.com/watch", "https://youtube.com/watch?v=", "https://youtu.be/")) {
            assertThatThrownBy(() -> service.publish(request("Soup", "", "Hướng dẫn nấu món ăn ngon", 0, 1,
                    url, List.of(validIngredient()), null))).isInstanceOf(RecipeValidationException.class)
                    .satisfies(error -> assertThat(((RecipeValidationException) error).errors())
                            .extracting(FieldError::field).contains("youtubeUrl"));
        }
    }

    @Test
    void publishAcceptsSupportedYoutubeFormsAndBlankOptionalValues() {
        authenticate("7");
        givenActiveExpert();
        RecipeIngredientReferenceEntity ingredient = ingredient(1L, "Nấm", true);
        RecipeUnitReferenceEntity unit = unit(1, "g", "gram", "MASS", true);
        when(ingredientReferenceRepository.findAllByIdInAndStatus(List.of(1L), "ACTIVE"))
                .thenReturn(List.of(ingredient));
        when(unitReferenceRepository.findAllById(List.of(1))).thenReturn(List.of(unit));
        when(recipeRepository.save(any(RecipePostEntity.class))).thenAnswer(invocation -> {
            RecipePostEntity recipe = invocation.getArgument(0);
            recipe.setId(99L);
            return recipe;
        });
        when(ingredientRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        for (String url : java.util.Arrays.asList(null, "  ", "https://youtu.be/video", "https://youtube.com/embed/video",
                "https://www.youtube.com/shorts/video", "https://youtube.com/watch?x=1&v=video")) {
            var result = service.publish(request(" Soup ", "   ", "  Hướng dẫn nấu món ăn ngon  ", 0, 1,
                    url, List.of(validIngredient()), null));
            assertThat(result.recipeId()).isEqualTo(99L);
            assertThat(result.status()).isEqualTo("PUBLISHED");
        }
        verify(conversionRepository, never()).existsByIngredientIdAndUnitIdAndActiveTrue(1L, 1);
    }

    @Test
    void publishRejectsMissingInactiveAndUnconvertibleIngredientReferences() {
        authenticate("7");
        givenActiveExpert();
        RecipeUnitReferenceEntity inactiveUnit = unit(1, "cup", "cup", "VOLUME", false);
        when(ingredientReferenceRepository.findAllByIdInAndStatus(List.of(1L), "ACTIVE")).thenReturn(List.of());
        when(unitReferenceRepository.findAllById(List.of(1))).thenReturn(List.of(inactiveUnit));
        assertThatThrownBy(() -> service.publish(validRequest())).isInstanceOf(RecipeValidationException.class)
                .satisfies(error -> assertThat(((RecipeValidationException) error).errors())
                        .extracting(FieldError::field).contains("ingredients[0].ingredientId", "ingredients[0].unitId"));

        RecipeIngredientReferenceEntity ingredient = ingredient(1L, "Nấm", true);
        RecipeUnitReferenceEntity unit = unit(1, "cup", "cup", "VOLUME", true);
        when(ingredientReferenceRepository.findAllByIdInAndStatus(List.of(1L), "ACTIVE"))
                .thenReturn(List.of(ingredient));
        when(unitReferenceRepository.findAllById(List.of(1))).thenReturn(List.of(unit));
        when(conversionRepository.existsByIngredientIdAndUnitIdAndActiveTrue(1L, 1)).thenReturn(false);
        assertThatThrownBy(() -> service.publish(validRequest())).isInstanceOf(RecipeValidationException.class)
                .satisfies(error -> assertThat(((RecipeValidationException) error).errors())
                        .extracting(FieldError::field).contains("ingredients[0].unitId"));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void publishPersistsTrimmedRecipeIngredientsAndTimestamp() {
        authenticate("7");
        givenActiveExpert();
        RecipeIngredientReferenceEntity ingredient = ingredient(1L, "Nấm", true);
        RecipeUnitReferenceEntity unit = unit(1, "cup", "cốc", "VOLUME", true);
        when(ingredientReferenceRepository.findAllByIdInAndStatus(List.of(1L), "ACTIVE"))
                .thenReturn(List.of(ingredient));
        when(unitReferenceRepository.findAllById(List.of(1))).thenReturn(List.of(unit));
        when(conversionRepository.existsByIngredientIdAndUnitIdAndActiveTrue(1L, 1)).thenReturn(true);
        when(recipeRepository.save(any(RecipePostEntity.class))).thenAnswer(invocation -> {
            RecipePostEntity recipe = invocation.getArgument(0);
            recipe.setId(99L);
            assertThat(recipe.getTitle()).isEqualTo("Soup");
            assertThat(recipe.getDescription()).isNull();
            assertThat(recipe.getInstructions()).isEqualTo("Hướng dẫn nấu món ăn ngon");
            assertThat(recipe.getYoutubeUrl()).isNull();
            assertThat(recipe.getPublishedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 10, 1, 0, 0));
            return recipe;
        });
        when(ingredientRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.publish(request(" Soup ", "  ", " Hướng dẫn nấu món ăn ngon ", 0, 1,
                " ", List.of(validIngredient()), null));

        assertThat(response.recipeId()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo("PUBLISHED");
        verify(ingredientRepository).saveAll(anyList());
    }

    @Test
    void getPublishedRecipeRejectsUnavailablePostAndReportsNutritionAvailability() {
        when(recipeRepository.findByIdAndStatus(7L, "PUBLISHED")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPublishedRecipe(7L)).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        RecipePostEntity recipe = new RecipePostEntity();
        recipe.setId(7L);
        recipe.setTitle("Canh");
        recipe.setInstructions("Hướng dẫn");
        recipe.setDishCategory("SOUP");
        recipe.setVegetarianType("VEGAN");
        recipe.setDifficulty("EASY");
        recipe.setServings(2);
        recipe.setPrepTimeMinutes(1);
        recipe.setCookTimeMinutes(2);
        RecipeIngredientEntity line = ingredientLine(10L, 1);
        RecipeIngredientEntity unsupportedLine = ingredientLine(11L, 2);
        when(recipeRepository.findByIdAndStatus(7L, "PUBLISHED")).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findAllByRecipeIdOrderByIdAsc(7L)).thenReturn(List.of(line, unsupportedLine));
        RecipeIngredientReferenceEntity supported = ingredient(10L, "Nấm", true);
        RecipeIngredientReferenceEntity unsupported = ingredient(11L, "Lá", false);
        when(ingredientReferenceRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(supported, unsupported));
        RecipeUnitReferenceEntity gram = unit(1, "g", "gram", "MASS", true);
        RecipeUnitReferenceEntity pinch = unit(2, "pinch", "nhúm", "MASS", true);
        when(unitReferenceRepository.findAllById(List.of(1, 2))).thenReturn(List.of(gram, pinch));

        var response = service.getPublishedRecipe(7L);

        assertThat(response.nutritionComplete()).isFalse();
        assertThat(response.ingredientsWithoutNutrition()).containsExactly("Lá");
        assertThat(response.ingredients()).hasSize(2);
    }

    @Test
    void formOptionsOnlyReturnsActiveUnitsAndFindIngredientsNormalizesQuery() {
        RecipeUnitReferenceEntity active = unit(1, "g", "gram", "MASS", true);
        RecipeUnitReferenceEntity inactive = unit(2, "cup", "cốc", "VOLUME", false);
        when(unitReferenceRepository.findAllByOrderByNameAsc()).thenReturn(List.of(active, inactive));
        var options = service.formOptions();
        assertThat(options.dishCategories()).isNotEmpty();
        assertThat(options.vegetarianTypes()).isNotEmpty();
        assertThat(options.difficulties()).isNotEmpty();
        assertThat(options.units()).extracting("unitId").containsExactly(1);

        when(ingredientReferenceRepository.findTop30ByStatusAndNameContainingIgnoreCaseOrderByNameAsc("ACTIVE", ""))
                .thenReturn(List.of());
        assertThat(service.findIngredients(null)).isEmpty();
        RecipeIngredientReferenceEntity mushroom = ingredient(5L, "Nấm", true);
        when(ingredientReferenceRepository.findTop30ByStatusAndNameContainingIgnoreCaseOrderByNameAsc("ACTIVE", "nam"))
                .thenReturn(List.of(mushroom));
        assertThat(service.findIngredients(" nam ")).extracting("name").containsExactly("Nấm");
    }

    private static CreateRecipeRequest validRequest() {
        return request("Soup", "", "Hướng dẫn nấu món ăn ngon", 0, 1, null, List.of(validIngredient()), null);
    }

    private static CreateRecipeRequest request(String title, String description, String instructions, Integer prep,
            Integer cook, String youtube, List<RecipeIngredientInput> ingredients, List<RecipeMediaInput> media) {
        return new CreateRecipeRequest(title, description, instructions, DishCategory.SOUP, VegetarianType.VEGAN,
                Difficulty.EASY, 2, prep, cook, youtube, ingredients, media);
    }

    private static RecipeIngredientInput validIngredient() {
        return new RecipeIngredientInput(1L, 1, new BigDecimal("100.00"));
    }

    private static void authenticate(String principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", AuthorityUtils.NO_AUTHORITIES));
    }

    private void givenActiveExpert() {
        RecipeAuthorReferenceEntity author = author("ACTIVE", "EXPERT");
        when(authorRepository.findById(7L)).thenReturn(Optional.of(author));
    }

    private static RecipeAuthorReferenceEntity author(String status, String role) {
        RecipeAuthorReferenceEntity author = mock(RecipeAuthorReferenceEntity.class);
        lenient().when(author.getId()).thenReturn(7L);
        lenient().when(author.getAccountStatus()).thenReturn(status);
        lenient().when(author.getRole()).thenReturn(role);
        return author;
    }

    private static RecipeIngredientReferenceEntity ingredient(long id, String name, boolean nutritionSupported) {
        RecipeIngredientReferenceEntity ingredient = mock(RecipeIngredientReferenceEntity.class);
        lenient().when(ingredient.getId()).thenReturn(id);
        lenient().when(ingredient.getName()).thenReturn(name);
        lenient().when(ingredient.isNutritionSupported()).thenReturn(nutritionSupported);
        return ingredient;
    }

    private static RecipeUnitReferenceEntity unit(int id, String code, String name, String dimension, boolean active) {
        RecipeUnitReferenceEntity unit = mock(RecipeUnitReferenceEntity.class);
        lenient().when(unit.getId()).thenReturn(id);
        lenient().when(unit.getCode()).thenReturn(code);
        lenient().when(unit.getName()).thenReturn(name);
        lenient().when(unit.getDimension()).thenReturn(dimension);
        lenient().when(unit.isActive()).thenReturn(active);
        return unit;
    }

    private static RecipeIngredientEntity ingredientLine(long ingredientId, int unitId) {
        RecipeIngredientEntity line = new RecipeIngredientEntity();
        line.setIngredientId(ingredientId);
        line.setUnitId(unitId);
        line.setQuantity(new BigDecimal("1.00"));
        return line;
    }

    private static void assertUnauthorized(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    private static void assertAccessDenied(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }
}
