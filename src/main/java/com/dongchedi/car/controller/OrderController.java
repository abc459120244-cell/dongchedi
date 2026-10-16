package com.dongchedi.car.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.OrderRequest;
import com.dongchedi.car.dto.OrderView;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.service.OrderService;
import com.dongchedi.car.support.SessionKeys;

import jakarta.validation.Valid;

/**
 * 买家端订单接口（需要登录）。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 提交预约试驾 / 询价 / 意向定金。
     */
    @PostMapping
    public OrderView create(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                            @Valid @RequestBody OrderRequest request) {
        return orderService.create(user, request);
    }

    /**
     * 我的订单。
     */
    @GetMapping("/mine")
    public List<OrderView> mine(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        return orderService.listForBuyer(user.id());
    }
}
