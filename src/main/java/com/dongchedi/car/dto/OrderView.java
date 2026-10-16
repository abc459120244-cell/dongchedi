package com.dongchedi.car.dto;

import java.time.LocalDateTime;

import com.dongchedi.car.entity.OrderEntity;
import com.dongchedi.car.model.OrderStatus;
import com.dongchedi.car.model.OrderType;

/**
 * 订单视图（买家端与商家端共用）。
 */
public record OrderView(
        Long id,
        Long carId,
        String carName,
        String type,
        String typeLabel,
        String status,
        String statusLabel,
        String buyerName,
        String contactName,
        String contactPhone,
        String remark,
        String dealerReply,
        Long dealerId,
        String dealerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static OrderView from(OrderEntity order, String dealerName) {
        OrderType type = parseType(order.getType());
        OrderStatus status = parseStatus(order.getStatus());
        return new OrderView(order.getId(), order.getCarId(), order.getCarName(),
                type.name(), type.label(), status.name(), status.label(),
                order.getBuyerName(), order.getContactName(), order.getContactPhone(),
                order.getRemark(), order.getDealerReply(), order.getDealerId(), dealerName,
                order.getCreatedAt(), order.getUpdatedAt());
    }

    private static OrderType parseType(String value) {
        try {
            return OrderType.valueOf(value);
        } catch (Exception ex) {
            return OrderType.INQUIRY;
        }
    }

    private static OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(value);
        } catch (Exception ex) {
            return OrderStatus.PENDING;
        }
    }
}
