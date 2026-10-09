package com.dongchedi.car.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * 选车优先级：决定各打分维度的权重分配，权重之和为 100。
 */
public enum RecommendationPriority {

    COMPREHENSIVE("综合推荐",
            weights(ScoreDimension.BUDGET, 30,
                    ScoreDimension.SCENE, 25,
                    ScoreDimension.ENERGY, 15,
                    ScoreDimension.REPUTATION, 10,
                    ScoreDimension.POPULARITY, 10,
                    ScoreDimension.SPACE, 10)),
    ECONOMY("经济性优先",
            weights(ScoreDimension.BUDGET, 30,
                    ScoreDimension.ECONOMY, 20,
                    ScoreDimension.SCENE, 15,
                    ScoreDimension.ENERGY, 15,
                    ScoreDimension.REPUTATION, 10,
                    ScoreDimension.POPULARITY, 10)),
    PERFORMANCE("动力性能优先",
            weights(ScoreDimension.BUDGET, 25,
                    ScoreDimension.POWER, 25,
                    ScoreDimension.SCENE, 20,
                    ScoreDimension.ENERGY, 15,
                    ScoreDimension.REPUTATION, 10,
                    ScoreDimension.POPULARITY, 5)),
    SPACE("空间舒适优先",
            weights(ScoreDimension.SPACE, 30,
                    ScoreDimension.BUDGET, 25,
                    ScoreDimension.SCENE, 20,
                    ScoreDimension.ENERGY, 10,
                    ScoreDimension.REPUTATION, 10,
                    ScoreDimension.POPULARITY, 5)),
    REPUTATION("口碑品质优先",
            weights(ScoreDimension.REPUTATION, 30,
                    ScoreDimension.BUDGET, 25,
                    ScoreDimension.SCENE, 20,
                    ScoreDimension.POPULARITY, 15,
                    ScoreDimension.ENERGY, 10));

    private final String label;
    private final Map<ScoreDimension, Double> weights;

    RecommendationPriority(String label, Map<ScoreDimension, Double> weights) {
        this.label = label;
        this.weights = weights;
    }

    public String label() {
        return label;
    }

    public Map<ScoreDimension, Double> weights() {
        return weights;
    }

    public static RecommendationPriority of(String code) {
        if (code == null || code.isBlank()) {
            return COMPREHENSIVE;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (RecommendationPriority priority : values()) {
            if (priority.name().equals(normalized)) {
                return priority;
            }
        }
        return COMPREHENSIVE;
    }

    private static Map<ScoreDimension, Double> weights(Object... pairs) {
        Map<ScoreDimension, Double> map = new EnumMap<>(ScoreDimension.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((ScoreDimension) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        }
        return Collections.unmodifiableMap(map);
    }
}
