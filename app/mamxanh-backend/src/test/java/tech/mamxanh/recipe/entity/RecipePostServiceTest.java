package tech.mamxanh.recipe.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    @Mock private RecipeValidationRepository validationRepository;
    @Mock private CurrentUserService currentUserService;
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
        recipe.setPrepTimeMin(10);
        recipe.setCookTimeMin(10);
        recipe.setStatus(RecipePostStatus.PUBLISHED);
        recipe.setUpdatedAt(LocalDateTime.now());
        recipe.setMedia(new ArrayList<>());
        recipe.setIngredients(new ArrayList<>());
        when(currentUserService.requireActiveExpert()).thenReturn(new CurrentUser(7L, Role.EXPERT));
    }

    @Test
    void authorCannotOpenRecipeHiddenByAdmin() {
        recipe.setStatus(RecipePostStatus.HIDDEN);
        when(repository.findWithMediaById(47L)).thenReturn(Optional.of(recipe));
        when(repository.findWithIngredientsById(47L)).thenReturn(Optional.of(recipe));

        assertThatThrownBy(() -> service.getForAuthor(47L)).isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).errorCode()).isEqualTo(ErrorCode.RECIPE_HIDDEN));
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
        when(repository.lockById(47L)).thenReturn(Optional.of(recipe));
        when(validationRepository.isActiveIngredient(null)).thenReturn(true);
        when(validationRepository.hasValidConvertibleUnit(null, 1)).thenReturn(true);
        when(validationRepository.findIngredientUnit(null, 1))
                .thenReturn(new RecipeValidationRepository.IngredientUnit(null, "g", "gram"));
        when(currentUserService.getPublicProfile(7L)).thenReturn(new PublicProfile(7L, "Chuyên gia", null));

        var response = service.update(47L, validRequest());

        assertThat(recipe.getStatus()).isEqualTo(RecipePostStatus.PUBLISHED);
        assertThat(recipe.getTitle()).isEqualTo("Canh mới ngon");
        assertThat(response.title()).isEqualTo("Canh mới ngon");
        assertThat(response.ingredients()).singleElement().satisfies(ingredient -> {
            assertThat(ingredient.customName()).isEqualTo("Nấm");
            assertThat(ingredient.unitName()).isEqualTo("gram");
        });
    }

    @Test
    void authorDeleteSetsTombstoneInsteadOfRemovingRecipe() {
        when(repository.lockById(47L)).thenReturn(Optional.of(recipe));

        service.delete(47L);

        assertThat(recipe.getStatus()).isEqualTo(RecipePostStatus.DELETED);
        verify(repository).lockById(47L);
    }

    @Test
    void invalidCoverSelectionDoesNotChangeRecipe() {
        when(repository.lockById(47L)).thenReturn(Optional.of(recipe));
        UpdateRecipePostRequest request = new UpdateRecipePostRequest("Canh mới ngon", "", "Nấu món này thật ngon",
                "SOUP", "VEGAN", "EASY", 2, 5, 10, "", List.of(),
                List.of(new UpdateRecipePostRequest.Media("https://cdn.test/photo.jpg", "image/jpeg", 1, false)));

        assertThatThrownBy(() -> service.update(47L, request)).isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).errorCode()).isEqualTo(ErrorCode.RECIPE_DATA_INVALID));
        assertThat(recipe.getTitle()).isEqualTo("Món cũ của tôi");
    }

    @Test
    void whitespaceCannotHideInstructionsThatAreShorterThanTheDatabaseMinimum() {
        when(repository.lockById(47L)).thenReturn(Optional.of(recipe));
        UpdateRecipePostRequest request = new UpdateRecipePostRequest("Canh mới ngon", "", "   ngắn    ",
                "SOUP", "VEGAN", "EASY", 2, 5, 10, "", List.of(), List.of());

        assertThatThrownBy(() -> service.update(47L, request)).isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).errorCode()).isEqualTo(ErrorCode.RECIPE_DATA_INVALID));
        assertThat(recipe.getInstructions()).isEqualTo("Cách làm món ăn cũ");
    }

    private static UpdateRecipePostRequest validRequest() {
        return new UpdateRecipePostRequest("Canh mới ngon", "Mô tả mới", "Nấu món này thật ngon",
                "SOUP", "VEGAN", "MEDIUM", 3, 5, 15, "", List.of(
                        new UpdateRecipePostRequest.Ingredient(null, "Nấm", 1, new BigDecimal("100.00"))), List.of());
    }
}
