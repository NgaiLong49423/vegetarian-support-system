package tech.mamxanh.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.mamxanh.auth.entity.MemberProfileEntity;

public interface MemberProfileRepository extends JpaRepository<MemberProfileEntity, Long> {

    @Query("""
            select profile from MemberProfileEntity profile
            where profile.role = tech.mamxanh.auth.entity.Role.EXPERT
              and profile.accountStatus = tech.mamxanh.auth.entity.AccountStatus.ACTIVE
              and (:query is null or lower(profile.displayName) like lower(concat('%', :query, '%'))
                   or lower(profile.bio) like lower(concat('%', :query, '%')))
            """)
    Page<MemberProfileEntity> searchActiveExperts(@Param("query") String query, Pageable pageable);
}
