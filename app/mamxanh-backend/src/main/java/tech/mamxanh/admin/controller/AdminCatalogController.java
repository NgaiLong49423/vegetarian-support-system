package tech.mamxanh.admin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
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
import tech.mamxanh.admin.dto.IngredientRecipeReferenceResponse;
import tech.mamxanh.admin.dto.IngredientCatalogOptionsResponse;
import tech.mamxanh.admin.dto.AdminIngredientResponse;
import tech.mamxanh.admin.service.AdminCatalogService;
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
public class AdminCatalogController {
    private final AdminCatalogService service;

    public AdminCatalogController(AdminCatalogService service) { this.service = service; }

    /** Backwards-compatible list operation used by the current admin client. */
    @GetMapping("/ingredients")
    public ApiResponse<List<IngredientResponse>> listIngredients(
            @RequestParam(required = false) @Size(max = 200) String query) {
        return ApiResponse.success("Lấy danh mục nguyên liệu thành công.", service.listIngredients(query));
    }

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

    @GetMapping("/ingredients/{ingredientId}")
    public ApiResponse<AdminIngredientResponse> getIngredient(@PathVariable @Min(1) long ingredientId) {
        return ApiResponse.success("Lấy chi tiết nguyên liệu thành công.", service.getIngredient(ingredientId));
    }

    @GetMapping("/ingredients/options")
    public ApiResponse<IngredientCatalogOptionsResponse> ingredientOptions() {
        return ApiResponse.success("Lấy lựa chọn danh mục nguyên liệu thành công.", service.ingredientOptions());
    }

    @GetMapping("/ingredients/{ingredientId}/recipes")
    public ApiResponse<List<IngredientRecipeReferenceResponse>> listRecipesUsingIngredient(
            @PathVariable @Min(1) long ingredientId) {
        return ApiResponse.success("Lấy công thức đang sử dụng nguyên liệu thành công.",
                service.listRecipesUsingIngredient(ingredientId));
    }

    @GetMapping("/ingredients/{ingredientId}/recipes/search")
    public ApiResponse<PageResponse<IngredientRecipeReferenceResponse>> searchRecipesUsingIngredient(
            @PathVariable @Min(1) long ingredientId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Lấy công thức đang sử dụng nguyên liệu thành công.",
                service.searchRecipesUsingIngredient(ingredientId, page, size));
    }

    @PostMapping("/ingredients")
    public ResponseEntity<ApiResponse<IngredientResponse>> createIngredient(
            @Valid @RequestBody IngredientSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo nguyên liệu thành công.", service.createIngredient(request)));
    }

    @PutMapping("/ingredients/{ingredientId}")
    public ApiResponse<IngredientResponse> updateIngredient(@PathVariable @Min(1) long ingredientId,
            @Valid @RequestBody IngredientSaveRequest request) {
        return ApiResponse.success("Cập nhật nguyên liệu thành công.", service.updateIngredient(ingredientId, request));
    }

    @PutMapping("/ingredients/{ingredientId}/nutrition")
    public ApiResponse<IngredientResponse> updateNutrition(@PathVariable @Min(1) long ingredientId,
            @Valid @RequestBody IngredientNutritionRequest request) {
        return ApiResponse.success("Cập nhật chỉ tiêu dinh dưỡng thành công.", service.updateNutrition(ingredientId, request));
    }

    @PatchMapping("/ingredients/{ingredientId}/nutrition-support")
    public ApiResponse<IngredientResponse> setNutritionSupport(@PathVariable @Min(1) long ingredientId,
            @Valid @RequestBody NutritionSupportUpdateRequest request) {
        return ApiResponse.success("Cập nhật hỗ trợ tính dinh dưỡng thành công.", service.setNutritionSupport(ingredientId, request));
    }

    @PatchMapping("/ingredients/{ingredientId}/status")
    public ApiResponse<IngredientResponse> setIngredientStatus(@PathVariable @Min(1) long ingredientId,
            @Valid @RequestBody CatalogStatusUpdateRequest request) {
        return ApiResponse.success("Cập nhật trạng thái nguyên liệu thành công.", service.setIngredientStatus(ingredientId, request));
    }

    @DeleteMapping("/ingredients/{ingredientId}")
    public void deleteIngredient(@PathVariable @Min(1) long ingredientId) { service.deleteIngredient(ingredientId); }

    @GetMapping("/units")
    public ApiResponse<List<UnitResponse>> listUnits() {
        return ApiResponse.success("Lấy danh mục đơn vị thành công.", service.listUnits());
    }

    @PostMapping("/units")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(@Valid @RequestBody UnitSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đơn vị thành công.", service.createUnit(request)));
    }

    @PutMapping("/units/{unitId}")
    public ApiResponse<UnitResponse> updateUnit(@PathVariable int unitId, @Valid @RequestBody UnitSaveRequest request) {
        return ApiResponse.success("Cập nhật đơn vị thành công.", service.updateUnit(unitId, request));
    }

    @PatchMapping("/units/{unitId}/status")
    public ApiResponse<UnitResponse> setUnitStatus(@PathVariable int unitId,
            @Valid @RequestBody CatalogStatusUpdateRequest request) {
        return ApiResponse.success("Cập nhật trạng thái đơn vị thành công.", service.setUnitStatus(unitId, request));
    }

    @DeleteMapping("/units/{unitId}")
    public void deleteUnit(@PathVariable int unitId) { service.deleteUnit(unitId); }

    @GetMapping("/ingredient-unit-conversions")
    public ApiResponse<List<ConversionResponse>> listConversions(@RequestParam(required = false) Long ingredientId) {
        return ApiResponse.success("Lấy bảng quy đổi thành công.", service.listConversions(ingredientId));
    }

    @PostMapping("/ingredients/{ingredientId}/unit-conversions/{unitId}")
    public ResponseEntity<ApiResponse<ConversionResponse>> createConversion(@PathVariable long ingredientId,
            @PathVariable int unitId, @Valid @RequestBody ConversionSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo tỷ lệ quy đổi thành công.",
                service.createConversion(ingredientId, unitId, request)));
    }

    @PutMapping("/ingredients/{ingredientId}/unit-conversions/{unitId}")
    public ApiResponse<ConversionResponse> updateConversion(@PathVariable long ingredientId, @PathVariable int unitId,
            @Valid @RequestBody ConversionSaveRequest request) {
        return ApiResponse.success("Cập nhật tỷ lệ quy đổi thành công.",
                service.updateConversion(ingredientId, unitId, request));
    }

    @PatchMapping("/ingredients/{ingredientId}/unit-conversions/{unitId}/status")
    public ApiResponse<ConversionResponse> setConversionStatus(@PathVariable long ingredientId,
            @PathVariable int unitId, @Valid @RequestBody CatalogStatusUpdateRequest request) {
        return ApiResponse.success("Cập nhật trạng thái tỷ lệ quy đổi thành công.",
                service.setConversionStatus(ingredientId, unitId, request));
    }
}
