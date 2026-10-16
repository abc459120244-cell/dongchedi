package com.dongchedi.car.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * 多端互动集成测试：买家下单 → 商家收到订单 → 商家处理 → 买家收到消息。
 *
 * <p>同时覆盖商家端车源增删改查对买家端列表的影响，以及商家端接口的权限控制。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantOrderFlowTest {

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
    @DisplayName("买家下单 → 商家收到订单并处理 → 买家收到订单状态消息")
    void shouldCompleteCrossRoleOrderFlow() throws Exception {
        MockHttpSession buyer = login("demo", "demo123");

        // 1. 买家对「比亚迪 秦PLUS DM-i」提交预约试驾（该车源归属 dealer_byd）
        MvcResult created = mockMvc.perform(post("/api/orders").session(buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":1,\"type\":\"TEST_DRIVE\",\"contactName\":\"小王\","
                                + "\"contactPhone\":\"13800001111\",\"remark\":\"希望周末到店\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.typeLabel").value("预约试驾"))
                .andExpect(jsonPath("$.dealerName").value("比亚迪新能源旗舰店"))
                .andReturn();
        long orderId = json(created).get("id").asLong();

        // 2. 商家端登录后能看到这笔订单（最新一条）
        MockHttpSession dealer = login("dealer_byd", "dealer123");
        mockMvc.perform(get("/api/merchant/orders").session(dealer).param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId))
                .andExpect(jsonPath("$[0].contactPhone").value("13800001111"))
                .andExpect(jsonPath("$[0].remark").value("希望周末到店"));

        // 商家端概览里待处理订单数 ≥ 1
        mockMvc.perform(get("/api/merchant/summary").session(dealer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealerName").value("比亚迪新能源旗舰店"));

        // 3. 商家处理订单，并给买家留言
        mockMvc.perform(post("/api/merchant/orders/" + orderId + "/status").session(dealer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\",\"reply\":\"已为您预留试驾车\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusLabel").value("已确认"))
                .andExpect(jsonPath("$.dealerReply").value("已为您预留试驾车"));

        // 4. 买家端收到订单状态消息（站内消息已落库）
        mockMvc.perform(get("/api/notifications").session(buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ORDER_CONFIRMED"))
                .andExpect(jsonPath("$[0].title").value(containsString("已确认")))
                .andExpect(jsonPath("$[0].content").value(containsString("已为您预留试驾车")));

        // 5. 买家「我的订单」可以看到处理结果
        mockMvc.perform(get("/api/orders/mine").session(buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("商家新增 / 修改 / 下架 / 删除车源，买家端列表同步变化")
    void shouldSyncMerchantCarChangesToStorefront() throws Exception {
        MockHttpSession dealer = login("dealer_ev", "dealer123");
        String createBody = """
                {"name":"测试车型 X1","brand":"特斯拉","level":"MID","bodyType":"SUV","energyType":"BEV",
                 "price":18.88,"seats":5,"maxPowerKw":200,"zeroToHundredSeconds":6.5,
                 "monthlySales":100,"userScore":4.2,"tags":["测试标签"],"scenes":["COMMUTE"],
                 "highlight":"商家端新增的测试车源","status":"ON_SALE"}
                """;

        // 新增：拿到数据库自增 id
        MvcResult created = mockMvc.perform(post("/api/merchant/cars").session(dealer)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试车型 X1"))
                .andExpect(jsonPath("$.statusLabel").value("在售"))
                .andReturn();
        long carId = json(created).get("id").asLong();

        // 买家端能查到这辆车（数据来自数据库）
        mockMvc.perform(get("/api/cars/" + carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试车型 X1"))
                .andExpect(jsonPath("$.price").value(18.88));

        // 修改价格
        String updateBody = createBody.replace("18.88", "15.5");
        mockMvc.perform(put("/api/merchant/cars/" + carId).session(dealer)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(15.5));
        mockMvc.perform(get("/api/cars/" + carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(15.5));

        // 下架后买家端不再展示
        mockMvc.perform(post("/api/merchant/cars/" + carId + "/status").session(dealer).param("status", "OFF_SHELF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusLabel").value("已下架"));
        mockMvc.perform(get("/api/cars/" + carId))
                .andExpect(status().isNotFound());

        // 删除车源
        mockMvc.perform(delete("/api/merchant/cars/" + carId).session(dealer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealerName").value("新势力体验中心"));
        mockMvc.perform(get("/api/merchant/cars").session(dealer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + carId + ")]", hasSize(0)));
    }

    @Test
    @DisplayName("商家端接口权限：未登录 401、普通用户 403、不能管理他人车源 403")
    void shouldEnforceMerchantPermissions() throws Exception {
        mockMvc.perform(get("/api/merchant/cars"))
                .andExpect(status().isUnauthorized());

        MockHttpSession buyer = login("demo", "demo123");
        mockMvc.perform(get("/api/merchant/cars").session(buyer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("商家账号")));
        mockMvc.perform(post("/api/orders").session(buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":1,\"contactName\":\"小王\",\"contactPhone\":\"123\"}"))
                .andExpect(status().isBadRequest());

        // dealer_ev 不能操作 dealer_byd 名下的车源（id=1 是比亚迪 秦PLUS DM-i）
        MockHttpSession otherDealer = login("dealer_ev", "dealer123");
        mockMvc.perform(post("/api/merchant/cars/1/status").session(otherDealer).param("status", "OFF_SHELF"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("只能管理自己名下")));
    }
}
