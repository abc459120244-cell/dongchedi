package com.dongchedi.car.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.AdminUserView;
import com.dongchedi.car.repository.UserRepository;
import com.dongchedi.car.service.FavoriteService;

/**
 * 后台管理接口，需要 ADMIN 角色（由 AdminRequiredInterceptor 校验）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final FavoriteService favoriteService;

    public AdminController(UserRepository userRepository, FavoriteService favoriteService) {
        this.userRepository = userRepository;
        this.favoriteService = favoriteService;
    }

    /**
     * 用户列表，附带每个用户的收藏数量。
     */
    @GetMapping("/users")
    public List<AdminUserView> users() {
        return userRepository.findAll().stream()
                .map(user -> AdminUserView.from(user, favoriteService.count(user.id())))
                .toList();
    }

    /**
     * 用户总数概览。
     */
    @GetMapping("/summary")
    public java.util.Map<String, Object> summary() {
        return java.util.Map.of("userCount", userRepository.count());
    }
}
