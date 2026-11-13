package com.dongchedi.car.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 评价系统、车型详情聚合、操作审计日志与订单搜索的集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewAndAuditTest {

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
    @DisplayName("评价：新增 → 详情聚合 → 修改（同一用户只保留一条）→ 删除")
    void shouldManageReviewsAndExposeThemInDetail() throws Exception {
        MockHttpSession buyer = login("demo", "demo123");

        MvcResult created = mockMvc.perform(post("/api/reviews").session(buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":1,\"rating\":5,\"content\":\"空间大、油耗低，家用很合适\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.mine").value(true))
                .andReturn();
        long reviewId = json(created).get("id").asLong();

        mockMvc.perform(get("/api/cars/1/detail").session(buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.car.name").value("比亚迪 秦PLUS DM-i"))
                .andExpect(jsonPath("$.dealerName").value("比亚迪新能源旗舰店"))
                .andExpect(jsonPath("$.reviewSummary.total").value(1))
                .andExpect(jsonPath("$.reviewSummary.average").value(5.0))
                .andExpect(jsonPath("$.reviews[0].content").value("空间大、油耗低，家用很合适"));

        // 再次提交视为修改，不会产生第二条
        mockMvc.perform(post("/api/reviews").session(buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":1,\"rating\":3,\"content\":\"使用一年后更新：整体满意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.rating").value(3));

        mockMvc.perform(get("/api/cars/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewSummary.total").value(1))
                .andExpect(jsonPath("$.reviewSummary.average").value(3.0))
                .andExpect(jsonPath("$.reviews[0].mine").value(false));

        mockMvc.perform(delete("/api/reviews/" + reviewId).session(buyer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/cars/1/detail"))
                .andExpect(jsonPath("$.reviewSummary.total").value(0));
    }

    @Test
    @DisplayName("操作审计：商家新增 / 删除车源会在数据库留下日志")
    void shouldRecordMerchantOperations() throws Exception {
        MockHttpSession dealer = login("dealer_ev", "dealer123");
        String body = """
                {"name":"审计测试车","brand":"特斯拉","level":"MID","bodyType":"SUV","energyType":"BEV",
                 "price":20.0,"seats":5,"maxPowerKw":200,"zeroToHundredSeconds":6.0,
                 "monthlySales":10,"userScore":4.0,"tags":[],"scenes":[],"highlight":"审计测试","status":"ON_SALE"}
                """;
        MvcResult created = mockMvc.perform(post("/api/merchant/cars").session(dealer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        long carId = json(created).get("id").asLong();

        mockMvc.perform(get("/api/merchant/logs").session(dealer).param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("新增车源"))
                .andExpect(jsonPath("$[0].targetId").value(carId))
                .andExpect(jsonPath("$[0].detail").value(containsString("审计测试车")));

        mockMvc.perform(delete("/api/merchant/cars/" + carId).session(dealer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/merchant/logs").session(dealer).param("limit", "10"))
                .andExpect(jsonPath("$[0].action").value("删除车源"));
    }

    @Test
    @DisplayName("商家订单支持按车型 / 买家 / 手机号搜索")
    void shouldSearchMerchantOrders() throws Exception {
        MockHttpSession buyer = login("demo", "demo123");
        mockMvc.perform(post("/api/orders").session(buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":1,\"type\":\"INQUIRY\",\"contactName\":\"李四\","
                                + "\"contactPhone\":\"13700003333\",\"remark\":\"想了解落地价\"}"))
                .andExpect(status().isOk());

        MockHttpSession dealer = login("dealer_byd", "dealer123");
        mockMvc.perform(get("/api/merchant/orders").session(dealer).param("keyword", "李四"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contactName").value("李四"));
        mockMvc.perform(get("/api/merchant/orders").session(dealer).param("keyword", "13700003333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/merchant/orders").session(dealer).param("keyword", "不存在的关键字"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
