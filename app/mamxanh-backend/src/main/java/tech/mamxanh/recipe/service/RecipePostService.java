package tech.mamxanh.recipe.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;
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
import tech.mamxanh.recipe.entity.RecipeIngredientReferenceEntity;
import tech.mamxanh.recipe.entity.RecipeUnitReferenceEntity;
import tech.mamxanh.recipe.repository.RecipeIngredientRepository;
import tech.mamxanh.recipe.repository.RecipeIngredientReferenceRepository;
import tech.mamxanh.recipe.repository.RecipeMediaRepository;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.RecipeUnitReferenceRepository;
import tech.mamxanh.recipe.repository.RecipeBrowseRepository;
import tech.mamxanh.recipe.repository.RecipeBrowseRepository.BrowseRow;
import tech.mamxanh.recipe.repository.RecipeStatisticsRepository;
import tech.mamxanh.recipe.repository.RecipeValidationRepository;

@Service
public class RecipePostService {
    private final RecipePostRepository repository;
    private final RecipeBrowseRepository browseRepository;
    private final RecipeStatisticsRepository statisticsRepository;
    private final RecipeIngredientRepository ingredientRepository;
    private final RecipeIngredientReferenceRepository ingredientReferenceRepository;
    private final RecipeUnitReferenceRepository unitReferenceRepository;
    private final RecipeMediaRepository mediaRepository;
    private final RecipeValidationRepository validationRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public RecipePostService(RecipePostRepository repository, RecipeBrowseRepository browseRepository,
            RecipeStatisticsRepository statisticsRepository, RecipeIngredientRepository ingredientRepository,
            RecipeIngredientReferenceRepository ingredientReferenceRepository,
            RecipeUnitReferenceRepository unitReferenceRepository,
            RecipeMediaRepository mediaRepository, RecipeValidationRepository validationRepository,
            CurrentUserService currentUserService, Clock clock) {
        this.repository = repository;
        this.browseRepository = browseRepository;
        this.statisticsRepository = statisticsRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientReferenceRepository = ingredientReferenceRepository;
        this.unitReferenceRepository = unitReferenceRepository;
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
        if ("DELETED".equals(recipe.getStatus())) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }
        return response(recipe);
    }

    /** Locks the recipe row so concurrent report submissions for the same post serialize. */
    @Transactional
    public void requirePublishedForReport(long recipeId) {
        RecipePostEntity recipe = repository.lockById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        if (!"PUBLISHED".equals(recipe.getStatus())) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public RecipePageResponse listMine(int page, int size) {
        CurrentUser author = currentUserService.requireActiveExpert();
        if (page < 0 || size < 1 || size > 50) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
        Page<RecipePostEntity> result = repository.findAllByAuthorIdAndStatusInOrderByUpdatedAtDesc(
                author.id(), List.of("PUBLISHED", "HIDDEN"), PageRequest.of(page, size, Sort.unsorted()));
        List<RecipePostResponse> items = result.getContent().stream().map(this::response).toList();
        return new RecipePageResponse(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public RecipePageResponse searchPublished(String keyword, int page, int size) {
        return searchPublished(keyword, page, size, RecipeSortMode.NEWEST, RecipeViewPeriod.ALL_TIME);
    }

    @Transactional(readOnly = true)
    public RecipePageResponse searchPublished(String keyword, int page, int size,
            RecipeSortMode sortMode, RecipeViewPeriod viewPeriod) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.length() > 120 || page < 0 || size < 1 || size > 50) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime viewSince = viewPeriod.hours() == null
                ? LocalDateTime.of(1, 1, 1, 0, 0) : now.minusHours(viewPeriod.hours());
        var result = browseRepository.findPublished(query, sortMode, viewSince, now, page, size);
        List<Long> ids = result.rows().stream().map(BrowseRow::recipeId).toList();
        Map<Long, RecipePostEntity> recipes = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(RecipePostEntity::getId, Function.identity()));
        Map<Long, BrowseRow> metrics = result.rows().stream()
                .collect(Collectors.toMap(BrowseRow::recipeId, Function.identity()));
        List<RecipePostEntity> orderedRecipes = ids.stream().map(recipes::get)
                .filter(java.util.Objects::nonNull).toList();
        List<RecipePostResponse> items = publicResponses(orderedRecipes, metrics);
        int totalPages = (int) Math.ceil((double) result.totalElements() / size);
        return new RecipePageResponse(items, page, size, result.totalElements(), totalPages);
    }

    @Transactional(readOnly = true)
    public RecipeReferenceData referenceData(String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.length() > 100) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
        return new RecipeReferenceData(validationRepository.findIngredientOptions(query));
    }

    @Transactional(readOnly = true)
    public Map<Long, MealPlanRecipeReference> findMealPlanReferences(Collection<Long> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) return Map.of();
        return repository.findAllById(recipeIds).stream().collect(Collectors.toMap(RecipePostEntity::getId, recipe -> {
            String coverUrl = mediaRepository.findAllByRecipeIdOrderByDisplayOrderAsc(recipe.getId()).stream()
                    .filter(RecipeMediaEntity::isCover).map(RecipeMediaEntity::getBlobUrl).findFirst().orElse(null);
            return new MealPlanRecipeReference(recipe.getId(), recipe.getTitle(), recipe.getStatus(), coverUrl,
                    recipe.getDishCategory(), recipe.getPrepTimeMinutes() + recipe.getCookTimeMinutes());
        }, (first, ignored) -> first));
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
        var stats = statisticsRepository.findStatistics(recipe.getId()).orElse(null);
        return response(recipe, stats == null ? null : new BrowseRow(recipe.getId(), stats.getLikes(),
                stats.getDislikes(), stats.getViewCount(), 0, stats.getViewCount()));
    }

    private RecipePostResponse response(RecipePostEntity recipe, BrowseRow stats) {
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
        return assembleResponse(recipe, stats, author, media, ingredients);
    }

    private List<RecipePostResponse> publicResponses(List<RecipePostEntity> recipes, Map<Long, BrowseRow> metrics) {
        if (recipes.isEmpty()) return List.of();
        List<Long> recipeIds = recipes.stream().map(RecipePostEntity::getId).toList();
        Map<Long, PublicProfile> authors = currentUserService.getPublicProfiles(
                recipes.stream().map(RecipePostEntity::getAuthorId).distinct().toList());
        Map<Long, List<Media>> mediaByRecipe = mediaRepository.findAllByRecipeIdInOrderByRecipeIdAscDisplayOrderAsc(recipeIds)
                .stream().collect(Collectors.groupingBy(RecipeMediaEntity::getRecipeId, Collectors.mapping(
                        item -> new Media(item.getBlobUrl(), item.getMimeType(), item.getDisplayOrder(), item.isCover()),
                        Collectors.toList())));
        List<RecipeIngredientEntity> ingredientRows = ingredientRepository
                .findAllByRecipeIdInOrderByRecipeIdAscIdAsc(recipeIds);
        List<Long> ingredientIds = ingredientRows.stream().map(RecipeIngredientEntity::getIngredientId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        List<Integer> unitIds = ingredientRows.stream().map(RecipeIngredientEntity::getUnitId).distinct().toList();
        Map<Long, RecipeIngredientReferenceEntity> ingredients = ingredientIds.isEmpty() ? Map.of()
                : ingredientReferenceRepository.findAllById(ingredientIds).stream()
                        .collect(Collectors.toMap(RecipeIngredientReferenceEntity::getId, Function.identity()));
        Map<Integer, RecipeUnitReferenceEntity> units = unitIds.isEmpty() ? Map.of()
                : unitReferenceRepository.findAllById(unitIds).stream()
                        .collect(Collectors.toMap(RecipeUnitReferenceEntity::getId, Function.identity()));
        Map<Long, List<Ingredient>> ingredientsByRecipe = ingredientRows.stream().collect(Collectors.groupingBy(
                RecipeIngredientEntity::getRecipeId, Collectors.mapping(row -> {
                    RecipeIngredientReferenceEntity ingredient = row.getIngredientId() == null
                            ? null : ingredients.get(row.getIngredientId());
                    RecipeUnitReferenceEntity unit = units.get(row.getUnitId());
                    return new Ingredient(row.getIngredientId(), ingredient == null ? row.getCustomIngredientName() : ingredient.getName(),
                            row.getCustomIngredientName(), row.getUnitId(), unit == null ? null : unit.getCode(),
                            unit == null ? null : unit.getName(), row.getQuantity());
                }, Collectors.toList())));
        return recipes.stream().map(recipe -> assembleResponse(recipe, metrics.get(recipe.getId()),
                authors.get(recipe.getAuthorId()), mediaByRecipe.getOrDefault(recipe.getId(), List.of()),
                ingredientsByRecipe.getOrDefault(recipe.getId(), List.of()))).toList();
    }

    private RecipePostResponse assembleResponse(RecipePostEntity recipe, BrowseRow stats, PublicProfile author,
            List<Media> media, List<Ingredient> ingredients) {
        long likes = stats == null ? 0 : stats.likes();
        long dislikes = stats == null ? 0 : stats.dislikes();
        long reactionCount = likes + dislikes;
        BigDecimal likePercentage = reactionCount == 0 ? null
                : BigDecimal.valueOf(likes).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(reactionCount), 2, RoundingMode.HALF_UP);
        return new RecipePostResponse(recipe.getId(), recipe.getAuthorId(), author.displayName(), author.avatarUrl(),
                recipe.getTitle(), recipe.getDescription(), recipe.getInstructions(), recipe.getDishCategory(),
                recipe.getVegetarianType(), recipe.getDifficulty(), recipe.getServings(), recipe.getPrepTimeMinutes(),
                recipe.getCookTimeMinutes(), recipe.getYoutubeUrl(), recipe.getStatus(), media, ingredients,
                likes, dislikes, likePercentage, stats == null ? 0 : stats.views());
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

    public record MealPlanRecipeReference(long recipeId, String title, String status, String coverUrl,
            String dishCategory, int totalTimeMinutes) { }
}
