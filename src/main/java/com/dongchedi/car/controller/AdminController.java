package com.dongchedi.car.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.AdminUserView;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.service.AdminService;
import com.dongchedi.car.support.SessionKeys;

/**
 * 后台管理接口，需要 ADMIN 角色（由 AdminRequiredInterceptor 校验）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 用户列表，附带收藏数量与启用状态。
     */
    @GetMapping("/users")
    public List<AdminUserView> users() {
        return adminService.users();
    }

    /**
     * 启用 / 禁用账号：{@code POST /api/admin/users/1/enabled?enabled=false}
     */
    @PostMapping("/users/{id}/enabled")
    public AdminUserView setEnabled(
            @RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount operator,
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return adminService.setEnabled(operator, id, enabled);
    }

    /**
     * 调整角色：{@code POST /api/admin/users/1/role?role=MERCHANT}
     */
    @PostMapping("/users/{id}/role")
    public AdminUserView setRole(
            @RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount operator,
            @PathVariable Long id,
            @RequestParam String role) {
        return adminService.setRole(operator, id, role);
    }

    /**
     * 用户总数概览。
     */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<AdminUserView> users = adminService.users();
        return Map.of(
                "userCount", users.size(),
                "disabledCount", users.stream().filter(user -> !user.enabled()).count(),
                "merchantCount", users.stream().filter(user -> "MERCHANT".equals(user.role())).count(),
                "adminCount", users.stream().filter(user -> "ADMIN".equals(user.role())).count());
    }
}
