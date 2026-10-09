package com.dongchedi.car.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * 推荐接口集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecommendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/recommend 按预算与场景返回推荐列表")
    void shouldRecommendByBudgetAndScene() throws Exception {
        String body = """
                {
                  "minPrice": 12,
                  "maxPrice": 20,
                  "scenes": ["FAMILY", "COMMUTE"],
                  "energyPreference": "ANY",
                  "minSeats": 5,
                  "priority": "COMPREHENSIVE",
                  "limit": 5
                }
                """;

        mockMvc.perform(post("/api/recommend").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("综合推荐"))
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.items[0].rank").value(1))
                .andExpect(jsonPath("$.items[0].reasons", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    @DisplayName("POST /api/recommend 参数校验失败时返回 400 与字段错误")
    void shouldRejectInvalidRequest() throws Exception {
        String body = """
                {"minPrice": 10, "maxPrice": 20, "minSeats": 20, "limit": 99}
                """;

        mockMvc.perform(post("/api/recommend").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.details", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("GET /api/recommend 快速推荐可用")
    void shouldSupportQuickRecommend() throws Exception {
        mockMvc.perform(get("/api/recommend")
                        .param("minPrice", "10")
                        .param("maxPrice", "15")
                        .param("scenes", "COMMUTE")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    @Test
    @DisplayName("GET /api/recommend/options 返回场景与优先级选项")
    void shouldReturnOptions() throws Exception {
        mockMvc.perform(get("/api/recommend/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenes", hasSize(9)))
                .andExpect(jsonPath("$.priorities", hasSize(5)))
                .andExpect(jsonPath("$.energyPreferences", hasSize(3)));
    }
}
