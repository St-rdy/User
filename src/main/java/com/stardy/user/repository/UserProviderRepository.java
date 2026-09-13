package com.stardy.user.repository;

import com.stardy.user.entity.User;
import com.stardy.user.entity.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface UserProviderRepository extends JpaRepository<UserProvider, Long> {
    boolean existsByProviderAndSocialId(String provider, String socialId);

    @EntityGraph(attributePaths = {"user", "user.role"})
    Optional<UserProvider> findByProviderAndSocialId(String provider, String socialId);

    Optional<UserProvider> findByUserAndProvider(User user, String provider);
    long countByUser(User user);
}
