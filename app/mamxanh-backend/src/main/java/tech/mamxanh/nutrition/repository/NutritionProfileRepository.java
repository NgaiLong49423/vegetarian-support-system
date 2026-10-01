package tech.mamxanh.nutrition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.nutrition.entity.NutritionProfileEntity;

public interface NutritionProfileRepository extends JpaRepository<NutritionProfileEntity, Long> {
}
