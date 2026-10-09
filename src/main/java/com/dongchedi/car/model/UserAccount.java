package com.dongchedi.car.model;

import java.time.LocalDateTime;

/**
 * 用户账号。
 *
 * <p>{@code passwordHash} 只存放 BCrypt 哈希，绝不明文保存，也不会序列化给前端
 * （对外统一使用 {@code UserView} DTO）。
 */
public record UserAccount(
        Long id,
        String username,
        String nickname,
        UserRole role,
        String passwordHash,
        LocalDateTime createdAt) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * 页面展示名：优先昵称，其次用户名。
     */
    public String displayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }
}
