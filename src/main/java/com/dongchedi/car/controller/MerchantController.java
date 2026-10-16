package com.dongchedi.car.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.CarFormRequest;
import com.dongchedi.car.dto.MerchantCarView;
import com.dongchedi.car.dto.MerchantSummary;
import com.dongchedi.car.dto.OrderView;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.service.MerchantCarService;
import com.dongchedi.car.service.OrderService;
import com.dongchedi.car.support.SessionKeys;

import jakarta.validation.Valid;

/**
 * 商家端接口：车源增删改查 + 订单处理（需要 MERCHANT / ADMIN 角色）。
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantCarService merchantCarService;
    private final OrderService orderService;

    public MerchantController(MerchantCarService merchantCarService, OrderService orderService) {
        this.merchantCarService = merchantCarService;
        this.orderService = orderService;
    }

    @GetMapping("/summary")
    public MerchantSummary summary(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        return merchantCarService.summary(user);
    }

    @GetMapping("/cars")
    public List<MerchantCarView> cars(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        return merchantCarService.list(user);
    }

    @PostMapping("/cars")
    public MerchantCarView create(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                                  @Valid @RequestBody CarFormRequest form) {
        return merchantCarService.create(user, form);
    }

    @PutMapping("/cars/{id}")
    public MerchantCarView update(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                                  @PathVariable Long id,
                                  @Valid @RequestBody CarFormRequest form) {
        return merchantCarService.update(user, id, form);
    }

    /**
     * 上架 / 下架：{@code POST /api/merchant/cars/12/status?status=OFF_SHELF}
     */
    @PostMapping("/cars/{id}/status")
    public MerchantCarView updateStatus(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                                        @PathVariable Long id,
                                        @RequestParam String status) {
        return merchantCarService.updateStatus(user, id, status);
    }

    @DeleteMapping("/cars/{id}")
    public MerchantSummary delete(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                                  @PathVariable Long id) {
        merchantCarService.delete(user, id);
        return merchantCarService.summary(user);
    }

    /**
     * 收到的订单，可按状态过滤：{@code /api/merchant/orders?status=PENDING}
     */
    @GetMapping("/orders")
    public List<OrderView> orders(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                                  @RequestParam(required = false) String status) {
        return orderService.listForDealer(user, status);
    }

    /**
     * 处理订单（确认 / 拒绝 / 完成）。
     */
    @PostMapping("/orders/{id}/status")
    public OrderView handleOrder(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                                 @PathVariable Long id,
                                 @RequestBody HandleOrderRequest request) {
        return orderService.updateStatus(user, id, request.status(), request.reply());
    }

    /**
     * 处理订单请求体。
     */
    public record HandleOrderRequest(String status, String reply) {
    }
}
