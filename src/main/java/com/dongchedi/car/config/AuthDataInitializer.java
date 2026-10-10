package com.dongchedi.car.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.FavoriteRepository;
import com.dongchedi.car.repository.UserRepository;

/**
 * 启动时初始化演示账号与其收藏数据。
 *
 * <p>账号信息来自 {@code dongchedi.auth.seed-users} 配置，密码在写入仓库前会做 BCrypt 哈希。
 */
@Component
public class AuthDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthDataInitializer.class);

    private final AuthProperties properties;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthDataInitializer(AuthProperties properties, UserRepository userRepository,
                               FavoriteRepository favoriteRepository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        for (AuthProperties.SeedUser seed : properties.getSeedUsers()) {
            if (seed.getUsername() == null || seed.getUsername().isBlank()
                    || seed.getPassword() == null || seed.getPassword().isBlank()) {
                log.warn("跳过不合法的演示账号配置: {}", seed.getUsername());
                continue;
            }
            if (userRepository.existsByUsername(seed.getUsername())) {
                continue;
            }
            UserAccount user = userRepository.create(seed.getUsername(), seed.getNickname(), seed.getRole(),
                    passwordEncoder.encode(seed.getPassword()));
            List<Long> favorites = seed.getFavorites();
            for (Long carId : favorites) {
                favoriteRepository.add(user.id(), carId);
            }
            created++;
        }
        if (created > 0) {
            log.info("已初始化 {} 个演示账号（用户名与密码见 README「用户登录系统」章节）", created);
        }
    }
}
