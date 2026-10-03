package tech.mamxanh.recipe.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import tech.mamxanh.recipe.entity.RecipePostStatus;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.RecipeValidationRepository;

@Service
public class RecipePostService {
    private final RecipePostRepository repository;
    private final RecipeValidationRepository validationRepository;
    private final CurrentUserService currentUserService;

    public RecipePostService(RecipePostRepository repository, RecipeValidationRepository validationRepository,
            CurrentUserService currentUserService) {
        this.repository = repository;
        this.validationRepository = validationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public RecipePostResponse getPublished(long recipeId) {
        RecipePostEntity recipe = findDetailed(recipeId);
        if (recipe.getStatus() != RecipePostStatus.PUBLISHED) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }
        return response(recipe);
    }

    @Transactional(readOnly = true)
    public RecipePostResponse getForAuthor(long recipeId) {
        CurrentUser author = currentUserService.requireActiveExpert();
        RecipePostEntity recipe = findDetailed(recipeId);
        if (recipe.getStatus() == RecipePostStatus.DELETED) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }
        requireOwner(recipe, author);
        if (recipe.getStatus() == RecipePostStatus.HIDDEN) {
            throw new AppException(ErrorCode.RECIPE_HIDDEN);
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
                RecipePostStatus.PUBLISHED, query, PageRequest.of(page, size, Sort.unsorted()));
        List<RecipePostResponse> items = result.getContent().stream().map(this::response).toList();
        return new RecipePageResponse(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public RecipeReferenceData referenceData(String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.length() > 100) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
        return new RecipeReferenceData(validationRepository.findIngredientOptions(query),
                validationRepository.findCustomIngredientUnits());
    }

    @Transactional
    public RecipePostResponse update(long recipeId, UpdateRecipePostRequest request) {
        CurrentUser author = currentUserService.requireActiveExpert();
        RecipePostEntity recipe = repository.lockById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        requireOwner(recipe, author);
        if (recipe.getStatus() == RecipePostStatus.HIDDEN) {
            throw new AppException(ErrorCode.RECIPE_HIDDEN);
        }
        if (recipe.getStatus() != RecipePostStatus.PUBLISHED) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }
        String title = request.title().trim();
        String instructions = request.instructions().trim();
        if (title.length() < 3 || instructions.length() < 10 || instructions.length() > 5000) {
            throw new AppException(ErrorCode.RECIPE_DATA_INVALID,
                    "Tên món sau khi bỏ khoảng trắng cần ít nhất 3 ký tự; hướng dẫn cần từ 10 đến 5.000 ký tự.");
        }
        validateProfile(request);
        recipe.setTitle(title);
        recipe.setDescription(blankToNull(request.description()));
        recipe.setInstructions(instructions);
        recipe.setDishCategory(request.dishCategory());
        recipe.setVegetarianType(request.vegetarianType());
        recipe.setDifficulty(request.difficulty());
        recipe.setServings(request.servings());
        recipe.setPrepTimeMin(request.prepTimeMin());
        recipe.setCookTimeMin(request.cookTimeMin());
        recipe.setYoutubeUrl(blankToNull(request.youtubeUrl()));
        recipe.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        recipe.replaceMedia(request.media().stream()
                .map(item -> new RecipeMediaEntity(item.url(), item.mimeType(), item.displayOrder(), item.cover()))
                .toList());
        recipe.replaceIngredients(request.ingredients().stream()
                .map(item -> new RecipeIngredientEntity(item.ingredientId(), item.unitId(), blankToNull(item.customName()), item.quantity()))
                .toList());
        return response(recipe);
    }

    @Transactional
    public void delete(long recipeId) {
        CurrentUser author = currentUserService.requireActiveExpert();
        RecipePostEntity recipe = repository.lockById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        requireOwner(recipe, author);
        if (recipe.getStatus() == RecipePostStatus.DELETED) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }
        recipe.setStatus(RecipePostStatus.DELETED);
        recipe.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
    }

    private void validateProfile(UpdateRecipePostRequest request) {
        if (request.prepTimeMin() + request.cookTimeMin() <= 0) {
            throw new AppException(ErrorCode.RECIPE_DATA_INVALID, "Tổng thời gian chuẩn bị và nấu phải lớn hơn 0.");
        }
        List<UpdateRecipePostRequest.Media> media = request.media();
        if (!media.isEmpty() && media.stream().filter(UpdateRecipePostRequest.Media::cover).count() != 1) {
            throw new AppException(ErrorCode.RECIPE_DATA_INVALID, "Chọn đúng một ảnh bìa khi công thức có hình ảnh.");
        }
        if (media.stream().map(UpdateRecipePostRequest.Media::displayOrder).distinct().count() != media.size()) {
            throw new AppException(ErrorCode.RECIPE_DATA_INVALID, "Thứ tự hình ảnh không được trùng nhau.");
        }
        for (UpdateRecipePostRequest.Ingredient item : request.ingredients()) {
            if ((item.ingredientId() == null && (item.customName() == null || item.customName().isBlank()))
                    || !validationRepository.isActiveIngredient(item.ingredientId())
                    || !validationRepository.hasValidConvertibleUnit(item.ingredientId(), item.unitId())) {
                throw new AppException(ErrorCode.RECIPE_DATA_INVALID,
                        "Mỗi nguyên liệu cần tên hợp lệ và đơn vị đang dùng có thể quy đổi sang gam.");
            }
        }
    }

    private RecipePostResponse response(RecipePostEntity recipe) {
        PublicProfile author = currentUserService.getPublicProfile(recipe.getAuthorId());
        List<Media> media = recipe.getMedia().stream()
                .sorted(java.util.Comparator.comparingInt(RecipeMediaEntity::getDisplayOrder))
                .map(item -> new Media(item.getUrl(), item.getMimeType(), item.getDisplayOrder(), item.isCover())).toList();
        List<Ingredient> ingredients = recipe.getIngredients().stream()
                .map(item -> {
                    RecipeValidationRepository.IngredientUnit lookup = validationRepository.findIngredientUnit(
                            item.getIngredientId(), item.getUnitId());
                    return new Ingredient(item.getIngredientId(), lookup.ingredientName(), item.getCustomName(),
                            item.getUnitId(), lookup.unitCode(), lookup.unitName(), item.getQuantity());
                })
                .toList();
        return new RecipePostResponse(recipe.getId(), recipe.getAuthorId(), author.displayName(), author.avatarUrl(),
                recipe.getTitle(), recipe.getDescription(), recipe.getInstructions(), recipe.getDishCategory(),
                recipe.getVegetarianType(), recipe.getDifficulty(), recipe.getServings(), recipe.getPrepTimeMin(),
                recipe.getCookTimeMin(), recipe.getYoutubeUrl(), recipe.getStatus().name(), media, ingredients);
    }

    private RecipePostEntity findDetailed(long recipeId) {
        RecipePostEntity recipe = repository.findWithMediaById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        return repository.findWithIngredientsById(recipeId).orElse(recipe);
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

    public record RecipeReferenceData(List<RecipeValidationRepository.IngredientOption> ingredients,
            List<RecipeValidationRepository.UnitOption> customIngredientUnits) { }
}
