package com.dongchedi.car.dto;

import java.util.List;

import com.dongchedi.car.model.Car;

/**
 * 单条推荐结果。
 */
public record RecommendItem(
        int rank,
        double score,
        Car car,
        List<String> reasons,
        List<ScoreDetail> details) {
}
