package com.dongchedi.car.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.dongchedi.car.security.AdminRequiredInterceptor;
import com.dongchedi.car.security.LoginRequiredInterceptor;
import com.dongchedi.car.security.MerchantRequiredInterceptor;

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
    private final MerchantRequiredInterceptor merchantRequiredInterceptor;

    public WebMvcConfig(LoginRequiredInterceptor loginRequiredInterceptor,
                        AdminRequiredInterceptor adminRequiredInterceptor,
                        MerchantRequiredInterceptor merchantRequiredInterceptor) {
        this.loginRequiredInterceptor = loginRequiredInterceptor;
        this.adminRequiredInterceptor = adminRequiredInterceptor;
        this.merchantRequiredInterceptor = merchantRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRequiredInterceptor)
                .addPathPatterns("/api/favorites/**", "/api/admin/**", "/api/merchant/**",
                        "/api/orders/**", "/api/notifications/**");
        registry.addInterceptor(adminRequiredInterceptor)
                .addPathPatterns("/api/admin/**");
        registry.addInterceptor(merchantRequiredInterceptor)
                .addPathPatterns("/api/merchant/**");
    }
}
