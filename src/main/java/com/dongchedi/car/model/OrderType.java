package com.dongchedi.car.model;

/**
 * 订单类型：买家在车型页发起的动作。
 */
public enum OrderType {

    TEST_DRIVE("预约试驾"),
    INQUIRY("询价咨询"),
    DEPOSIT("意向定金");

    private final String label;

    OrderType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
