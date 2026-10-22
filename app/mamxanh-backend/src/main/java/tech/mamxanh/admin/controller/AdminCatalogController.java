package tech.mamxanh.admin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.mamxanh.admin.dto.IngredientImageResponse;
import tech.mamxanh.admin.dto.IngredientRecipeReferenceResponse;
import tech.mamxanh.admin.dto.IngredientCatalogOptionsResponse;
import tech.mamxanh.admin.dto.AdminIngredientResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import tech.mamxanh.admin.service.AdminCatalogService;
import tech.mamxanh.admin.service.IngredientImageService;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.ApiResponse;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.nutrition.dto.request.CatalogStatusUpdateRequest;
import tech.mamxanh.nutrition.dto.request.ConversionSaveRequest;
import tech.mamxanh.nutrition.dto.request.IngredientNutritionRequest;
import tech.mamxanh.nutrition.dto.request.IngredientSaveRequest;
import tech.mamxanh.nutrition.dto.request.NutritionSupportUpdateRequest;
import tech.mamxanh.nutrition.dto.request.UnitSaveRequest;
import tech.mamxanh.nutrition.dto.response.ConversionResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminCatalogController {
    private final AdminCatalogService service;
    private final IngredientImageService ingredientImageService;

    public AdminCatalogController(AdminCatalogService service, IngredientImageService ingredientImageService) {
        this.service = service;
        this.ingredientImageService = ingredientImageService;
    }

    /** Backwards-compatible list operation used by the current admin client. */
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ingredient catalog in the `ApiResponse` envelope.")
    })
    @GetMapping("/ingredients")
    public ApiResponse<List<IngredientResponse>> listIngredients(
            @RequestParam(required = false) @Size(max = 200) String query) {
        return ApiResponse.success("Lấy danh mục nguyên liệu thành công.", service.listIngredients(query));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "A page of ingredients in the `ApiResponse` envelope.")
    })
    @GetMapping("/ingredients/search")
    public ApiResponse<PageResponse<AdminIngredientResponse>> searchIngredients(
            @RequestParam(required = false) @Size(max = 200) String query,
            @RequestParam(required = false) @Size(max = 100) String ingredientGroup,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean nutritionSupported,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Tìm danh mục nguyên liệu thành công.",
                service.searchIngredients(query, ingredientGroup, active, nutritionSupported, page, size));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ingredient detail."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND: no ingredient has this id.")
    })
    @GetMapping("/ingredients/{ingredientId}")
    public ApiResponse<AdminIngredientResponse> getIngredient(@PathVariable @Min(1) long ingredientId) {
        return ApiResponse.success("Lấy chi tiết nguyên liệu thành công.", service.getIngredient(ingredientId));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Allowed ingredient groups and nutrition sources.")
    })
    @GetMapping("/ingredients/options")
    public ApiResponse<IngredientCatalogOptionsResponse> ingredientOptions() {
        return ApiResponse.success("Lấy lựa chọn danh mục nguyên liệu thành công.", service.ingredientOptions());
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recipe Posts that use the ingredient."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND: no ingredient has this id.")
    })
    @GetMapping("/ingredients/{ingredientId}/recipes")
    public ApiResponse<List<IngredientRecipeReferenceResponse>> listRecipesUsingIngredient(
            @PathVariable @Min(1) long ingredientId) {
        return ApiResponse.success("Lấy công thức đang sử dụng nguyên liệu thành công.",
                service.listRecipesUsingIngredient(ingredientId));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "A page of Recipe Posts that use the ingredient."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND: no ingredient has this id.")
    })
    @GetMapping("/ingredients/{ingredientId}/recipes/search")
    public ApiResponse<PageResponse<IngredientRecipeReferenceResponse>> searchRecipesUsingIngredient(
            @PathVariable @Min(1) long ingredientId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Lấy công thức đang sử dụng nguyên liệu thành công.",
                service.searchRecipesUsingIngredient(ingredientId, page, size));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "The new ingredient."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_FAILED, UNSUPPORTED_INGREDIENT_GROUP or UNSUPPORTED_NUTRITION_SOURCE."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "INGREDIENT_NAME_EXISTS: an active ingredient already has this name.")
    })
    @PostMapping("/ingredients")
    public ResponseEntity<ApiResponse<IngredientResponse>> createIngredient(
            @Valid @RequestBody IngredientSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo nguyên liệu thành công.", service.createIngredient(request)));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The updated ingredient."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_FAILED, UNSUPPORTED_INGREDIENT_GROUP, UNSUPPORTED_NUTRITION_SOURCE or SOURCE_REQUIRED_WHEN_NUTRITION_SUPPORTED."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND: no ingredient has this id."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "INGREDIENT_NAME_EXISTS or NUTRITION_DATA_REQUIRED_WHILE_SUPPORTED.")
    })
    @PutMapping("/ingredients/{ingredientId}")
    public ApiResponse<IngredientResponse> updateIngredient(@PathVariable @Min(1) long ingredientId,
            @Valid @RequestBody IngredientSaveRequest request) {
        return ApiResponse.success("Cập nhật nguyên liệu thành công.", service.updateIngredient(ingredientId, request));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The uploaded ingredient image URL."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or UNSUPPORTED_IMAGE_TYPE."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "FILE_TOO_LARGE: image exceeds 2 MB.")
    })
    @PostMapping(value = "/ingredients/{ingredientId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<IngredientImageResponse> uploadIngredientImage(
            @PathVariable @Min(1) long ingredientId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Đã lưu ảnh minh họa nguyên liệu.",
                ingredientImageService.upload(ingredientId, file));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The ingredient with its new nutrition values."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND: no ingredient has this id."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "NUTRITION_DATA_REQUIRED_WHILE_SUPPORTED: a supported ingredient needs all nine values.")
    })
    @PutMapping("/ingredients/{ingredientId}/nutrition")
    public ApiResponse<IngredientResponse> updateNutrition(@PathVariable @Min(1) long ingredientId,
            @Valid @RequestBody IngredientNutritionRequest request) {
        return ApiResponse.success("Cập nhật chỉ tiêu dinh dưỡng thành công.", service.updateNutrition(ingredientId, request));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The ingredient with its new nutrition support state."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or UNSUPPORTED_NUTRITION_SOURCE."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND: no ingredient has this id."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "NUTRITION_VALUES_INCOMPLETE or NUTRITION_SOURCE_INCOMPLETE.")
    })
    @PatchMapping("/ingredients/{ingredientId}/nutrition-support")
    public ApiResponse<IngredientResponse> setNutritionSupport(@PathVariable @Min(1) long ingredientId,
            @Valid @RequestBody NutritionSupportUpdateRequest request) {
        return ApiResponse.success("Cập nhật hỗ trợ tính dinh dưỡng thành công.", service.setNutritionSupport(ingredientId, request));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The ingredient with its new status."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND: no ingredient has this id."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "INGREDIENT_NAME_EXISTS: reactivating would duplicate an active name.")
    })
    @PatchMapping("/ingredients/{ingredientId}/status")
    public ApiResponse<IngredientResponse> setIngredientStatus(@PathVariable @Min(1) long ingredientId,
            @Valid @RequestBody CatalogStatusUpdateRequest request) {
        return ApiResponse.success("Cập nhật trạng thái nguyên liệu thành công.", service.setIngredientStatus(ingredientId, request));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND: no ingredient has this id."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "HARD_DELETE_NOT_SUPPORTED: deactivate the ingredient instead.")
    })
    @DeleteMapping("/ingredients/{ingredientId}")
    public void deleteIngredient(@PathVariable @Min(1) long ingredientId) { service.deleteIngredient(ingredientId); }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unit catalog.")
    })
    @GetMapping("/units")
    public ApiResponse<List<UnitResponse>> listUnits() {
        return ApiResponse.success("Lấy danh mục đơn vị thành công.", service.listUnits());
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "The new unit."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "UNIT_CODE_EXISTS: the unit code is already used.")
    })
    @PostMapping("/units")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(@Valid @RequestBody UnitSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đơn vị thành công.", service.createUnit(request)));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The updated unit."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "UNIT_NOT_FOUND: no unit has this id."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "UNIT_CODE_EXISTS or UNIT_HAS_INGREDIENT_CONVERSIONS.")
    })
    @PutMapping("/units/{unitId}")
    public ApiResponse<UnitResponse> updateUnit(@PathVariable int unitId, @Valid @RequestBody UnitSaveRequest request) {
        return ApiResponse.success("Cập nhật đơn vị thành công.", service.updateUnit(unitId, request));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The unit with its new status."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "UNIT_NOT_FOUND: no unit has this id.")
    })
    @PatchMapping("/units/{unitId}/status")
    public ApiResponse<UnitResponse> setUnitStatus(@PathVariable int unitId,
            @Valid @RequestBody CatalogStatusUpdateRequest request) {
        return ApiResponse.success("Cập nhật trạng thái đơn vị thành công.", service.setUnitStatus(unitId, request));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "UNIT_NOT_FOUND: no unit has this id."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "HARD_DELETE_NOT_SUPPORTED: deactivate the unit instead.")
    })
    @DeleteMapping("/units/{unitId}")
    public void deleteUnit(@PathVariable int unitId) { service.deleteUnit(unitId); }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ingredient unit conversions.")
    })
    @GetMapping("/ingredient-unit-conversions")
    public ApiResponse<List<ConversionResponse>> listConversions(@RequestParam(required = false) Long ingredientId) {
        return ApiResponse.success("Lấy bảng quy đổi thành công.", service.listConversions(ingredientId));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "The new conversion."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND or UNIT_NOT_FOUND."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "CONVERSION_EXISTS, INGREDIENT_INACTIVE, UNIT_INACTIVE or MASS_CONVERSION_NOT_ALLOWED.")
    })
    @PostMapping("/ingredients/{ingredientId}/unit-conversions/{unitId}")
    public ResponseEntity<ApiResponse<ConversionResponse>> createConversion(@PathVariable long ingredientId,
            @PathVariable int unitId, @Valid @RequestBody ConversionSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo tỷ lệ quy đổi thành công.",
                service.createConversion(ingredientId, unitId, request)));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The updated conversion."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CONVERSION_NOT_FOUND or UNIT_NOT_FOUND."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "MASS_CONVERSION_NOT_ALLOWED: mass units use their fixed factor.")
    })
    @PutMapping("/ingredients/{ingredientId}/unit-conversions/{unitId}")
    public ApiResponse<ConversionResponse> updateConversion(@PathVariable long ingredientId, @PathVariable int unitId,
            @Valid @RequestBody ConversionSaveRequest request) {
        return ApiResponse.success("Cập nhật tỷ lệ quy đổi thành công.",
                service.updateConversion(ingredientId, unitId, request));
    }

    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The conversion with its new status."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CONVERSION_NOT_FOUND or UNIT_NOT_FOUND."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "MASS_CONVERSION_NOT_ALLOWED: mass units use their fixed factor.")
    })
    @PatchMapping("/ingredients/{ingredientId}/unit-conversions/{unitId}/status")
    public ApiResponse<ConversionResponse> setConversionStatus(@PathVariable long ingredientId,
            @PathVariable int unitId, @Valid @RequestBody CatalogStatusUpdateRequest request) {
        return ApiResponse.success("Cập nhật trạng thái tỷ lệ quy đổi thành công.",
                service.setConversionStatus(ingredientId, unitId, request));
    }
}
