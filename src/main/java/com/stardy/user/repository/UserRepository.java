package com.stardy.user.repository;

import com.stardy.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);
    List<User> findByIdIn(List<Long> userIds);
    Optional<User> findByNickname(String nickname);
}
