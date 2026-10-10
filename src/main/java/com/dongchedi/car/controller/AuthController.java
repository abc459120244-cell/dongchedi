package com.dongchedi.car.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.LoginRequest;
import com.dongchedi.car.dto.RegisterRequest;
import com.dongchedi.car.dto.UserView;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 认证接口：注册、登录、退出、查询当前登录用户。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册并自动登录。
     *
     * <pre>POST /api/auth/register  {"username":"tom_2026","password":"tom12345","nickname":"汤姆"}</pre>
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        UserAccount user = authService.register(request, registerRequest);
        return UserView.from(user);
    }

    /**
     * 登录。
     *
     * <pre>POST /api/auth/login  {"username":"demo","password":"demo123"}</pre>
     */
    @PostMapping("/login")
    public UserView login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        UserAccount user = authService.login(request, loginRequest.getUsername(), loginRequest.getPassword());
        return UserView.from(user);
    }

    /**
     * 退出登录（未登录时同样返回成功，保证幂等）。
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        authService.logout(request);
        return Map.of("message", "已退出登录");
    }

    /**
     * 查询当前登录用户，未登录返回 401。
     */
    @GetMapping("/me")
    public UserView me(HttpServletRequest request) {
        return authService.currentUser(request)
                .map(UserView::from)
                .orElseThrow(() -> new BizException(HttpStatus.UNAUTHORIZED, "尚未登录"));
    }
}
