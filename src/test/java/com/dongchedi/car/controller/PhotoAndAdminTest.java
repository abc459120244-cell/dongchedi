package com.dongchedi.car.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 车源图片上传与管理端用户管理的集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoAndAdminTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("商家上传车源图片后，买家端详情与图片地址同步更新，删除后回退")
    void shouldUploadAndRemoveCarPhoto() throws Exception {
        MockHttpSession dealer = login("dealer_byd", "dealer123");
        byte[] jpeg = "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "car.jpg", "image/jpeg", jpeg);

        MvcResult uploaded = mockMvc.perform(multipart("/api/merchant/cars/1/photo").file(file).session(dealer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl", startsWith("/uploads/cars/car-1-")))
                .andReturn();
        String photoUrl = json(uploaded).get("photoUrl").asText();

        mockMvc.perform(get("/api/cars/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.car.photoUrl").value(photoUrl));

        mockMvc.perform(get(photoUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        mockMvc.perform(delete("/api/merchant/cars/1/photo").session(dealer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
        mockMvc.perform(get(photoUrl)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("上传非图片文件会被拒绝")
    void shouldRejectNonImageUpload() throws Exception {
        MockHttpSession dealer = login("dealer_byd", "dealer123");
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/merchant/cars/1/photo").file(file).session(dealer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只支持 JPG / PNG / WebP 格式的图片"));
    }

    @Test
    @DisplayName("商家编辑车源时不会清空表单未包含的油耗 / 续航 / 场景")
    void shouldPreserveFieldsMissingFromMerchantForm() throws Exception {
        MockHttpSession dealer = login("dealer_byd", "dealer123");
        String body = """
                {"name":"比亚迪 秦PLUS DM-i","brand":"比亚迪","level":"COMPACT","bodyType":"SEDAN",
                 "energyType":"PHEV","price":9.98,"seats":5,"maxPowerKw":81,"zeroToHundredSeconds":7.9,
                 "monthlySales":32500,"userScore":4.6,"tags":["超低油耗"],"scenes":[],
                 "highlight":"10 万级插混家轿销量冠军，亏电油耗仅 3.8L","status":"ON_SALE"}
                """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/merchant/cars/1").session(dealer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cars/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuelConsumption").value(3.8))
                .andExpect(jsonPath("$.electricRangeKm").value(120))
                .andExpect(jsonPath("$.scenes[0]").value("FAMILY"));
    }

    @Test
    @DisplayName("管理员可以调整角色、禁用/启用账号；禁用后登录与会话立即失效")
    void shouldManageUsers() throws Exception {
        MockHttpSession admin = login("admin", "admin123");
        String username = "up_" + (System.nanoTime() % 1_000_000_000L);

        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pass12345\",\"nickname\":\"测试账号\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long userId = json(registered).get("id").asLong();
        MockHttpSession userSession = (MockHttpSession) registered.getRequest().getSession(false);

        // 普通用户不能访问商家端
        mockMvc.perform(get("/api/merchant/summary").session(userSession))
                .andExpect(status().isForbidden());

        // 管理员把角色调成商家
        mockMvc.perform(post("/api/admin/users/" + userId + "/role").session(admin).param("role", "MERCHANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MERCHANT"))
                .andExpect(jsonPath("$.roleLabel").value("商家"));
        mockMvc.perform(get("/api/merchant/summary").session(userSession))
                .andExpect(status().isOk());

        // 禁用后：已有会话立即失效，重新登录被拒绝
        mockMvc.perform(post("/api/admin/users/" + userId + "/enabled").session(admin).param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        mockMvc.perform(get("/api/merchant/summary").session(userSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pass12345\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("账号已被禁用，请联系管理员"));

        // 重新启用后恢复正常
        mockMvc.perform(post("/api/admin/users/" + userId + "/enabled").session(admin).param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pass12345\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("管理员不能禁用或修改自己的角色")
    void shouldProtectAdminSelf() throws Exception {
        MockHttpSession admin = login("admin", "admin123");
        // 从用户列表里找到 admin 的 id（避免依赖固定 id）
        long adminId = 0;
        MvcResult users = mockMvc.perform(get("/api/admin/users").session(admin))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode user : json(users)) {
            if ("admin".equals(user.get("username").asText())) {
                adminId = user.get("id").asLong();
                break;
            }
        }

        mockMvc.perform(post("/api/admin/users/" + adminId + "/enabled").session(admin).param("enabled", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能修改自己的账号状态或角色"));
    }
}
