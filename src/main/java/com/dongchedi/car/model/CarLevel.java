package com.dongchedi.car.model;

/**
 * 尺寸级别。
 */
public enum CarLevel {

    MINI("微型车"),
    SMALL("小型车"),
    COMPACT("紧凑型车"),
    MID("中型车"),
    LARGE("中大型车"),
    FULL("大型车");

    private final String label;

    CarLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
