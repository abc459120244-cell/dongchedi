package com.dongchedi.car.entity;

import java.time.LocalDateTime;

import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.model.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户实体（数据库表 user_account，涵盖普通用户 / 商家 / 管理员三种角色）。
 */
@Entity
@Table(name = "user_account", indexes = @Index(name = "idx_user_username", columnList = "username", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String username;

    @Column(length = 32)
    private String nickname;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserAccount toModel() {
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role);
        } catch (Exception ex) {
            userRole = UserRole.USER;
        }
        return new UserAccount(id, username, nickname, userRole, passwordHash, createdAt);
    }

    public static UserEntity fromModel(UserAccount account) {
        UserEntity entity = new UserEntity();
        entity.setUsername(account.username());
        entity.setNickname(account.nickname());
        entity.setRole(account.role().name());
        entity.setPasswordHash(account.passwordHash());
        return entity;
    }
}
