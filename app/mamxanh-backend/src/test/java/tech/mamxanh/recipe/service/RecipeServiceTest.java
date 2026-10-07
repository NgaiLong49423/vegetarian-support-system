package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.dto.request.CreateRecipeRequest;
import tech.mamxanh.recipe.dto.request.RecipeIngredientInput;
import tech.mamxanh.recipe.dto.request.RecipeMediaInput;
import tech.mamxanh.recipe.entity.RecipeAuthorReferenceEntity;
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
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.recipe.repository.RecipeMediaRepository;
import tech.mamxanh.recipe.repository.RecipeStatisticsRepository;
import tech.mamxanh.nutrition.service.RecipeNutritionService;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {
    @Mock private RecipePostRepository recipeRepository;
    @Mock private RecipeIngredientRepository ingredientRepository;
    @Mock private RecipeIngredientReferenceRepository ingredientReferenceRepository;
    @Mock private RecipeUnitReferenceRepository unitReferenceRepository;
    @Mock private RecipeConversionRepository conversionRepository;
    @Mock private RecipeAuthorReferenceRepository authorRepository;
    @Mock private Clock clock;
    @Mock private CurrentUserService currentUserService;
    @Mock private RecipeMediaRepository mediaRepository;
    @Mock private RecipeNutritionService nutritionService;
    @Mock private RecipeStatisticsRepository statisticsRepository;
    @InjectMocks private RecipeService service;

    private RecipeAuthorReferenceEntity author;
    private RecipeIngredientReferenceEntity ingredient;
    private RecipeUnitReferenceEntity gram;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        author = new RecipeAuthorReferenceEntity();
        ReflectionTestUtils.setField(author, "id", 7L);
        ReflectionTestUtils.setField(author, "role", "EXPERT");
        ReflectionTestUtils.setField(author, "accountStatus", "ACTIVE");
        ingredient = new RecipeIngredientReferenceEntity();
        ReflectionTestUtils.setField(ingredient, "id", 11L);
        ReflectionTestUtils.setField(ingredient, "name", "Đậu hũ");
        ReflectionTestUtils.setField(ingredient, "nutritionSupported", true);
        gram = new RecipeUnitReferenceEntity();
        ReflectionTestUtils.setField(gram, "id", 1);
        ReflectionTestUtils.setField(gram, "code", "g");
        ReflectionTestUtils.setField(gram, "name", "gram");
        ReflectionTestUtils.setField(gram, "dimension", "MASS");
        ReflectionTestUtils.setField(gram, "active", true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingUnauthenticatedAnonymousAndUnparseablePrincipals() {
        assertUnauthenticated();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("7", "ignored"));
        assertUnauthenticated();
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.AnonymousAuthenticationToken("key", "anonymous",
                        List.of(() -> "ROLE_ANONYMOUS")));
        assertUnauthenticated();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("not-a-number", null, List.of()));
        assertThatThrownBy(() -> service.publish(validRequest())).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
        verify(authorRepository, never()).findById(any());
    }

    @Test
    void detailReadsCurrentPublicRecipeAndProjectsAuthorNutritionAndSeparateInteractionCounts() {
        RecipePostEntity recipe = new RecipePostEntity();
        recipe.setId(44L);
        recipe.setAuthorId(7L);
        recipe.setTitle("Đậu hũ kho");
        recipe.setInstructions("Kho đến khi thấm gia vị");
        recipe.setDishCategory("BRAISED");
        recipe.setVegetarianType("VEGAN");
        recipe.setDifficulty("EASY");
        recipe.setServings(2);
        recipe.setPrepTimeMinutes(5);
        recipe.setCookTimeMinutes(15);
        recipe.setStatus("PUBLISHED");
        when(recipeRepository.findByIdAndStatus(44L, "PUBLISHED")).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findAllByRecipeIdOrderByIdAsc(44L)).thenReturn(List.of());
        when(currentUserService.getPublicProfile(7L)).thenReturn(
                new CurrentUserService.PublicProfile(7L, "Bếp xanh", "/public/avatar.png"));
        when(nutritionService.calculate(List.of(), 2)).thenReturn(
                new RecipeNutritionService.NutritionSummary(true, List.of(), java.util.Map.of(), java.util.Map.of()));
        var projection = org.mockito.Mockito.mock(RecipeStatisticsRepository.StatisticsProjection.class);
        when(projection.getLikes()).thenReturn(3L);
        when(projection.getDislikes()).thenReturn(1L);
        when(projection.getReactionCount()).thenReturn(4L);
        when(projection.getViewCount()).thenReturn(21L);
        when(statisticsRepository.findStatistics(44L)).thenReturn(Optional.of(projection));

        var result = service.getPublishedRecipe(44L);

        assertThat(result.author().displayName()).isEqualTo("Bếp xanh");
        assertThat(result.statistics().likes()).isEqualTo(3);
        assertThat(result.statistics().viewCount()).isEqualTo(21);
        assertThat(result.statistics().likePercentage()).isEqualByComparingTo("75.00");
        assertThat(result.nutrition().complete()).isTrue();
        assertThat(result.instructions()).isEqualTo("Kho đến khi thấm gia vị");
    }

    @Test
    void rejectsUnknownInactiveAndNonExpertAccounts() {
        authenticate("7");
        when(authorRepository.findById(7L)).thenReturn(Optional.empty());
        assertUnauthenticated();

        when(authorRepository.findById(7L)).thenReturn(Optional.of(author));
        ReflectionTestUtils.setField(author, "accountStatus", "LOCKED");
        assertAccessDenied();
        ReflectionTestUtils.setField(author, "accountStatus", "ACTIVE");
        ReflectionTestUtils.setField(author, "role", "CUSTOMER");
        assertAccessDenied();
    }

    @Test
    void trimsOptionalValuesAndPersistsAValidRecipeAtTheInjectedTime() {
        prepareCatalog();
        LocalDateTime now = LocalDateTime.of(2026, 10, 6, 8, 30);
        when(clock.instant()).thenReturn(now.toInstant(ZoneOffset.UTC));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(recipeRepository.save(any(RecipePostEntity.class))).thenAnswer(invocation -> {
            RecipePostEntity saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        authenticate("7");

        var result = service.publish(request("  Đậu hũ kho  ", "  Mô tả  ", "  Kho thật ngon nhé  ",
                "https://youtu.be/abcdef", List.of(), 10, 15));

        assertThat(result.recipeId()).isEqualTo(99L);
        assertThat(result.title()).isEqualTo("Đậu hũ kho");
        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(result.publishedAt()).isEqualTo(now);
        verify(ingredientRepository).saveAll(any());
    }

    @Test
    void collectsAllTextAndTimeValidationErrorsAfterCheckingCatalog() {
        prepareCatalog();
        authenticate("7");
        var request = new CreateRecipeRequest("  ", "x".repeat(2001), "short", null, null, null,
                2, 0, 0, null, List.of(new RecipeIngredientInput(11L, 1, BigDecimal.ONE)), null);

        assertThatThrownBy(() -> service.publish(request)).isInstanceOf(RecipeValidationException.class)
                .satisfies(error -> assertThat(((RecipeValidationException) error).errors())
                        .extracting(RecipeValidationException.FieldError::field)
                        .contains("title", "instructions", "description", "prepTimeMinutes"));
    }

    @Test
    void rejectsOverlongTitleAndInstructions() {
        prepareCatalog();
        authenticate("7");
        assertValidationError(request("t".repeat(121), "", "valid instruction", null, List.of(), 1, 1), "title");
        assertValidationError(request("Valid title", "", "i".repeat(5001), null, List.of(), 1, 1), "instructions");
    }

    @Test
    void validatesYoutubeSchemeHostPathAndWatchQuery() {
        prepareCatalog();
        authenticate("7");
        assertValidationError(withYoutube("http://youtu.be/abcdef"), "youtubeUrl");
        assertValidationError(withYoutube("https://m.youtube.com/watch?v=abcdef"), "youtubeUrl");
        assertValidationError(withYoutube("https:///watch?v=abcdef"), "youtubeUrl");
        assertValidationError(withYoutube("https://www.youtube.com/watch"), "youtubeUrl");
        assertValidationError(withYoutube("https://youtube.com/watch?x=1&v="), "youtubeUrl");
        assertValidationError(withYoutube("https://youtube.com/watch?x=1&v=abcdef"), null);
        assertValidationError(withYoutube("https://www.youtube.com/embed/abcdef"), null);
        assertValidationError(withYoutube("https://www.youtube.com/shorts/abcdef"), null);
        assertValidationError(withYoutube("https://youtube.com/watch?x=1&v=video"), null);
        assertValidationError(withYoutube("not a URI"), "youtubeUrl");
        assertValidationError(withYoutube("  "), null);
    }

    @Test
    void validatesMediaCountCoverMimeTypeAndUploadedReference() {
        prepareCatalog();
        authenticate("7");
        assertValidationError(withMedia(List.of()), null);
        assertValidationError(withMedia(List.of(new RecipeMediaInput("blob", "image/jpeg", false))), "media");
        assertValidationError(withMedia(List.of(new RecipeMediaInput("blob", "image/gif", true))), "media[0].mimeType");
        assertValidationError(withMedia(List.of(new RecipeMediaInput(" ", "image/png", true))), "media[0].blobUrl");
        assertValidationError(withMedia(List.of(new RecipeMediaInput(null, null, true))), "media[0].mimeType");
        var tooMany = java.util.stream.IntStream.range(0, 6)
                .mapToObj(i -> new RecipeMediaInput("blob-" + i, "image/webp", i == 0)).toList();
        assertValidationError(withMedia(tooMany), "media");
    }

    @Test
    void reportsUnavailableIngredientUnitAndMissingUnitConversion() {
        authenticate("7");
        when(authorRepository.findById(7L)).thenReturn(Optional.of(author));
        when(ingredientReferenceRepository.findAllByIdInAndStatus(any(), org.mockito.ArgumentMatchers.eq("ACTIVE")))
                .thenReturn(List.of());
        when(unitReferenceRepository.findAllById(List.of(1))).thenReturn(List.of());
        assertValidationError(validRequest(), "ingredients[0].ingredientId");

        when(ingredientReferenceRepository.findAllByIdInAndStatus(any(), org.mockito.ArgumentMatchers.eq("ACTIVE")))
                .thenReturn(List.of(ingredient));
        when(unitReferenceRepository.findAllById(List.of(1))).thenReturn(List.of());
        assertValidationError(validRequest(), "ingredients[0].unitId");

        var countingUnit = new RecipeUnitReferenceEntity();
        ReflectionTestUtils.setField(countingUnit, "id", 2);
        ReflectionTestUtils.setField(countingUnit, "active", true);
        ReflectionTestUtils.setField(countingUnit, "dimension", "COUNT");
        when(ingredientReferenceRepository.findAllByIdInAndStatus(any(), org.mockito.ArgumentMatchers.eq("ACTIVE")))
                .thenReturn(List.of(ingredient));
        when(unitReferenceRepository.findAllById(List.of(2))).thenReturn(List.of(countingUnit));
        when(conversionRepository.existsByIngredientIdAndUnitIdAndActiveTrue(11L, 2)).thenReturn(false);
        assertValidationError(requestWithUnit(2), "ingredients[0].unitId");
    }

    @Test
    void allowsCountingUnitWhenItsIngredientHasAnActiveConversion() {
        authenticate("7");
        when(authorRepository.findById(7L)).thenReturn(Optional.of(author));
        when(ingredientReferenceRepository.findAllByIdInAndStatus(any(), org.mockito.ArgumentMatchers.eq("ACTIVE")))
                .thenReturn(List.of(ingredient));
        var countingUnit = new RecipeUnitReferenceEntity();
        ReflectionTestUtils.setField(countingUnit, "id", 2);
        ReflectionTestUtils.setField(countingUnit, "active", true);
        ReflectionTestUtils.setField(countingUnit, "dimension", "COUNT");
        when(unitReferenceRepository.findAllById(List.of(2))).thenReturn(List.of(countingUnit));
        when(conversionRepository.existsByIngredientIdAndUnitIdAndActiveTrue(11L, 2)).thenReturn(true);
        when(recipeRepository.save(any(RecipePostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-06T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        assertThat(service.publish(requestWithUnit(2)).status()).isEqualTo("PUBLISHED");
    }

    @Test
    void formOptionsAndIngredientLookupFilterInactiveUnitsAndNormalizeQuery() {
        var inactive = new RecipeUnitReferenceEntity();
        ReflectionTestUtils.setField(inactive, "active", false);
        when(unitReferenceRepository.findAllByOrderByNameAsc()).thenReturn(List.of(gram, inactive));

        var options = service.formOptions();
        assertThat(options.dishCategories()).isNotEmpty();
        assertThat(options.vegetarianTypes()).isNotEmpty();
        assertThat(options.difficulties()).isNotEmpty();
        assertThat(options.units()).hasSize(1);

        when(ingredientReferenceRepository.findTop30ByStatusAndNameContainingIgnoreCaseOrderByNameAsc("ACTIVE", ""))
                .thenReturn(List.of(ingredient));
        assertThat(service.findIngredients(null)).hasSize(1);
        verify(ingredientReferenceRepository).findTop30ByStatusAndNameContainingIgnoreCaseOrderByNameAsc("ACTIVE", "");
        when(ingredientReferenceRepository.findTop30ByStatusAndNameContainingIgnoreCaseOrderByNameAsc("ACTIVE", "Đậu hũ"))
                .thenReturn(List.of(ingredient));
        assertThat(service.findIngredients("  Đậu hũ  ")).hasSize(1);
    }

    @Test
    void getPublishedRecipeRejectsUnavailablePostAndReportsNutritionAvailability() {
        when(recipeRepository.findByIdAndStatus(7L, "PUBLISHED")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPublishedRecipe(7L)).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        RecipePostEntity recipe = new RecipePostEntity();
        recipe.setId(7L);
        recipe.setAuthorId(7L);
        recipe.setTitle("Canh");
        recipe.setInstructions("Hướng dẫn");
        recipe.setDishCategory("SOUP");
        recipe.setVegetarianType("VEGAN");
        recipe.setDifficulty("EASY");
        recipe.setServings(2);
        recipe.setPrepTimeMinutes(1);
        recipe.setCookTimeMinutes(2);
        when(currentUserService.getPublicProfile(7L)).thenReturn(
                new CurrentUserService.PublicProfile(7L, "Chủ bếp", null));

        RecipeIngredientEntity supportedLine = ingredientLine(10L, 1);
        RecipeIngredientEntity unsupportedLine = ingredientLine(11L, 2);
        RecipeIngredientReferenceEntity supported = new RecipeIngredientReferenceEntity();
        ReflectionTestUtils.setField(supported, "id", 10L);
        ReflectionTestUtils.setField(supported, "name", "Nấm");
        ReflectionTestUtils.setField(supported, "nutritionSupported", true);
        RecipeIngredientReferenceEntity unsupported = new RecipeIngredientReferenceEntity();
        ReflectionTestUtils.setField(unsupported, "id", 11L);
        ReflectionTestUtils.setField(unsupported, "name", "Lá");
        ReflectionTestUtils.setField(unsupported, "nutritionSupported", false);
        RecipeUnitReferenceEntity pinch = new RecipeUnitReferenceEntity();
        ReflectionTestUtils.setField(pinch, "id", 2);
        ReflectionTestUtils.setField(pinch, "code", "pinch");
        ReflectionTestUtils.setField(pinch, "name", "nhúm");
        ReflectionTestUtils.setField(pinch, "dimension", "MASS");
        ReflectionTestUtils.setField(pinch, "active", true);

        when(recipeRepository.findByIdAndStatus(7L, "PUBLISHED")).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findAllByRecipeIdOrderByIdAsc(7L)).thenReturn(List.of(supportedLine, unsupportedLine));
        when(ingredientReferenceRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(supported, unsupported));
        when(unitReferenceRepository.findAllById(List.of(1, 2))).thenReturn(List.of(gram, pinch));
        when(nutritionService.calculate(any(), org.mockito.ArgumentMatchers.eq(2))).thenReturn(
                new RecipeNutritionService.NutritionSummary(false, List.of("Lá"), java.util.Map.of(), java.util.Map.of()));
        var statistics = org.mockito.Mockito.mock(RecipeStatisticsRepository.StatisticsProjection.class);
        when(statistics.getLikes()).thenReturn(0L);
        when(statistics.getDislikes()).thenReturn(0L);
        when(statistics.getReactionCount()).thenReturn(0L);
        when(statistics.getViewCount()).thenReturn(0L);
        when(statisticsRepository.findStatistics(7L)).thenReturn(Optional.of(statistics));

        var response = service.getPublishedRecipe(7L);

        assertThat(response.nutritionComplete()).isFalse();
        assertThat(response.ingredientsWithoutNutrition()).containsExactly("Lá");
        assertThat(response.ingredients()).hasSize(2);
    }

    private void prepareCatalog() {
        authenticate("7");
        when(authorRepository.findById(7L)).thenReturn(Optional.of(author));
        when(ingredientReferenceRepository.findAllByIdInAndStatus(any(), org.mockito.ArgumentMatchers.eq("ACTIVE")))
                .thenReturn(List.of(ingredient));
        when(unitReferenceRepository.findAllById(any())).thenReturn(List.of(gram));
    }

    private void assertUnauthenticated() {
        assertThatThrownBy(() -> service.publish(validRequest())).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    private void assertAccessDenied() {
        assertThatThrownBy(() -> service.publish(validRequest())).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    private void assertValidationError(CreateRecipeRequest request, String field) {
        if (field == null) {
            when(recipeRepository.save(any(RecipePostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(clock.instant()).thenReturn(Instant.parse("2026-10-06T00:00:00Z"));
            when(clock.getZone()).thenReturn(ZoneOffset.UTC);
            assertThat(service.publish(request)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.publish(request)).isInstanceOf(RecipeValidationException.class)
                    .satisfies(error -> assertThat(((RecipeValidationException) error).errors())
                            .extracting(RecipeValidationException.FieldError::field).contains(field));
        }
    }

    private CreateRecipeRequest validRequest() {
        return request("Đậu hũ kho", "", "Kho đậu hũ thật ngon", null, List.of(), 10, 10);
    }

    private CreateRecipeRequest request(String title, String description, String instructions, String youtube,
            List<RecipeMediaInput> media, int prep, int cook) {
        return new CreateRecipeRequest(title, description, instructions, tech.mamxanh.recipe.entity.RecipeCodes.DishCategory.BRAISED,
                tech.mamxanh.recipe.entity.RecipeCodes.VegetarianType.VEGAN,
                tech.mamxanh.recipe.entity.RecipeCodes.Difficulty.EASY, 2, prep, cook, youtube,
                List.of(new RecipeIngredientInput(11L, 1, BigDecimal.ONE)), media);
    }

    private CreateRecipeRequest withYoutube(String youtube) {
        return request("Đậu hũ kho", "", "Kho đậu hũ thật ngon", youtube, List.of(), 10, 10);
    }

    private CreateRecipeRequest withMedia(List<RecipeMediaInput> media) {
        return request("Đậu hũ kho", "", "Kho đậu hũ thật ngon", null, media, 10, 10);
    }

    private CreateRecipeRequest requestWithUnit(int unitId) {
        return new CreateRecipeRequest("Đậu hũ kho", "", "Kho đậu hũ thật ngon",
                tech.mamxanh.recipe.entity.RecipeCodes.DishCategory.BRAISED,
                tech.mamxanh.recipe.entity.RecipeCodes.VegetarianType.VEGAN,
                tech.mamxanh.recipe.entity.RecipeCodes.Difficulty.EASY, 2, 10, 10, null,
                List.of(new RecipeIngredientInput(11L, unitId, BigDecimal.ONE)), List.of());
    }

    private static RecipeIngredientEntity ingredientLine(long ingredientId, int unitId) {
        RecipeIngredientEntity line = new RecipeIngredientEntity();
        line.setIngredientId(ingredientId);
        line.setUnitId(unitId);
        line.setQuantity(BigDecimal.ONE);
        return line;
    }

    private static void authenticate(String principal) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }
}
