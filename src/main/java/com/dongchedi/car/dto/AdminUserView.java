package com.dongchedi.car.dto;

import java.time.LocalDateTime;

import com.dongchedi.car.model.UserAccount;

/**
 * 管理员视角的用户信息，附带收藏数量等统计字段。
 */
public record AdminUserView(
        Long id,
        String username,
        String nickname,
        String role,
        String roleLabel,
        LocalDateTime createdAt,
        int favoriteCount) {

    public static AdminUserView from(UserAccount user, int favoriteCount) {
        return new AdminUserView(user.id(), user.username(), user.nickname(), user.role().name(),
                user.role().label(), user.createdAt(), favoriteCount);
    }
}
