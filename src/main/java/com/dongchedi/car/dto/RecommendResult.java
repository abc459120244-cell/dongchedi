package com.dongchedi.car.dto;

import java.util.List;

/**
 * 推荐结果集合。
 */
public record RecommendResult(
        String strategy,
        String priority,
        String summary,
        int candidateCount,
        List<RecommendItem> items) {
}
