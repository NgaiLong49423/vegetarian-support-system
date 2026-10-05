package tech.mamxanh.recipe.service;

import java.net.URI;
import java.util.ArrayList;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.dto.request.CreateRecipeRequest;
import tech.mamxanh.recipe.dto.request.RecipeIngredientInput;
import tech.mamxanh.recipe.dto.request.RecipeMediaInput;
import tech.mamxanh.recipe.dto.response.CreateRecipeResponse;
import tech.mamxanh.recipe.dto.response.IngredientOptionResponse;
import tech.mamxanh.recipe.dto.response.RecipeFormOptionsResponse;
import tech.mamxanh.recipe.dto.response.RecipeDetailResponse;
import tech.mamxanh.recipe.dto.response.RecipeFormOptionsResponse.Choice;
import tech.mamxanh.recipe.dto.response.RecipeFormOptionsResponse.UnitOption;
import tech.mamxanh.recipe.entity.RecipeAuthorReferenceEntity;
import tech.mamxanh.recipe.entity.RecipeCodes.Difficulty;
import tech.mamxanh.recipe.entity.RecipeCodes.DishCategory;
import tech.mamxanh.recipe.entity.RecipeCodes.MediaType;
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

@Service
public class RecipeService {
    private final RecipePostRepository recipeRepository;
    private final RecipeIngredientRepository ingredientRepository;
    private final RecipeIngredientReferenceRepository ingredientReferenceRepository;
    private final RecipeUnitReferenceRepository unitReferenceRepository;
    private final RecipeConversionRepository conversionRepository;
    private final RecipeAuthorReferenceRepository authorRepository;
    private final Clock clock;

    @Autowired
    public RecipeService(RecipePostRepository recipeRepository,
            RecipeIngredientRepository ingredientRepository,
            RecipeIngredientReferenceRepository ingredientReferenceRepository,
            RecipeUnitReferenceRepository unitReferenceRepository,
            RecipeConversionRepository conversionRepository,
            RecipeAuthorReferenceRepository authorRepository, Clock clock) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientReferenceRepository = ingredientReferenceRepository;
        this.unitReferenceRepository = unitReferenceRepository;
        this.conversionRepository = conversionRepository;
        this.authorRepository = authorRepository;
        this.clock = clock;
    }

    @Transactional
    public CreateRecipeResponse publish(CreateRecipeRequest request) {
        RecipeAuthorReferenceEntity author = authenticatedExpert();
        List<FieldError> errors = validateFields(request);

        List<Long> ingredientIds = request.ingredients().stream().map(RecipeIngredientInput::ingredientId).distinct().toList();
        List<Integer> unitIds = request.ingredients().stream().map(RecipeIngredientInput::unitId).distinct().toList();
        Map<Long, RecipeIngredientReferenceEntity> ingredients = ingredientReferenceRepository
                .findAllByIdInAndStatus(ingredientIds, "ACTIVE").stream()
                .collect(Collectors.toMap(RecipeIngredientReferenceEntity::getId, Function.identity()));
        Map<Integer, RecipeUnitReferenceEntity> units = unitReferenceRepository.findAllById(unitIds).stream()
                .collect(Collectors.toMap(RecipeUnitReferenceEntity::getId, Function.identity()));

        for (int index = 0; index < request.ingredients().size(); index++) {
            RecipeIngredientInput input = request.ingredients().get(index);
            String field = "ingredients[" + index + "]";
            RecipeIngredientReferenceEntity ingredient = ingredients.get(input.ingredientId());
            RecipeUnitReferenceEntity unit = units.get(input.unitId());
            if (ingredient == null) {
                errors.add(new FieldError(field + ".ingredientId", "Chọn nguyên liệu đang có trong danh mục."));
            }
            if (unit == null) {
                errors.add(new FieldError(field + ".unitId", "Chọn đơn vị đang có trong danh mục."));
            } else {
                if (ingredient != null && !"MASS".equals(unit.getDimension())
                        && !conversionRepository.existsByIngredientIdAndUnitIdAndActiveTrue(ingredient.getId(), unit.getId())) {
                    errors.add(new FieldError(field + ".unitId",
                            "Nguyên liệu này chưa có quy đổi sang gram cho đơn vị đã chọn. Hãy chọn g/kg hoặc đơn vị có quy đổi."));
                }
            }
        }
        if (!errors.isEmpty()) throw new RecipeValidationException(errors);

        LocalDateTime publishedAt = LocalDateTime.now(clock);
        RecipePostEntity recipe = new RecipePostEntity();
        recipe.setAuthorId(author.getId());
        recipe.setTitle(request.title().trim());
        recipe.setDescription(trimToNull(request.description()));
        recipe.setInstructions(request.instructions().trim());
        recipe.setDishCategory(request.dishCategory().name());
        recipe.setVegetarianType(request.vegetarianType().name());
        recipe.setDifficulty(request.difficulty().name());
        recipe.setServings(request.servings());
        recipe.setPrepTimeMinutes(request.prepTimeMinutes());
        recipe.setCookTimeMinutes(request.cookTimeMinutes());
        recipe.setYoutubeUrl(trimToNull(request.youtubeUrl()));
        recipe.setStatus("PUBLISHED");
        recipe.setPublishedAt(publishedAt);
        RecipePostEntity saved = recipeRepository.save(recipe);

        List<RecipeIngredientEntity> recipeIngredients = request.ingredients().stream().map(input -> {
            RecipeIngredientEntity line = new RecipeIngredientEntity();
            line.setRecipeId(saved.getId());
            line.setIngredientId(input.ingredientId());
            line.setUnitId(input.unitId());
            line.setQuantity(input.quantity());
            return line;
        }).toList();
        ingredientRepository.saveAll(recipeIngredients);

        return new CreateRecipeResponse(saved.getId(), saved.getTitle(), saved.getStatus(), publishedAt);
    }

    @Transactional(readOnly = true)
    public RecipeDetailResponse getPublishedRecipe(Long recipeId) {
        RecipePostEntity recipe = recipeRepository.findByIdAndStatus(recipeId, "PUBLISHED")
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        List<RecipeIngredientEntity> recipeIngredients = ingredientRepository
                .findAllByRecipeIdOrderByIdAsc(recipeId);
        Map<Long, RecipeIngredientReferenceEntity> ingredientReferences = ingredientReferenceRepository
                .findAllById(recipeIngredients.stream().map(RecipeIngredientEntity::getIngredientId).distinct().toList())
                .stream().collect(Collectors.toMap(RecipeIngredientReferenceEntity::getId, Function.identity()));
        Map<Integer, RecipeUnitReferenceEntity> unitReferences = unitReferenceRepository
                .findAllById(recipeIngredients.stream().map(RecipeIngredientEntity::getUnitId).distinct().toList())
                .stream().collect(Collectors.toMap(RecipeUnitReferenceEntity::getId, Function.identity()));

        DishCategory category = DishCategory.valueOf(recipe.getDishCategory());
        VegetarianType vegetarianType = VegetarianType.valueOf(recipe.getVegetarianType());
        Difficulty difficulty = Difficulty.valueOf(recipe.getDifficulty());
        List<RecipeDetailResponse.Ingredient> ingredients = recipeIngredients.stream().map(line -> {
            RecipeIngredientReferenceEntity ingredient = ingredientReferences.get(line.getIngredientId());
            RecipeUnitReferenceEntity unit = unitReferences.get(line.getUnitId());
            return new RecipeDetailResponse.Ingredient(line.getIngredientId(), ingredient.getName(), line.getQuantity(),
                    line.getUnitId(), unit.getCode(), unit.getName());
        }).toList();
        return new RecipeDetailResponse(recipe.getId(), recipe.getTitle(), recipe.getDescription(),
                recipe.getInstructions(), category.name(), category.label(), vegetarianType.name(), vegetarianType.label(),
                difficulty.name(), difficulty.label(), recipe.getServings(), recipe.getPrepTimeMinutes(),
                recipe.getCookTimeMinutes(), recipe.getYoutubeUrl(), recipe.getPublishedAt(), ingredients, List.of());
    }

    @Transactional(readOnly = true)
    public RecipeFormOptionsResponse formOptions() {
        List<Choice> categories = java.util.Arrays.stream(DishCategory.values())
                .map(value -> new Choice(value.name(), value.label())).toList();
        List<Choice> vegetarianTypes = java.util.Arrays.stream(VegetarianType.values())
                .map(value -> new Choice(value.name(), value.label())).toList();
        List<Choice> difficulties = java.util.Arrays.stream(Difficulty.values())
                .map(value -> new Choice(value.name(), value.label())).toList();
        List<UnitOption> units = unitReferenceRepository.findAllByOrderByNameAsc().stream()
                .map(unit -> new UnitOption(unit.getId(), unit.getCode(), unit.getName(), unit.getDimension())).toList();
        return new RecipeFormOptionsResponse(categories, vegetarianTypes, difficulties, units);
    }

    @Transactional(readOnly = true)
    public List<IngredientOptionResponse> findIngredients(String query) {
        String keyword = query == null ? "" : query.trim();
        return ingredientReferenceRepository.findTop30ByStatusAndNameContainingIgnoreCaseOrderByNameAsc("ACTIVE", keyword)
                .stream().map(item -> new IngredientOptionResponse(item.getId(), item.getName())).toList();
    }

    private RecipeAuthorReferenceEntity authenticatedExpert() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Không xác định được tài khoản từ phiên đăng nhập.");
        }
        RecipeAuthorReferenceEntity author = authorRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (!"ACTIVE".equals(author.getAccountStatus()) || !"EXPERT".equals(author.getRole())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Chỉ tài khoản Chuyên gia đang hoạt động được đăng công thức.");
        }
        return author;
    }

    private List<FieldError> validateFields(CreateRecipeRequest request) {
        List<FieldError> errors = new ArrayList<>();
        String title = request.title() == null ? "" : request.title().trim();
        if (title.length() < 3 || title.length() > 120) {
            errors.add(new FieldError("title", "Tên món phải dài từ 3 đến 120 ký tự sau khi bỏ khoảng trắng thừa."));
        }
        String instructions = request.instructions() == null ? "" : request.instructions().trim();
        if (instructions.length() < 10 || instructions.length() > 5000) {
            errors.add(new FieldError("instructions", "Hướng dẫn phải dài từ 10 đến 5.000 ký tự sau khi bỏ khoảng trắng thừa."));
        }
        String description = request.description() == null ? "" : request.description().trim();
        if (description.length() > 2000) errors.add(new FieldError("description", "Mô tả tối đa 2.000 ký tự."));
        if (request.prepTimeMinutes() != null && request.cookTimeMinutes() != null
                && request.prepTimeMinutes() + request.cookTimeMinutes() <= 0) {
            errors.add(new FieldError("prepTimeMinutes", "Tổng thời gian chuẩn bị và nấu phải lớn hơn 0."));
        }
        validateYoutube(request.youtubeUrl(), errors);
        validateMedia(request.media(), errors);
        return errors;
    }

    private static void validateYoutube(String rawUrl, List<FieldError> errors) {
        if (rawUrl == null || rawUrl.isBlank()) return;
        try {
            URI uri = URI.create(rawUrl.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            boolean supportedHost = host.equals("youtu.be") || host.equals("youtube.com") || host.equals("www.youtube.com");
            String path = uri.getPath() == null ? "" : uri.getPath();
            boolean hasVideo = host.equals("youtu.be") ? path.length() > 1
                    : path.startsWith("/embed/") || path.startsWith("/shorts/")
                    || (path.equals("/watch") && hasNonEmptyVideoParameter(uri.getRawQuery()));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !supportedHost || !hasVideo) {
                errors.add(new FieldError("youtubeUrl", "Nhập link video YouTube hợp lệ hoặc để trống."));
            }
        } catch (IllegalArgumentException exception) {
            errors.add(new FieldError("youtubeUrl", "Nhập link video YouTube hợp lệ hoặc để trống."));
        }
    }

    private static boolean hasNonEmptyVideoParameter(String rawQuery) {
        if (rawQuery == null) return false;
        for (String parameter : rawQuery.split("&")) {
            int separator = parameter.indexOf('=');
            if (separator > 0 && "v".equals(parameter.substring(0, separator))
                    && separator < parameter.length() - 1) {
                return true;
            }
        }
        return false;
    }

    private static void validateMedia(List<RecipeMediaInput> media, List<FieldError> errors) {
        if (media == null || media.isEmpty()) return;
        if (media.size() > 5) errors.add(new FieldError("media", "Mỗi công thức được có tối đa 5 ảnh."));
        long covers = media.stream().filter(RecipeMediaInput::cover).count();
        if (covers != 1) errors.add(new FieldError("media", "Nếu có ảnh, hãy chọn đúng 1 ảnh bìa."));
        errors.add(new FieldError("media", "Đăng công thức kèm ảnh chưa được hỗ trợ. Hãy bỏ ảnh; upload ảnh sẽ được tích hợp sau FR-14."));
        for (int index = 0; index < media.size(); index++) {
            RecipeMediaInput item = media.get(index);
            if (item.mimeType() == null || !MediaType.accepts(item.mimeType().trim())) {
                errors.add(new FieldError("media[" + index + "].mimeType", "Ảnh phải có định dạng JPEG, PNG hoặc WebP."));
            }
            if (item.blobUrl() == null || item.blobUrl().isBlank()) {
                errors.add(new FieldError("media[" + index + "].blobUrl", "Thiếu tham chiếu ảnh đã được upload."));
            }
        }
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
