package tech.mamxanh.recipe.service;

import java.net.URI;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import tech.mamxanh.recipe.dto.response.RecipeFormOptionsResponse.Choice;
import tech.mamxanh.recipe.dto.response.RecipeFormOptionsResponse.UnitOption;
import tech.mamxanh.recipe.entity.RecipeAuthorReferenceEntity;
import tech.mamxanh.recipe.entity.RecipeCodes.Difficulty;
import tech.mamxanh.recipe.entity.RecipeCodes.DishCategory;
import tech.mamxanh.recipe.entity.RecipeCodes.MediaType;
import tech.mamxanh.recipe.entity.RecipeCodes.VegetarianType;
import tech.mamxanh.recipe.entity.RecipeIngredientEntity;
import tech.mamxanh.recipe.entity.RecipeIngredientReferenceEntity;
import tech.mamxanh.recipe.entity.RecipeMediaEntity;
import tech.mamxanh.recipe.entity.RecipePostEntity;
import tech.mamxanh.recipe.entity.RecipeUnitReferenceEntity;
import tech.mamxanh.recipe.repository.RecipeAuthorReferenceRepository;
import tech.mamxanh.recipe.repository.RecipeConversionRepository;
import tech.mamxanh.recipe.repository.RecipeIngredientReferenceRepository;
import tech.mamxanh.recipe.repository.RecipeIngredientRepository;
import tech.mamxanh.recipe.repository.RecipeMediaRepository;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.RecipeUnitReferenceRepository;
import tech.mamxanh.recipe.service.RecipeValidationException.FieldError;

@Service
public class RecipeService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
    private final RecipePostRepository recipeRepository;
    private final RecipeIngredientRepository ingredientRepository;
    private final RecipeMediaRepository mediaRepository;
    private final RecipeIngredientReferenceRepository ingredientReferenceRepository;
    private final RecipeUnitReferenceRepository unitReferenceRepository;
    private final RecipeConversionRepository conversionRepository;
    private final RecipeAuthorReferenceRepository authorRepository;
    private final Clock clock;

    @Autowired
    public RecipeService(RecipePostRepository recipeRepository,
            RecipeIngredientRepository ingredientRepository,
            RecipeMediaRepository mediaRepository,
            RecipeIngredientReferenceRepository ingredientReferenceRepository,
            RecipeUnitReferenceRepository unitReferenceRepository,
            RecipeConversionRepository conversionRepository,
            RecipeAuthorReferenceRepository authorRepository, Clock clock) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.mediaRepository = mediaRepository;
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
                validateMassQuantity(input.quantity(), unit, field, errors);
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

        List<RecipeMediaInput> media = request.media() == null ? List.of() : request.media();
        List<RecipeMediaEntity> recipeMedia = new ArrayList<>(media.size());
        for (int index = 0; index < media.size(); index++) {
            RecipeMediaInput input = media.get(index);
            RecipeMediaEntity item = new RecipeMediaEntity();
            item.setRecipeId(saved.getId());
            item.setBlobUrl(input.blobUrl().trim());
            item.setMimeType(input.mimeType().toLowerCase());
            item.setDisplayOrder(index + 1);
            item.setCover(input.cover());
            recipeMedia.add(item);
        }
        if (!recipeMedia.isEmpty()) mediaRepository.saveAll(recipeMedia);

        return new CreateRecipeResponse(saved.getId(), saved.getTitle(), saved.getStatus(), publishedAt);
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

    private static void validateMassQuantity(BigDecimal quantity, RecipeUnitReferenceEntity unit,
            String field, List<FieldError> errors) {
        if (quantity == null) return;
        String message = massQuantityError(quantity, unit.getCode());
        if (message != null) {
            errors.add(new FieldError(field + ".quantity", message));
        }
    }

    static String massQuantityError(BigDecimal quantity, String unitCode) {
        if (quantity == null) return null;
        BigDecimal grams;
        if ("g".equalsIgnoreCase(unitCode)) {
            if (quantity.stripTrailingZeros().scale() > 0) {
                return "Số lượng đơn vị g phải là số nguyên bội số của 100 g (100, 200, 500...).";
            }
            grams = quantity;
        } else if ("kg".equalsIgnoreCase(unitCode)) {
            grams = quantity.multiply(THOUSAND);
        } else {
            return null;
        }
        if (grams.remainder(HUNDRED).compareTo(BigDecimal.ZERO) != 0) {
            if ("g".equalsIgnoreCase(unitCode)) {
                return "Số lượng đơn vị g phải là bội số của 100 g (100, 200, 500...).";
            }
            return "Khối lượng sau khi đổi ra gram phải chia hết cho 100 g.";
        }
        return null;
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
                    || (path.equals("/watch") && uri.getQuery() != null && uri.getQuery().matches(".*(?:^|&)v=[^&]+.*"));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !supportedHost || !hasVideo) {
                errors.add(new FieldError("youtubeUrl", "Nhập link video YouTube hợp lệ hoặc để trống."));
            }
        } catch (IllegalArgumentException exception) {
            errors.add(new FieldError("youtubeUrl", "Nhập link video YouTube hợp lệ hoặc để trống."));
        }
    }

    private static void validateMedia(List<RecipeMediaInput> media, List<FieldError> errors) {
        if (media == null || media.isEmpty()) return;
        if (media.size() > 5) errors.add(new FieldError("media", "Mỗi công thức được có tối đa 5 ảnh."));
        long covers = media.stream().filter(RecipeMediaInput::cover).count();
        if (covers != 1) errors.add(new FieldError("media", "Nếu có ảnh, hãy chọn đúng 1 ảnh bìa."));
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
