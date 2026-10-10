package com.dongchedi.car.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证相关 Bean。
 *
 * <p>只使用 spring-security-crypto 提供的加密工具，不启用 Spring Security 过滤器链，
 * 因此不会影响现有 REST 接口与静态资源。
 */
@Configuration
public class AuthConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
