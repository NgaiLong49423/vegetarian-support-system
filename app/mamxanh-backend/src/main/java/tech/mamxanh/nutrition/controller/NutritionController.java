package tech.mamxanh.nutrition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest;
import tech.mamxanh.nutrition.dto.request.SaveNutritionProfileRequest;
import tech.mamxanh.nutrition.dto.request.UpdateNutritionEligibilityRequest;
import tech.mamxanh.nutrition.dto.response.NutritionEligibilityResponse;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse;
import tech.mamxanh.nutrition.service.NutritionProfileService;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequestMapping(path = "/api/v1/nutrition/profile", produces = MediaType.APPLICATION_JSON_VALUE)
public class NutritionController {
    private final NutritionProfileService service;

    public NutritionController(NutritionProfileService service) { this.service = service; }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Eligibility confirmation required or nutrition access disabled",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NutritionProfileResponse getOwnProfile() { return service.getOwnProfile(); }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid profile or consent",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Eligibility confirmation required or nutrition access disabled",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Profile outside the FR-35 support range",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NutritionProfileResponse saveOwnProfile(@Valid @RequestBody SaveNutritionProfileRequest request) {
        return service.saveOwnProfile(request);
    }

    @PostMapping(path = "/calculate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid calculation confirmation",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Eligibility confirmation required or nutrition access disabled",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Nutrition profile is required before calculation",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Profile outside the FR-35 support range",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NutritionProfileResponse calculateOwnResults(@Valid @RequestBody ConfirmNutritionEligibilityRequest confirmation) {
        return service.calculateOwnResults(confirmation);
    }

    @GetMapping("/eligibility")
    @Operation(summary = "Read current nutrition eligibility")
    @ApiResponses({
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Active Member account required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NutritionEligibilityResponse getOwnEligibility() {
        return service.getOwnEligibility();
    }

    @PutMapping(path = "/eligibility", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Confirm current nutrition eligibility")
    @ApiResponses({
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid confirmation status",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Active Member account required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NutritionEligibilityResponse updateOwnEligibility(@Valid @RequestBody UpdateNutritionEligibilityRequest request) {
        return service.updateOwnEligibility(request);
    }
}
