package tech.mamxanh.recipe.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.CurrentUserService.CurrentUser;
import tech.mamxanh.auth.service.CurrentUserService.PublicProfile;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.dto.request.UpdateRecipePostRequest;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.recipe.dto.response.RecipePostResponse.Ingredient;
import tech.mamxanh.recipe.dto.response.RecipePostResponse.Media;
import tech.mamxanh.recipe.entity.RecipeIngredientEntity;
import tech.mamxanh.recipe.entity.RecipeMediaEntity;
import tech.mamxanh.recipe.entity.RecipePostEntity;
import tech.mamxanh.recipe.repository.RecipeIngredientRepository;
import tech.mamxanh.recipe.repository.RecipeMediaRepository;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.RecipeValidationRepository;

@Service
public class RecipePostService {
    private final RecipePostRepository repository;
    private final RecipeIngredientRepository ingredientRepository;
    private final RecipeMediaRepository mediaRepository;
    private final RecipeValidationRepository validationRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public RecipePostService(RecipePostRepository repository, RecipeIngredientRepository ingredientRepository,
            RecipeMediaRepository mediaRepository, RecipeValidationRepository validationRepository,
            CurrentUserService currentUserService, Clock clock) {
        this.repository = repository;
        this.ingredientRepository = ingredientRepository;
        this.mediaRepository = mediaRepository;
        this.validationRepository = validationRepository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RecipePostResponse getForAuthor(long recipeId) {
        CurrentUser author = currentUserService.requireActiveExpert();
        RecipePostEntity recipe = findById(recipeId);
        requireOwner(recipe, author);
        if ("HIDDEN".equals(recipe.getStatus())) {
            throw new AppException(ErrorCode.RECIPE_HIDDEN);
        }
        if (!"PUBLISHED".equals(recipe.getStatus())) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }
        return response(recipe);
    }

    @Transactional(readOnly = true)
    public RecipePageResponse searchPublished(String keyword, int page, int size) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.length() > 120 || page < 0 || size < 1 || size > 50) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
        Page<RecipePostEntity> result = repository.findAllByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
                "PUBLISHED", query, PageRequest.of(page, size, Sort.unsorted()));
        List<RecipePostResponse> items = result.getContent().stream().map(this::response).toList();
        return new RecipePageResponse(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public RecipeReferenceData referenceData(String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.length() > 100) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
        return new RecipeReferenceData(validationRepository.findIngredientOptions(query));
    }

    @Transactional
    public RecipePostResponse update(long recipeId, UpdateRecipePostRequest request) {
        CurrentUser author = currentUserService.requireActiveExpert();
        RecipePostEntity recipe = repository.lockById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        requireOwner(recipe, author);
        if ("HIDDEN".equals(recipe.getStatus())) {
            throw new AppException(ErrorCode.RECIPE_HIDDEN);
        }
        if (!"PUBLISHED".equals(recipe.getStatus())) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }

        validateProfile(request);
        recipe.setTitle(request.title().trim());
        recipe.setDescription(blankToNull(request.description()));
        recipe.setInstructions(request.instructions().trim());
        recipe.setDishCategory(request.dishCategory());
        recipe.setVegetarianType(request.vegetarianType());
        recipe.setDifficulty(request.difficulty());
        recipe.setServings(request.servings());
        recipe.setPrepTimeMinutes(request.prepTimeMin());
        recipe.setCookTimeMinutes(request.cookTimeMin());
        recipe.setYoutubeUrl(blankToNull(request.youtubeUrl()));
        recipe.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(recipe);

        ingredientRepository.deleteAllByRecipeId(recipeId);
        List<RecipeIngredientEntity> replacements = request.ingredients().stream().map(item -> {
            RecipeIngredientEntity line = new RecipeIngredientEntity();
            line.setRecipeId(recipeId);
            line.setIngredientId(item.ingredientId());
            line.setUnitId(item.unitId());
            line.setQuantity(item.quantity());
            return line;
        }).toList();
        ingredientRepository.saveAll(replacements);
        return response(recipe);
    }

    @Transactional
    public void delete(long recipeId) {
        CurrentUser author = currentUserService.requireActiveExpert();
        RecipePostEntity recipe = repository.lockById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        requireOwner(recipe, author);
        if ("DELETED".equals(recipe.getStatus())) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }
        recipe.setStatus("DELETED");
        recipe.setUpdatedAt(LocalDateTime.now(clock));
    }

    private void validateProfile(UpdateRecipePostRequest request) {
        String title = request.title() == null ? "" : request.title().trim();
        String instructions = request.instructions() == null ? "" : request.instructions().trim();
        String description = request.description() == null ? "" : request.description().trim();
        if (title.length() < 3 || title.length() > 120 || instructions.length() < 10
                || instructions.length() > 5000 || description.length() > 2000) {
            throw new AppException(ErrorCode.RECIPE_DATA_INVALID,
                    "Kiểm tra lại tên món, mô tả và hướng dẫn theo giới hạn của Recipe Validation Profile.");
        }
        if (request.prepTimeMin() + request.cookTimeMin() <= 0) {
            throw new AppException(ErrorCode.RECIPE_DATA_INVALID,
                    "Tổng thời gian chuẩn bị và nấu phải lớn hơn 0.");
        }
        for (int index = 0; index < request.ingredients().size(); index++) {
            UpdateRecipePostRequest.Ingredient item = request.ingredients().get(index);
            if (item.ingredientId() == null || !validationRepository.isActiveIngredient(item.ingredientId())
                    || !validationRepository.hasValidConvertibleUnit(item.ingredientId(), item.unitId())) {
                throw new AppException(ErrorCode.RECIPE_DATA_INVALID,
                        "ingredients[" + index + "].ingredientId: Chọn nguyên liệu có trong danh mục và đơn vị có quy đổi hợp lệ.");
            }
        }
    }

    private RecipePostResponse response(RecipePostEntity recipe) {
        PublicProfile author = currentUserService.getPublicProfile(recipe.getAuthorId());
        List<Media> media = mediaRepository.findAllByRecipeIdOrderByDisplayOrderAsc(recipe.getId()).stream()
                .map(item -> new Media(item.getBlobUrl(), item.getMimeType(), item.getDisplayOrder(), item.isCover()))
                .toList();
        List<Ingredient> ingredients = ingredientRepository.findAllByRecipeIdOrderByIdAsc(recipe.getId()).stream()
                .map(item -> {
                    RecipeValidationRepository.IngredientUnit lookup = validationRepository.findIngredientUnit(
                            item.getIngredientId(), item.getUnitId());
                    return new Ingredient(item.getIngredientId(), lookup.ingredientName(), null,
                            item.getUnitId(), lookup.unitCode(), lookup.unitName(), item.getQuantity());
                })
                .toList();
        return new RecipePostResponse(recipe.getId(), recipe.getAuthorId(), author.displayName(), author.avatarUrl(),
                recipe.getTitle(), recipe.getDescription(), recipe.getInstructions(), recipe.getDishCategory(),
                recipe.getVegetarianType(), recipe.getDifficulty(), recipe.getServings(), recipe.getPrepTimeMinutes(),
                recipe.getCookTimeMinutes(), recipe.getYoutubeUrl(), recipe.getStatus(), media, ingredients);
    }

    private RecipePostEntity findById(long recipeId) {
        return repository.findById(recipeId).orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
    }

    private static void requireOwner(RecipePostEntity recipe, CurrentUser author) {
        if (!recipe.getAuthorId().equals(author.id())) {
            throw new AppException(ErrorCode.RECIPE_EDIT_NOT_ALLOWED);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record RecipePageResponse(List<RecipePostResponse> items, int page, int size,
            long totalElements, int totalPages) { }

    public record RecipeReferenceData(List<RecipeValidationRepository.IngredientOption> ingredients) { }
}
