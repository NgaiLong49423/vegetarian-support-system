package tech.mamxanh.nutrition.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.mamxanh.nutrition.entity.UserIngredientPreferenceEntity;

public interface UserIngredientPreferenceRepository extends JpaRepository<UserIngredientPreferenceEntity, Long> {

    List<UserIngredientPreferenceEntity> findByUserIdOrderByIdAsc(Long userId);

    /**
     * Bulk delete that runs immediately, so the replacement rows can reuse the same names without
     * hitting the per-user unique indexes (Hibernate would otherwise flush inserts before deletes).
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from UserIngredientPreferenceEntity p where p.userId = :userId")
    void deleteAllOfUser(@Param("userId") Long userId);
}
