package com.dongchedi.car.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.support.SessionKeys;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 管理员拦截器：在 {@link LoginRequiredInterceptor} 之后执行，校验角色。
 */
@Component
public class AdminRequiredInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AdminRequiredInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Object attribute = request.getAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE);
        if (attribute instanceof UserAccount user && user.isAdmin()) {
            return true;
        }
        ErrorResponses.write(objectMapper, request, response, HttpStatus.FORBIDDEN, "该操作需要管理员权限");
        return false;
    }
}
