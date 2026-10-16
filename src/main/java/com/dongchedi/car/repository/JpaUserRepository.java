package com.dongchedi.car.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.dongchedi.car.entity.UserEntity;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.model.UserRole;
import com.dongchedi.car.repository.jpa.UserJpaRepository;

/**
 * 基于数据库的用户仓库实现，用户名唯一约束由数据库保证。
 */
@Repository
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    public JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UserAccount> findById(Long id) {
        return id == null ? Optional.empty() : jpa.findById(id).map(UserEntity::toModel);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return jpa.findByUsernameIgnoreCase(username.trim()).map(UserEntity::toModel);
    }

    @Override
    public boolean existsByUsername(String username) {
        return username != null && jpa.existsByUsernameIgnoreCase(username.trim());
    }

    @Override
    public UserAccount create(String username, String nickname, UserRole role, String passwordHash) {
        String display = username == null ? "" : username.trim();
        if (display.isEmpty()) {
            throw BizException.badRequest("用户名不能为空");
        }
        if (existsByUsername(display)) {
            throw new BizException(HttpStatus.CONFLICT, "用户名已被注册：" + display);
        }
        UserEntity entity = new UserEntity();
        entity.setUsername(display);
        entity.setNickname(nickname == null || nickname.isBlank() ? null : nickname.trim());
        entity.setRole(role.name());
        entity.setPasswordHash(passwordHash);
        try {
            return jpa.save(entity).toModel();
        } catch (DataIntegrityViolationException ex) {
            // 并发注册同名用户时由数据库唯一索引兜底
            throw new BizException(HttpStatus.CONFLICT, "用户名已被注册：" + display);
        }
    }

    @Override
    public List<UserAccount> findAll() {
        return jpa.findAll().stream().map(UserEntity::toModel).toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
