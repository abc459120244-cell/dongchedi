package com.dongchedi.car.dto;

import java.time.LocalDateTime;

import com.dongchedi.car.model.UserAccount;

/**
 * 对外暴露的用户信息（不含密码哈希）。
 */
public record UserView(
        Long id,
        String username,
        String nickname,
        String displayName,
        String role,
        String roleLabel,
        LocalDateTime createdAt) {

    public static UserView from(UserAccount user) {
        return new UserView(user.id(), user.username(), user.nickname(), user.displayName(),
                user.role().name(), user.role().label(), user.createdAt());
    }
}
