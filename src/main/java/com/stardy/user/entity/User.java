package com.stardy.user.entity;

import jakarta.persistence.Entity;

@Entity
public class User {
    private String email;
    private String role;

    public User(String email, String role) {
        this.email = email;
        this.role = role;
    }
}
