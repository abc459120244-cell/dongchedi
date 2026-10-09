package com.dongchedi.car.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 车型接口集成测试，走完整的 Spring MVC + 服务层链路。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/cars 返回分页结构")
    void shouldReturnPagedCars() throws Exception {
        mockMvc.perform(get("/api/cars").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.total").value(35))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    @DisplayName("GET /api/cars 支持多选能源类型与价格区间")
    void shouldSupportFilterParams() throws Exception {
        mockMvc.perform(get("/api/cars")
                        .param("energyType", "BEV")
                        .param("maxPrice", "20")
                        .param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].name").value("五菱 宏光MINIEV"));
    }

    @Test
    @DisplayName("GET /api/cars/filters 返回筛选项")
    void shouldReturnFilterOptions() throws Exception {
        mockMvc.perform(get("/api/cars/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCars").value(35))
                .andExpect(jsonPath("$.brands.length()").value(23));
    }

    @Test
    @DisplayName("GET /api/cars/{id} 不存在时返回 404")
    void shouldReturn404ForUnknownCar() throws Exception {
        mockMvc.perform(get("/api/cars/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("车型不存在: 9999"));
    }

    @Test
    @DisplayName("GET /api/cars/compare 至少需要两款车")
    void shouldRejectCompareWithOneCar() throws Exception {
        mockMvc.perform(get("/api/cars/compare").param("ids", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("访问不存在的静态资源返回 404 而不是 500")
    void shouldReturn404ForMissingStaticResource() throws Exception {
        mockMvc.perform(get("/not-exists.html"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("请求的资源不存在"));
    }
}
