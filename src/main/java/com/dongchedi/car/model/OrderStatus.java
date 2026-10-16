package com.dongchedi.car.model;

/**
 * 订单状态。
 */
public enum OrderStatus {

    PENDING("待处理"),
    CONFIRMED("已确认"),
    REJECTED("已拒绝"),
    COMPLETED("已完成");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
