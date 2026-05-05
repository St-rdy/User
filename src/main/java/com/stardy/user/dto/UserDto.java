package com.stardy.user.dto;

import com.stardy.user.entity.User;
import lombok.Getter;

import java.util.Map;

@Getter
public class UserDto {

    private final Long id;
    private final String email;
    private final String name;
    private final String nickname;
    private final String profileImageUrl;
    private final Map<String, Object> domain;
    private final String status;
    private final String roleId;

    public UserDto(Long id, String email, String name, String nickname, String profileImageUrl, Map<String, Object> domain, String status, String roleId) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.domain = domain;
        this.status = status;
        this.roleId = roleId;
    }

    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getDomain(),
                user.getStatus(),
                user.getRole().getRoleId()
        );
    }

}
