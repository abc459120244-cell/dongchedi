package com.dongchedi.car.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
    private final String uploadDir;

    public WebMvcConfig(LoginRequiredInterceptor loginRequiredInterceptor,
                        AdminRequiredInterceptor adminRequiredInterceptor,
                        MerchantRequiredInterceptor merchantRequiredInterceptor,
                        @Value("${dongchedi.upload.dir:./uploads}") String uploadDir) {
        this.loginRequiredInterceptor = loginRequiredInterceptor;
        this.adminRequiredInterceptor = adminRequiredInterceptor;
        this.merchantRequiredInterceptor = merchantRequiredInterceptor;
        this.uploadDir = uploadDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRequiredInterceptor)
                .addPathPatterns("/api/favorites/**", "/api/admin/**", "/api/merchant/**",
                        "/api/orders/**", "/api/notifications/**", "/api/reviews", "/api/reviews/**");
        registry.addInterceptor(adminRequiredInterceptor)
                .addPathPatterns("/api/admin/**");
        registry.addInterceptor(merchantRequiredInterceptor)
                .addPathPatterns("/api/merchant/**");
    }

    /**
     * 把运行时上传的图片目录映射到 /uploads/**，新上传的图片无需重新打包即可访问。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
