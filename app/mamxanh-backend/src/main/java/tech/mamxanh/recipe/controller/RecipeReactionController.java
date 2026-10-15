package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.request.SetRecipeReactionRequest;
import tech.mamxanh.recipe.dto.response.RecipeReactionResponse;
import tech.mamxanh.recipe.service.RecipeReactionService;

@RestController
@RequestMapping("/api/v1/recipes/{recipeId}/reaction")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lựa chọn hiện tại và thống kê bình chọn", content = @Content(
                mediaType = "application/json", schema = @Schema(implementation = RecipeReactionResponse.class))),
        @ApiResponse(responseCode = "400", description = "ID hoặc loại phản hồi không hợp lệ", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Không đủ quyền; tài khoản khóa hoặc tác giả tự bình chọn", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Công thức không tồn tại hoặc không còn công khai", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class RecipeReactionController {
    private final RecipeReactionService service;

    public RecipeReactionController(RecipeReactionService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Đọc phản hồi của Member hiện tại và thống kê bình chọn")
    public ResponseEntity<RecipeReactionResponse> get(@PathVariable @Min(1) long recipeId) {
        return privateResponse(service.get(recipeId));
    }

    @PutMapping
    @Operation(summary = "Đặt hoặc chuyển Like/Dislike", description = "PUT cùng lựa chọn giữ nguyên phản hồi; bấm lại nút đang chọn trên UI gọi DELETE để hủy.")
    public ResponseEntity<RecipeReactionResponse> set(@PathVariable @Min(1) long recipeId,
            @Valid @RequestBody SetRecipeReactionRequest request) {
        return privateResponse(service.set(recipeId, request.reactionType()));
    }

    @DeleteMapping
    @Operation(summary = "Hủy phản hồi của Member hiện tại", description = "Trả lựa chọn null và thống kê mới; hủy lặp không thay đổi dữ liệu.")
    public ResponseEntity<RecipeReactionResponse> remove(@PathVariable @Min(1) long recipeId) {
        return privateResponse(service.remove(recipeId));
    }

    private ResponseEntity<RecipeReactionResponse> privateResponse(RecipeReactionResponse response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
