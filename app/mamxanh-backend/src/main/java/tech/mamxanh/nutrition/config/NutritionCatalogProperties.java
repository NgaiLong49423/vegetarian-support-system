package tech.mamxanh.nutrition.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "mamxanh.nutrition.catalog")
public record NutritionCatalogProperties(List<String> allowedSources, List<String> allowedGroups) {
    public NutritionCatalogProperties(List<String> allowedSources) {
        this(allowedSources, List.of());
    }

    @ConstructorBinding
    public NutritionCatalogProperties {
        allowedSources = allowedSources == null ? List.of() : List.copyOf(allowedSources);
        allowedGroups = allowedGroups == null ? List.of() : List.copyOf(allowedGroups);
    }
}
