package tech.mamxanh.nutrition.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest;
import tech.mamxanh.nutrition.dto.request.SaveNutritionProfileRequest;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse;
import tech.mamxanh.nutrition.service.NutritionProfileService;

@RestController
@RequestMapping(path = "/api/v1/nutrition/profile", produces = MediaType.APPLICATION_JSON_VALUE)
public class NutritionController {
    private final NutritionProfileService service;

    public NutritionController(NutritionProfileService service) { this.service = service; }

    @GetMapping
    public NutritionProfileResponse getOwnProfile() { return service.getOwnProfile(); }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public NutritionProfileResponse saveOwnProfile(@Valid @RequestBody SaveNutritionProfileRequest request) {
        return service.saveOwnProfile(request);
    }

    @PostMapping(path = "/calculate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public NutritionProfileResponse calculateOwnResults(@Valid @RequestBody ConfirmNutritionEligibilityRequest confirmation) {
        return service.calculateOwnResults(confirmation);
    }
}
