package tech.mamxanh.nutrition.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mamxanh.nutrition.catalog")
public record NutritionCatalogProperties(List<String> allowedSources) {
    public NutritionCatalogProperties {
        allowedSources = allowedSources == null ? List.of() : List.copyOf(allowedSources);
    }
}
