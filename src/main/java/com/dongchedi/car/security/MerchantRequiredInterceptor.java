package com.dongchedi.car.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.model.UserRole;
import com.dongchedi.car.support.SessionKeys;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 商家拦截器：商家端接口需要 MERCHANT 或 ADMIN 角色。
 */
@Component
public class MerchantRequiredInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public MerchantRequiredInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Object attribute = request.getAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE);
        if (attribute instanceof UserAccount user
                && (user.role() == UserRole.MERCHANT || user.isAdmin())) {
            return true;
        }
        ErrorResponses.write(objectMapper, request, response, HttpStatus.FORBIDDEN,
                "该操作需要商家账号（或管理员）");
        return false;
    }
}
