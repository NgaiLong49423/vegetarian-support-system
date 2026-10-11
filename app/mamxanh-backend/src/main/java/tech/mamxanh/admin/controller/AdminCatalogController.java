package tech.mamxanh.admin.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import tech.mamxanh.common.response.ApiResponse;
import tech.mamxanh.nutrition.dto.request.CatalogStatusUpdateRequest;
import tech.mamxanh.nutrition.dto.request.ConversionSaveRequest;
import tech.mamxanh.nutrition.dto.request.IngredientSaveRequest;
import tech.mamxanh.nutrition.dto.request.UnitSaveRequest;
import tech.mamxanh.nutrition.dto.response.ConversionResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.service.IngredientCatalogService;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {
    private final IngredientCatalogService catalogService;
    public AdminCatalogController(IngredientCatalogService catalogService) { this.catalogService = catalogService; }

    @GetMapping("/ingredients")
    public ApiResponse<List<IngredientResponse>> listIngredients(@RequestParam(required = false) String query) { return ApiResponse.success("Lấy danh mục nguyên liệu thành công.", catalogService.listIngredients(query)); }
    @PostMapping("/ingredients")
    public ResponseEntity<ApiResponse<IngredientResponse>> createIngredient(@Valid @RequestBody IngredientSaveRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo nguyên liệu thành công.", catalogService.createIngredient(request))); }
    @PutMapping("/ingredients/{ingredientId}")
    public ApiResponse<IngredientResponse> updateIngredient(@PathVariable long ingredientId, @Valid @RequestBody IngredientSaveRequest request) { return ApiResponse.success("Cập nhật nguyên liệu thành công.", catalogService.updateIngredient(ingredientId, request)); }
    @PatchMapping("/ingredients/{ingredientId}/status")
    public ApiResponse<IngredientResponse> setIngredientStatus(@PathVariable long ingredientId, @Valid @RequestBody CatalogStatusUpdateRequest request) { return ApiResponse.success("Cập nhật trạng thái nguyên liệu thành công.", catalogService.setIngredientStatus(ingredientId, request)); }
    @DeleteMapping("/ingredients/{ingredientId}")
    public void deleteIngredient(@PathVariable long ingredientId) { catalogService.rejectHardDelete("nguyên liệu"); }

    @GetMapping("/units")
    public ApiResponse<List<UnitResponse>> listUnits() { return ApiResponse.success("Lấy danh mục đơn vị thành công.", catalogService.listUnits()); }
    @PostMapping("/units")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(@Valid @RequestBody UnitSaveRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo đơn vị thành công.", catalogService.createUnit(request))); }
    @PutMapping("/units/{unitId}")
    public ApiResponse<UnitResponse> updateUnit(@PathVariable int unitId, @Valid @RequestBody UnitSaveRequest request) { return ApiResponse.success("Cập nhật đơn vị thành công.", catalogService.updateUnit(unitId, request)); }
    @PatchMapping("/units/{unitId}/status")
    public ApiResponse<UnitResponse> setUnitStatus(@PathVariable int unitId, @Valid @RequestBody CatalogStatusUpdateRequest request) { return ApiResponse.success("Cập nhật trạng thái đơn vị thành công.", catalogService.setUnitStatus(unitId, request)); }
    @DeleteMapping("/units/{unitId}")
    public void deleteUnit(@PathVariable int unitId) { catalogService.rejectHardDelete("đơn vị"); }

    @GetMapping("/ingredient-unit-conversions")
    public ApiResponse<List<ConversionResponse>> listConversions(@RequestParam(required = false) Long ingredientId) { return ApiResponse.success("Lấy bảng quy đổi thành công.", catalogService.listConversions(ingredientId)); }
    @PostMapping("/ingredients/{ingredientId}/unit-conversions/{unitId}")
    public ResponseEntity<ApiResponse<ConversionResponse>> createConversion(@PathVariable long ingredientId, @PathVariable int unitId, @Valid @RequestBody ConversionSaveRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo tỷ lệ quy đổi thành công.", catalogService.createConversion(ingredientId, unitId, request))); }
    @PutMapping("/ingredients/{ingredientId}/unit-conversions/{unitId}")
    public ApiResponse<ConversionResponse> updateConversion(@PathVariable long ingredientId, @PathVariable int unitId, @Valid @RequestBody ConversionSaveRequest request) { return ApiResponse.success("Cập nhật tỷ lệ quy đổi thành công.", catalogService.updateConversion(ingredientId, unitId, request)); }
    @PatchMapping("/ingredients/{ingredientId}/unit-conversions/{unitId}/status")
    public ApiResponse<ConversionResponse> setConversionStatus(@PathVariable long ingredientId, @PathVariable int unitId, @Valid @RequestBody CatalogStatusUpdateRequest request) { return ApiResponse.success("Cập nhật trạng thái tỷ lệ quy đổi thành công.", catalogService.setConversionStatus(ingredientId, unitId, request)); }
}
