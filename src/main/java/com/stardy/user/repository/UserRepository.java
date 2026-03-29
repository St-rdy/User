package com.stardy.user.repository;

import com.stardy.user.entity.User;

public interface UserRepository {
    String getRole(String email);
    void save(User user);
}
