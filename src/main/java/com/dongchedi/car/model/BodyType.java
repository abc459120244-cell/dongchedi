package com.dongchedi.car.model;

/**
 * 车身形式。
 */
public enum BodyType {

    SEDAN("轿车"),
    HATCHBACK("两厢车"),
    SUV("SUV"),
    MPV("MPV"),
    COUPE("轿跑车"),
    SPORTS("跑车"),
    PICKUP("皮卡"),
    WAGON("旅行车");

    private final String label;

    BodyType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
