package com.dongchedi.car.repository;

import java.util.List;
import java.util.Optional;

import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.model.UserRole;

/**
 * 用户数据访问接口。
 */
public interface UserRepository {

    Optional<UserAccount> findById(Long id);

    /**
     * 按用户名查询，用户名不区分大小写。
     */
    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * 创建用户，用户名重复时抛业务异常。
     */
    UserAccount create(String username, String nickname, UserRole role, String passwordHash);

    List<UserAccount> findAll();

    long count();
}
