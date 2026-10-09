package com.dongchedi.car.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.dongchedi.car.security.AdminRequiredInterceptor;
import com.dongchedi.car.security.LoginRequiredInterceptor;

/**
 * 注册登录 / 管理员拦截器。
 *
 * <p>公开接口（车型筛选、推荐、统计、健康检查、注册登录）不做拦截，
 * 只有用户私有数据与后台管理接口需要登录。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginRequiredInterceptor loginRequiredInterceptor;
    private final AdminRequiredInterceptor adminRequiredInterceptor;

    public WebMvcConfig(LoginRequiredInterceptor loginRequiredInterceptor,
                        AdminRequiredInterceptor adminRequiredInterceptor) {
        this.loginRequiredInterceptor = loginRequiredInterceptor;
        this.adminRequiredInterceptor = adminRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRequiredInterceptor)
                .addPathPatterns("/api/favorites/**", "/api/admin/**");
        registry.addInterceptor(adminRequiredInterceptor)
                .addPathPatterns("/api/admin/**");
    }
}
