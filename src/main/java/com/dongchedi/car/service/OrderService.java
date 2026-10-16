package com.dongchedi.car.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dongchedi.car.dto.OrderRequest;
import com.dongchedi.car.dto.OrderView;
import com.dongchedi.car.entity.CarEntity;
import com.dongchedi.car.entity.OrderEntity;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.OrderStatus;
import com.dongchedi.car.model.OrderType;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.jpa.CarJpaRepository;
import com.dongchedi.car.repository.jpa.OrderJpaRepository;
import com.dongchedi.car.repository.jpa.UserJpaRepository;

/**
 * 订单服务：买家下单 → 归属到车源商家 → 商家处理 → 双向消息通知。
 */
@Service
public class OrderService {

    private final OrderJpaRepository orderJpa;
    private final CarJpaRepository carJpa;
    private final UserJpaRepository userJpa;
    private final NotificationService notifications;
    private final RealtimeNotifier notifier;

    public OrderService(OrderJpaRepository orderJpa, CarJpaRepository carJpa, UserJpaRepository userJpa,
                        NotificationService notifications, RealtimeNotifier notifier) {
        this.orderJpa = orderJpa;
        this.carJpa = carJpa;
        this.userJpa = userJpa;
        this.notifications = notifications;
        this.notifier = notifier;
    }

    /**
     * 买家提交订单，并立即通知车源所属商家。
     */
    @Transactional
    public OrderView create(UserAccount buyer, OrderRequest request) {
        CarEntity car = carJpa.findById(request.getCarId())
                .filter(CarEntity::isOnSale)
                .orElseThrow(() -> BizException.notFound("车型不存在或已下架：" + request.getCarId()));

        OrderType type = parseType(request.getType());
        OrderEntity order = new OrderEntity();
        order.setCarId(car.getId());
        order.setCarName(car.getName());
        order.setDealerId(car.getDealerId());
        order.setBuyerId(buyer.id());
        order.setBuyerName(buyer.displayName());
        order.setType(type.name());
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setRemark(request.getRemark());
        order.setStatus(OrderStatus.PENDING.name());
        OrderEntity saved = orderJpa.save(order);

        OrderView view = OrderView.from(saved, dealerName(car.getDealerId()));
        String content = String.format("%s 对【%s】提交了%s，联系电话 %s%s",
                buyer.displayName(), car.getName(), type.label(), request.getContactPhone(),
                request.getRemark() == null || request.getRemark().isBlank() ? "" : "，备注：" + request.getRemark());
        for (Long recipient : recipients(car.getDealerId())) {
            notifications.notify(recipient, "ORDER_CREATED", "收到新的" + type.label(), content, saved.getId());
            notifier.push(recipient, "order", view);
        }
        return view;
    }

    /**
     * 买家查看自己的订单。
     */
    public List<OrderView> listForBuyer(Long buyerId) {
        return orderJpa.findByBuyerIdOrderByIdDesc(buyerId).stream()
                .map(order -> OrderView.from(order, dealerName(order.getDealerId())))
                .toList();
    }

    /**
     * 商家查看收到的订单，可按状态过滤。
     */
    public List<OrderView> listForDealer(UserAccount dealer, String status) {
        List<OrderEntity> list = status == null || status.isBlank()
                ? orderJpa.findByDealerIdOrderByIdDesc(dealer.id())
                : orderJpa.findByDealerIdAndStatusOrderByIdDesc(dealer.id(), status.trim().toUpperCase());
        String name = dealer.displayName();
        return list.stream().map(order -> OrderView.from(order, name)).toList();
    }

    /**
     * 商家处理订单（确认 / 拒绝 / 完成），并通知买家。
     */
    @Transactional
    public OrderView updateStatus(UserAccount dealer, Long orderId, String status, String reply) {
        OrderEntity order = orderJpa.findById(orderId)
                .orElseThrow(() -> BizException.notFound("订单不存在：" + orderId));
        if (!dealer.isAdmin() && !dealer.id().equals(order.getDealerId())) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权处理该订单");
        }
        OrderStatus target;
        try {
            target = OrderStatus.valueOf(status == null ? "" : status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw BizException.badRequest("不支持的订单状态：" + status);
        }
        order.setStatus(target.name());
        order.setDealerReply(reply);
        order.setUpdatedAt(LocalDateTime.now());
        OrderEntity saved = orderJpa.save(order);

        OrderView view = OrderView.from(saved, dealer.displayName());
        notifications.notify(order.getBuyerId(), "ORDER_" + target.name(),
                "订单状态更新：" + target.label(),
                String.format("商家【%s】将你对【%s】的%s处理为「%s」%s",
                        dealer.displayName(), order.getCarName(), parseType(order.getType()).label(), target.label(),
                        reply == null || reply.isBlank() ? "" : "，商家回复：" + reply),
                saved.getId());
        notifier.push(order.getBuyerId(), "order", view);
        return view;
    }

    public long pendingCount(Long dealerId) {
        return orderJpa.countByDealerIdAndStatus(dealerId, OrderStatus.PENDING.name());
    }

    /**
     * 平台自营车源（没有归属商家）时，订单通知给管理员。
     */
    private List<Long> recipients(Long dealerId) {
        if (dealerId != null) {
            return List.of(dealerId);
        }
        List<Long> admins = new ArrayList<>();
        userJpa.findAll().stream()
                .filter(user -> "ADMIN".equals(user.getRole()))
                .forEach(user -> admins.add(user.getId()));
        return admins;
    }

    private String dealerName(Long dealerId) {
        if (dealerId == null) {
            return "平台自营";
        }
        return userJpa.findById(dealerId)
                .map(user -> user.getNickname() == null ? user.getUsername() : user.getNickname())
                .orElse("平台自营");
    }

    private OrderType parseType(String value) {
        try {
            return OrderType.valueOf(value == null || value.isBlank() ? "INQUIRY" : value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return OrderType.INQUIRY;
        }
    }
}
