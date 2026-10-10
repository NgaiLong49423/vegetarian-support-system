package tech.mamxanh.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.mamxanh.auth.entity.MemberProfileEntity;

public interface MemberProfileRepository extends JpaRepository<MemberProfileEntity, Long> {
}
