package com.dongchedi.car.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.dongchedi.car.model.UserRole;

/**
 * 登录相关配置。
 */
@Component
@ConfigurationProperties(prefix = "dongchedi.auth")
public class AuthProperties {

    /** 允许的连续登录失败次数，达到后锁定账号（按用户名计数） */
    private int maxFailedAttempts = 5;

    /** 锁定时长 */
    private Duration lockDuration = Duration.ofMinutes(5);

    /** 启动时初始化的演示账号 */
    private List<SeedUser> seedUsers = new ArrayList<>();

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }

    public List<SeedUser> getSeedUsers() {
        return seedUsers;
    }

    public void setSeedUsers(List<SeedUser> seedUsers) {
        this.seedUsers = seedUsers == null ? new ArrayList<>() : seedUsers;
    }

    /**
     * 演示账号配置。
     *
     * <p>这里的密码以明文书写仅用于教学演示：应用启动时会立刻用 BCrypt 哈希后再存入仓库，
     * 生产环境应改为由管理员初始化或从密钥管理服务读取。
     */
    public static class SeedUser {

        private String username;
        private String password;
        private String nickname;
        private UserRole role = UserRole.USER;
        private List<Long> favorites = new ArrayList<>();

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }

        public List<Long> getFavorites() {
            return favorites;
        }

        public void setFavorites(List<Long> favorites) {
            this.favorites = favorites == null ? new ArrayList<>() : favorites;
        }
    }
}
