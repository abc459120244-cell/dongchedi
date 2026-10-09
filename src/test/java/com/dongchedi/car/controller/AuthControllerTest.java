package com.dongchedi.car.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 认证、收藏与角色权限的集成测试（走完整 Spring MVC 链路，包含拦截器）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** 每次使用不同用户名，避免内存仓库在同一个测试上下文中相互影响 */
    private String uniqueUsername() {
        return "user_" + (System.nanoTime() % 1_000_000_000L);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    @DisplayName("注册成功后即处于登录态，/api/auth/me 返回当前用户")
    void shouldRegisterAndKeepSession() throws Exception {
        String username = uniqueUsername();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pass12345\",\"nickname\":\"测试用户\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("测试用户"));
    }

    @Test
    @DisplayName("注册参数不合法时返回 400 与字段错误")
    void shouldRejectInvalidRegister() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.details", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("使用演示账号登录、查询、退出后变回未登录")
    void shouldLoginQueryAndLogout() throws Exception {
        MockHttpSession session = login("demo", "demo123");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.roleLabel").value("普通用户"));

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已退出登录"));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("密码错误返回 401")
    void shouldReturn401ForWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("未登录访问收藏接口返回 401")
    void shouldRejectAnonymousFavoriteAccess() throws Exception {
        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录后再操作"));
    }

    @Test
    @DisplayName("登录后可以读写自己的收藏，且数据与其他用户隔离")
    void shouldManageFavoritesAfterLogin() throws Exception {
        MockHttpSession session = login("demo", "demo123");

        mockMvc.perform(get("/api/favorites/ids").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains(1, 6, 16)));

        mockMvc.perform(post("/api/favorites/35").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true))
                .andExpect(jsonPath("$.total").value(4));

        mockMvc.perform(get("/api/favorites").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].name").value("比亚迪 秦PLUS DM-i"));

        mockMvc.perform(delete("/api/favorites/35").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false))
                .andExpect(jsonPath("$.total").value(3));

        // 新注册用户看不到 demo 的收藏
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + uniqueUsername() + "\",\"password\":\"pass12345\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession newSession = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/favorites/ids").session(newSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("收藏不存在的车型返回 404")
    void shouldRejectUnknownCar() throws Exception {
        MockHttpSession session = login("demo", "demo123");

        mockMvc.perform(post("/api/favorites/9999").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("车型不存在: 9999"));
    }

    @Test
    @DisplayName("普通用户访问后台接口返回 403，管理员可以访问")
    void shouldEnforceAdminRole() throws Exception {
        MockHttpSession userSession = login("demo", "demo123");
        mockMvc.perform(get("/api/admin/users").session(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("该操作需要管理员权限"));

        MockHttpSession adminSession = login("admin", "admin123");
        mockMvc.perform(get("/api/admin/users").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[0].favoriteCount").exists());
    }
}
