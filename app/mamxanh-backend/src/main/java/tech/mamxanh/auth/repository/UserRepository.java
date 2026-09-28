package tech.mamxanh.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.mamxanh.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailVerificationToken(String tokenHash);
}
