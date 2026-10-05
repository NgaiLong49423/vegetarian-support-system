package tech.mamxanh.nutrition.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import tech.mamxanh.nutrition.entity.DietaryPreferenceEntity;

public interface DietaryPreferenceRepository extends JpaRepository<DietaryPreferenceEntity, Long> {

    /**
     * Lookup with a row lock held until the transaction ends. Concurrent sign-ins of one account
     * claim the Onboarding invitation one after another, so only the first can be shown it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from DietaryPreferenceEntity p where p.userId = :userId")
    Optional<DietaryPreferenceEntity> findByIdForUpdate(@Param("userId") Long userId);
}
