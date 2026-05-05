package com.stardy.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true, length = 20)
    private String roleId;

    @Column(nullable = false, length = 50)
    private String name; // "일반 사용자", "관리자" 등

    @Column(length = 255)
    private String description;

    public Role(String roleId, String name) {
        this(roleId, name, null);
    }

    public Role(String roleId, String name, String description) {
        this.roleId = roleId;
        this.name = name;
        this.description = description;
    }
}
