package tech.mamxanh.mealplan.controller;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.service.MealPlanReadService;

@RestController
@RequestMapping("/api/v1/meal-plans")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MealPlanReadController {
    private final MealPlanReadService service;

    public MealPlanReadController(MealPlanReadService service) {
        this.service = service;
    }

    @GetMapping
    public MealPlanWeekResponse getWeek(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        return service.getWeek(weekStartDate);
    }
}
