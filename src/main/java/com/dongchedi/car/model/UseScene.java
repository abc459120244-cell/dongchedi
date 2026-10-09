package com.dongchedi.car.model;

/**
 * 用车场景，推荐算法依据该标签匹配用户需求。
 */
public enum UseScene {

    FAMILY("家用代步"),
    COMMUTE("城市通勤"),
    FIRST_CAR("新手首购"),
    FAMILY_TRIP("家庭出游"),
    LONG_TRIP("长途自驾"),
    BUSINESS("商务接待"),
    PERFORMANCE("性能驾控"),
    OUTDOOR("越野户外"),
    RIDE_HAILING("网约出行");

    private final String label;

    UseScene(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
