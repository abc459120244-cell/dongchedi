package com.dongchedi.car.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.UserRepository;
import com.dongchedi.car.support.SessionKeys;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 登录拦截器：校验 Session 中的登录状态，并把用户对象放入请求属性供控制器使用。
 */
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public LoginRequiredInterceptor(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object attribute = session == null ? null : session.getAttribute(SessionKeys.LOGIN_USER_ID);
        if (attribute instanceof Long userId) {
            Optional<UserAccount> user = userRepository.findById(userId);
            if (user.isPresent()) {
                request.setAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE, user.get());
                return true;
            }
        }

        if (session != null) {
            session.invalidate();
        }
        ErrorResponses.write(objectMapper, request, response, HttpStatus.UNAUTHORIZED, "请先登录后再操作");
        return false;
    }
}
