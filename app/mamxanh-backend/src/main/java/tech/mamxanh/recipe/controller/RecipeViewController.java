package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.request.RecipeViewRequest;
import tech.mamxanh.recipe.dto.response.RecipeViewResponse;
import tech.mamxanh.recipe.service.RecipeViewService;

@RestController
@RequestMapping(path = "/api/v1/recipes", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecipeViewController {
    private final RecipeViewService service;

    public RecipeViewController(RecipeViewService service) { this.service = service; }

    @PostMapping(path = "/{recipeId}/views", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ghi nhận lượt xem công thức công khai",
            description = "Guest gửi anonymousViewerId dạng UUID; Member gửi {} và dùng danh tính từ Bearer. "
                    + "Lượt lặp trong 30 phút (kể cả đúng mốc 30 phút) trả counted=false, không kéo dài cửa sổ.",
            security = {@SecurityRequirement, @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lượt xem đã ghi nhận hoặc đã khử trùng lặp."),
            @ApiResponse(responseCode = "400", description = "ID hoặc định danh Guest không hợp lệ.",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Bearer token không hợp lệ hoặc hết hạn.",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Tài khoản mang Bearer token bị khóa.",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Công thức không tồn tại hoặc không công khai.",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Chưa lấy được khóa ghi nhận lượt xem; có thể thử lại.",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RecipeViewResponse> record(@PathVariable @Min(1) long recipeId,
            @Valid @RequestBody RecipeViewRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt == null ? null : Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.record(recipeId, userId, request.anonymousViewerId()));
    }
}
