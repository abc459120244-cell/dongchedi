package com.dongchedi.car.model;

/**
 * 推荐算法的打分维度。
 */
public enum ScoreDimension {

    BUDGET("预算匹配"),
    SCENE("场景匹配"),
    ENERGY("能源偏好"),
    REPUTATION("用户口碑"),
    POPULARITY("市场热度"),
    SPACE("空间表现"),
    ECONOMY("能耗经济"),
    POWER("动力性能");

    private final String label;

    ScoreDimension(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
