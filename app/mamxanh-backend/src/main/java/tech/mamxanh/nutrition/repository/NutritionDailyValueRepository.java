package tech.mamxanh.nutrition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.nutrition.entity.NutritionDailyValueEntity;

public interface NutritionDailyValueRepository extends JpaRepository<NutritionDailyValueEntity, String> { }
