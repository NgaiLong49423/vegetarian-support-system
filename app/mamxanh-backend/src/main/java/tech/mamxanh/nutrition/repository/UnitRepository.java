package tech.mamxanh.nutrition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.nutrition.entity.UnitEntity;

public interface UnitRepository extends JpaRepository<UnitEntity, Integer> {
    boolean existsByCodeIgnoreCase(String code);
    java.util.List<UnitEntity> findByActiveTrue();
}
