package com.dongchedi.car.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;

import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.model.UserRole;

/**
 * 内存用户仓库。
 *
 * <p>演示项目不依赖数据库，用户数据保存在内存中（重启后重置）；
 * 保留 {@link UserRepository} 接口，便于后续替换为 JPA / MyBatis 实现。
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, UserAccount> usersById = new ConcurrentHashMap<>();
    private final Map<String, Long> idByUsername = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Optional<UserAccount> findById(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        Long id = idByUsername.get(normalize(username));
        return id == null ? Optional.empty() : findById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return username != null && idByUsername.containsKey(normalize(username));
    }

    @Override
    public UserAccount create(String username, String nickname, UserRole role, String passwordHash) {
        String display = username == null ? "" : username.trim();
        String key = normalize(display);
        if (key.isEmpty()) {
            throw BizException.badRequest("用户名不能为空");
        }

        long id = sequence.incrementAndGet();
        UserAccount account = new UserAccount(id, display,
                nickname == null || nickname.isBlank() ? null : nickname.trim(),
                role, passwordHash, LocalDateTime.now());

        // 用 putIfAbsent 保证并发注册时同名用户只会成功一个
        Long existing = idByUsername.putIfAbsent(key, id);
        if (existing != null) {
            throw new BizException(HttpStatus.CONFLICT, "用户名已被注册：" + display);
        }
        usersById.put(id, account);
        return account;
    }

    @Override
    public List<UserAccount> findAll() {
        return usersById.values().stream()
                .sorted(Comparator.comparingLong(UserAccount::id))
                .toList();
    }

    @Override
    public long count() {
        return usersById.size();
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
