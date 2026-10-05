package tech.mamxanh.nutrition.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.mamxanh.nutrition.entity.DietaryPreferenceEntity;

public interface DietaryPreferenceRepository extends JpaRepository<DietaryPreferenceEntity, Long> {
}
