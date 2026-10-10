package tech.mamxanh.recipe.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.recipe.repository.RecipeIngredientRepository;
import tech.mamxanh.recipe.repository.RecipeMediaRepository;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.CurrentUserService.CurrentUser;
import tech.mamxanh.auth.service.CurrentUserService.PublicProfile;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.dto.request.UpdateRecipePostRequest;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.RecipeBrowseRepository;
import tech.mamxanh.recipe.repository.RecipeStatisticsRepository;
import tech.mamxanh.recipe.repository.RecipeIngredientReferenceRepository;
import tech.mamxanh.recipe.repository.RecipeUnitReferenceRepository;
import tech.mamxanh.recipe.entity.RecipeMediaEntity;
import tech.mamxanh.recipe.entity.RecipeIngredientReferenceEntity;
import tech.mamxanh.recipe.entity.RecipeUnitReferenceEntity;
import org.springframework.test.util.ReflectionTestUtils;
import tech.mamxanh.recipe.repository.RecipeValidationRepository;
import tech.mamxanh.recipe.service.RecipePostService;

@ExtendWith(MockitoExtension.class)
class RecipePostServiceTest {
    @Mock private RecipePostRepository repository;
    @Mock private RecipeBrowseRepository browseRepository;
    @Mock private RecipeStatisticsRepository statisticsRepository;
    @Mock private RecipeIngredientReferenceRepository ingredientReferenceRepository;
    @Mock private RecipeUnitReferenceRepository unitReferenceRepository;
    @Mock private RecipeIngredientRepository ingredientRepository;
    @Mock private RecipeMediaRepository mediaRepository;
    @Mock private RecipeValidationRepository validationRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private Clock clock;
    @InjectMocks private RecipePostService service;

    private RecipePostEntity recipe;

    @BeforeEach
    void setUp() {
        recipe = new RecipePostEntity();
        recipe.setId(47L);
        recipe.setAuthorId(7L);
        recipe.setTitle("Món cũ của tôi");
        recipe.setDescription("Mô tả cũ");
        recipe.setInstructions("Cách làm món ăn cũ");
        recipe.setDishCategory("SOUP");
        recipe.setVegetarianType("VEGAN");
        recipe.setDifficulty("EASY");
        recipe.setServings(2);
        recipe.setPrepTimeMinutes(10);
        recipe.setCookTimeMinutes(10);
        recipe.setStatus("PUBLISHED");
        recipe.setUpdatedAt(LocalDateTime.now());
        lenient().when(currentUserService.requireActiveExpert()).thenReturn(new CurrentUser(7L, Role.EXPERT));
    }

    @Test
    void authorCanViewRecipeHiddenByAdminWithoutGettingEditAuthorization() {
        recipe.setStatus("HIDDEN");
        when(repository.findById(47L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getPublicProfile(7L)).thenReturn(new PublicProfile(7L, "Chuyên gia", null));
        when(ingredientRepository.findAllByRecipeIdOrderByIdAsc(47L)).thenReturn(List.of());
        when(mediaRepository.findAllByRecipeIdOrderByDisplayOrderAsc(47L)).thenReturn(List.of());

        assertThat(service.getForAuthor(47L).status()).isEqualTo("HIDDEN");
    }

    @Test
    void personalListIncludesOnlyOwnedPublishedAndHiddenRecipes() {
        when(repository.findAllByAuthorIdAndStatusInOrderByUpdatedAtDesc(7L, List.of("PUBLISHED", "HIDDEN"),
                PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(recipe)));
        when(currentUserService.getPublicProfile(7L)).thenReturn(new PublicProfile(7L, "Chuyên gia", null));
        when(ingredientRepository.findAllByRecipeIdOrderByIdAsc(47L)).thenReturn(List.of());
        when(mediaRepository.findAllByRecipeIdOrderByDisplayOrderAsc(47L)).thenReturn(List.of());

        var response = service.listMine(0, 20);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(47L);
            assertThat(item.status()).isEqualTo("PUBLISHED");
        });
    }

    @Test
    void anotherExpertCannotUpdateRecipe() {
        recipe.setAuthorId(8L);
        when(currentUserService.requireActiveExpert()).thenReturn(new CurrentUser(7L, Role.EXPERT));
        when(repository.lockById(47L)).thenReturn(Optional.of(recipe));

        assertThatThrownBy(() -> service.update(47L, validRequest())).isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).errorCode()).isEqualTo(ErrorCode.RECIPE_EDIT_NOT_ALLOWED));
        assertThat(recipe.getTitle()).isEqualTo("Món cũ của tôi");
    }

    @Test
    void authorUpdatePersistsPublishedRecipeAndReturnsCurrentData() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-05T00:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(repository.lockById(47L)).thenReturn(Optional.of(recipe));
        when(validationRepository.isActiveIngredient(1L)).thenReturn(true);
        when(validationRepository.hasValidConvertibleUnit(1L, 1)).thenReturn(true);
        when(validationRepository.findIngredientUnit(1L, 1))
                .thenReturn(new RecipeValidationRepository.IngredientUnit("Nấm", "g", "gram"));
        when(currentUserService.getPublicProfile(7L)).thenReturn(new PublicProfile(7L, "Chuyên gia", null));
        when(repository.save(recipe)).thenReturn(recipe);
        RecipeIngredientEntity line = new RecipeIngredientEntity();
        line.setRecipeId(47L);
        line.setIngredientId(1L);
        line.setUnitId(1);
        line.setQuantity(new BigDecimal("100.00"));
        when(ingredientRepository.findAllByRecipeIdOrderByIdAsc(47L)).thenReturn(List.of(line));
        when(mediaRepository.findAllByRecipeIdOrderByDisplayOrderAsc(47L)).thenReturn(List.of());

        var response = service.update(47L, validRequest());

        assertThat(recipe.getStatus()).isEqualTo("PUBLISHED");
        assertThat(recipe.getTitle()).isEqualTo("Canh mới ngon");
        assertThat(response.title()).isEqualTo("Canh mới ngon");
        assertThat(response.ingredients()).singleElement().satisfies(ingredient -> {
            assertThat(ingredient.name()).isEqualTo("Nấm");
            assertThat(ingredient.unitName()).isEqualTo("gram");
        });
    }

    @Test
    void authorDeleteSetsTombstoneInsteadOfRemovingRecipe() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-05T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(repository.lockById(47L)).thenReturn(Optional.of(recipe));

        service.delete(47L);

        assertThat(recipe.getStatus()).isEqualTo("DELETED");
        verify(repository).lockById(47L);
    }

    @Test
    void whitespaceCannotHideInstructionsThatAreShorterThanTheDatabaseMinimum() {
        when(repository.lockById(47L)).thenReturn(Optional.of(recipe));
        UpdateRecipePostRequest request = new UpdateRecipePostRequest("Canh mới ngon", "", "   ngắn    ",
                "SOUP", "VEGAN", "EASY", 2, 5, 10, "", List.of());

        assertThatThrownBy(() -> service.update(47L, request)).isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).errorCode()).isEqualTo(ErrorCode.RECIPE_DATA_INVALID));
        assertThat(recipe.getInstructions()).isEqualTo("Cách làm món ăn cũ");
    }

    @Test
    void referenceDataTrimsKeywordsAndRejectsTooLongSearches() {
        var option = new RecipeValidationRepository.IngredientOption(1L, "Nấm", 1, "g", "gram");
        when(validationRepository.findIngredientOptions("nam")).thenReturn(List.of(option));
        when(validationRepository.findIngredientOptions("")).thenReturn(List.of());

        assertThat(service.referenceData(" nam ").ingredients()).containsExactly(option);
        assertThat(service.referenceData(null).ingredients()).isEmpty();
        assertThatThrownBy(() -> service.referenceData("x".repeat(101)))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        verify(validationRepository, never()).findIngredientOptions("x".repeat(101));
    }

    @Test
    void mineAndPublicSearchRejectOutOfRangePagingAndNormalizePublicKeyword() {
        assertThatThrownBy(() -> service.listMine(-1, 20)).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThatThrownBy(() -> service.listMine(0, 51)).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThatThrownBy(() -> service.searchPublished("x".repeat(121), 0, 12)).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThatThrownBy(() -> service.searchPublished(null, 0, 0)).isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-08T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(browseRepository.findPublished("", null, null, List.of(), null,
                tech.mamxanh.recipe.service.RecipeSortMode.NEWEST, LocalDateTime.of(1, 1, 1, 0, 0),
                LocalDateTime.of(2026, 10, 8, 0, 0), 0, 12))
                .thenReturn(new RecipeBrowseRepository.BrowsePage(List.of(), 0));
        assertThat(service.searchPublished(null, 0, 12).items()).isEmpty();
        assertThat(service.searchPublished("  ", 0, 12).items()).isEmpty();
        verify(browseRepository, times(2)).findPublished("", null, null, List.of(), null,
                tech.mamxanh.recipe.service.RecipeSortMode.NEWEST, LocalDateTime.of(1, 1, 1, 0, 0),
                LocalDateTime.of(2026, 10, 8, 0, 0), 0, 12);
    }

    @Test
    void publicBrowseMapsReadStatisticsAndUsesTheSelectedViewWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 8, 0, 0);
        when(clock.instant()).thenReturn(now.toInstant(ZoneOffset.UTC));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        recipe.setStatus("PUBLISHED");
        recipe.setTitle("Canh chua");
        var mode = tech.mamxanh.recipe.service.RecipeSortMode.MOST_VIEWED;
        var period = tech.mamxanh.recipe.service.RecipeViewPeriod.LAST_24_HOURS;
        var row = new RecipeBrowseRepository.BrowseRow(47L, 3, 1, 19, 2, 5);
        when(browseRepository.findPublished("canh", null, null, List.of(), null, mode, now.minusHours(24), now, 0, 12))
                .thenReturn(new RecipeBrowseRepository.BrowsePage(List.of(row), 1));
        when(repository.findAllById(List.of(47L))).thenReturn(List.of(recipe));
        when(currentUserService.getPublicProfiles(List.of(7L))).thenReturn(Map.of(7L,
                new PublicProfile(7L, "Tác giả", "https://img.test/avatar.png")));
        when(mediaRepository.findAllByRecipeIdInOrderByRecipeIdAscDisplayOrderAsc(List.of(47L))).thenReturn(List.of());
        when(ingredientRepository.findAllByRecipeIdInOrderByRecipeIdAscIdAsc(List.of(47L))).thenReturn(List.of());

        var result = service.searchPublished(" canh ", 0, 12, mode, period);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().authorName()).isEqualTo("Tác giả");
        assertThat(result.items().getFirst().authorAvatarUrl()).isEqualTo("https://img.test/avatar.png");
        assertThat(result.items().getFirst().likes()).isEqualTo(3);
        assertThat(result.items().getFirst().dislikes()).isEqualTo(1);
        assertThat(result.items().getFirst().likePercentage()).isEqualByComparingTo("75.00");
        assertThat(result.items().getFirst().viewCount()).isEqualTo(19);
        verify(browseRepository).findPublished("canh", null, null, List.of(), null, mode, now.minusHours(24), now, 0, 12);
    }

    @Test
    void publicBrowseBatchesAndMapsRecipeMediaAndIngredientReferences() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 8, 0, 0);
        when(clock.instant()).thenReturn(now.toInstant(ZoneOffset.UTC));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        var row = new RecipeBrowseRepository.BrowseRow(47L, 1, 1, 6, 0, 2);
        when(browseRepository.findPublished("", null, null, List.of(), null,
                tech.mamxanh.recipe.service.RecipeSortMode.NEWEST, LocalDateTime.of(1, 1, 1, 0, 0), now, 0, 12))
                .thenReturn(new RecipeBrowseRepository.BrowsePage(List.of(row), 1));
        when(repository.findAllById(List.of(47L))).thenReturn(List.of(recipe));
        when(currentUserService.getPublicProfiles(List.of(7L))).thenReturn(Map.of(7L,
                new PublicProfile(7L, "Tác giả", null)));

        RecipeMediaEntity media = new RecipeMediaEntity();
        media.setRecipeId(47L);
        media.setBlobUrl("https://img.test/recipe.png");
        media.setMimeType("image/png");
        media.setDisplayOrder(1);
        media.setCover(true);
        when(mediaRepository.findAllByRecipeIdInOrderByRecipeIdAscDisplayOrderAsc(List.of(47L)))
                .thenReturn(List.of(media));

        RecipeIngredientEntity ingredientLine = new RecipeIngredientEntity();
        ingredientLine.setRecipeId(47L);
        ingredientLine.setIngredientId(91L);
        ingredientLine.setUnitId(3);
        ingredientLine.setQuantity(new BigDecimal("125.00"));
        when(ingredientRepository.findAllByRecipeIdInOrderByRecipeIdAscIdAsc(List.of(47L)))
                .thenReturn(List.of(ingredientLine));
        RecipeIngredientReferenceEntity ingredientReference = new RecipeIngredientReferenceEntity();
        ReflectionTestUtils.setField(ingredientReference, "id", 91L);
        ReflectionTestUtils.setField(ingredientReference, "name", "Đậu hũ");
        when(ingredientReferenceRepository.findAllById(List.of(91L))).thenReturn(List.of(ingredientReference));
        RecipeUnitReferenceEntity unitReference = new RecipeUnitReferenceEntity();
        ReflectionTestUtils.setField(unitReference, "id", 3);
        ReflectionTestUtils.setField(unitReference, "code", "g");
        ReflectionTestUtils.setField(unitReference, "name", "gram");
        when(unitReferenceRepository.findAllById(List.of(3))).thenReturn(List.of(unitReference));

        var result = service.searchPublished(null, 0, 12);

        assertThat(result.items().getFirst().media()).containsExactly(
                new RecipePostResponse.Media("https://img.test/recipe.png", "image/png", 1, true));
        assertThat(result.items().getFirst().ingredients()).containsExactly(
                new RecipePostResponse.Ingredient(91L, "Đậu hũ", null, 3, "g", "gram", new BigDecimal("125.00")));
        assertThat(result.items().getFirst().likePercentage()).isEqualByComparingTo("50.00");
    }

    @Test
    void publicBrowseNormalizesFilterCodesAndDeduplicatesIngredientIds() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 8, 0, 0);
        when(clock.instant()).thenReturn(now.toInstant(ZoneOffset.UTC));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(browseRepository.findPublished("", "VEGAN", "SOUP", List.of(44L, 55L), 30,
                tech.mamxanh.recipe.service.RecipeSortMode.NEWEST,
                LocalDateTime.of(1, 1, 1, 0, 0), now, 0, 12))
                .thenReturn(new RecipeBrowseRepository.BrowsePage(List.of(), 0));

        service.searchPublished("", 0, 12, tech.mamxanh.recipe.service.RecipeSortMode.NEWEST,
                tech.mamxanh.recipe.service.RecipeViewPeriod.ALL_TIME, " vegan ", "soup",
                List.of(44L, 44L, 55L), 30);

        verify(browseRepository).findPublished("", "VEGAN", "SOUP", List.of(44L, 55L), 30,
                tech.mamxanh.recipe.service.RecipeSortMode.NEWEST,
                LocalDateTime.of(1, 1, 1, 0, 0), now, 0, 12);
        assertThatThrownBy(() -> service.searchPublished("", 0, 12,
                tech.mamxanh.recipe.service.RecipeSortMode.NEWEST,
                tech.mamxanh.recipe.service.RecipeViewPeriod.ALL_TIME, "UNKNOWN", null, List.of(), null))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    private static UpdateRecipePostRequest validRequest() {
        return new UpdateRecipePostRequest("Canh mới ngon", "Mô tả mới", "Nấu món này thật ngon",
                "SOUP", "VEGAN", "MEDIUM", 3, 5, 15, "", List.of(
                        new UpdateRecipePostRequest.Ingredient(1L, 1, new BigDecimal("100.00"))));
    }
}
