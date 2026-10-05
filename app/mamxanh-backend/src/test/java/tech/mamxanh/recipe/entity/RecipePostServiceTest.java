package tech.mamxanh.recipe.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.RecipeValidationRepository;
import tech.mamxanh.recipe.service.RecipePostService;

@ExtendWith(MockitoExtension.class)
class RecipePostServiceTest {
    @Mock private RecipePostRepository repository;
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
        when(currentUserService.requireActiveExpert()).thenReturn(new CurrentUser(7L, Role.EXPERT));
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
        when(clock.instant()).thenReturn(Instant.parse("2026-10-05T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
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

    private static UpdateRecipePostRequest validRequest() {
        return new UpdateRecipePostRequest("Canh mới ngon", "Mô tả mới", "Nấu món này thật ngon",
                "SOUP", "VEGAN", "MEDIUM", 3, 5, 15, "", List.of(
                        new UpdateRecipePostRequest.Ingredient(1L, 1, new BigDecimal("100.00"))));
    }
}
