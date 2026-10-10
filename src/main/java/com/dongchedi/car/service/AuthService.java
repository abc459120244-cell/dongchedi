package com.dongchedi.car.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dongchedi.car.config.AuthProperties;
import com.dongchedi.car.dto.RegisterRequest;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.model.UserRole;
import com.dongchedi.car.repository.UserRepository;
import com.dongchedi.car.support.SessionKeys;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 认证服务：注册、登录、退出与会话管理。
 *
 * <p>安全要点：
 * <ul>
 *   <li>密码使用 BCrypt 哈希存储，永不保存明文；</li>
 *   <li>用户名不存在时也会执行一次哈希比对，避免通过响应时间判断账号是否存在；</li>
 *   <li>连续登录失败达到阈值后按用户名临时锁定；</li>
 *   <li>登录成功后重建 Session（防会话固定攻击）。</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** 用户名：4~20 位字母、数字或下划线 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,20}$");
    private static final Pattern HAS_LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");

    /** 登录会话有效期：30 分钟 */
    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;

    /** 按用户名记录连续失败次数与锁定截止时间 */
    private final Map<String, Failure> failures = new ConcurrentHashMap<>();

    /** 用户名不存在时用来消耗同等时间的占位哈希 */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.dummyHash = passwordEncoder.encode("dongchedi-placeholder-password");
    }

    /**
     * 注册新用户，注册成功后自动登录。
     */
    public UserAccount register(HttpServletRequest request, RegisterRequest registerRequest) {
        String username = validateUsername(registerRequest.getUsername());
        validatePassword(registerRequest.getPassword());

        if (userRepository.existsByUsername(username)) {
            throw new BizException(HttpStatus.CONFLICT, "用户名已被注册：" + username);
        }

        String hash = passwordEncoder.encode(registerRequest.getPassword());
        UserAccount user = userRepository.create(username, registerRequest.getNickname(), UserRole.USER, hash);
        log.info("新用户注册成功: {}", user.username());
        bindSession(request, user);
        return user;
    }

    /**
     * 校验用户名密码并建立登录会话。
     */
    public UserAccount login(HttpServletRequest request, String username, String rawPassword) {
        String key = username == null ? "" : username.trim().toLowerCase(java.util.Locale.ROOT);
        assertNotLocked(key);

        Optional<UserAccount> found = userRepository.findByUsername(username);
        String hash = found.map(UserAccount::passwordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(rawPassword == null ? "" : rawPassword, hash);

        if (found.isEmpty() || !matches) {
            recordFailure(key);
            log.warn("登录失败: username={}", username);
            throw new BizException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        failures.remove(key);
        UserAccount user = found.get();
        bindSession(request, user);
        log.info("用户登录成功: {}", user.username());
        return user;
    }

    /**
     * 退出登录：销毁会话。
     */
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * 读取当前登录用户，未登录返回空。
     */
    public Optional<UserAccount> currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object attribute = session == null ? null : session.getAttribute(SessionKeys.LOGIN_USER_ID);
        if (attribute instanceof Long userId) {
            return userRepository.findById(userId);
        }
        return Optional.empty();
    }

    private void bindSession(HttpServletRequest request, UserAccount user) {
        // 防会话固定攻击：登录前后不使用同一个 Session
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionKeys.LOGIN_USER_ID, user.id());
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
    }

    private String validateUsername(String raw) {
        String username = raw == null ? "" : raw.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw BizException.badRequest("用户名需为 4~20 位字母、数字或下划线");
        }
        return username;
    }

    private void validatePassword(String password) {
        String value = password == null ? "" : password;
        if (value.length() < 6 || value.length() > 32) {
            throw BizException.badRequest("密码长度需为 6~32 位");
        }
        if (!HAS_LETTER.matcher(value).find() || !HAS_DIGIT.matcher(value).find()) {
            throw BizException.badRequest("密码需同时包含字母和数字");
        }
    }

    private void assertNotLocked(String key) {
        Failure failure = failures.get(key);
        if (failure == null || failure.lockedUntil() == null) {
            return;
        }
        Instant now = Instant.now();
        if (now.isBefore(failure.lockedUntil())) {
            long seconds = Math.max(Duration.between(now, failure.lockedUntil()).toSeconds(), 1);
            throw new BizException(HttpStatus.TOO_MANY_REQUESTS,
                    "登录失败次数过多，请 " + seconds + " 秒后重试");
        }
        failures.remove(key);
    }

    private void recordFailure(String key) {
        failures.compute(key, (name, old) -> {
            int count = (old == null ? 0 : old.count()) + 1;
            if (count >= properties.getMaxFailedAttempts()) {
                log.warn("账号 {} 连续登录失败 {} 次，锁定 {} 分钟", name, count,
                        properties.getLockDuration().toMinutes());
                return new Failure(0, Instant.now().plus(properties.getLockDuration()));
            }
            return new Failure(count, null);
        });
    }

    /**
     * 连续失败记录。
     */
    private record Failure(int count, Instant lockedUntil) {
    }
}
