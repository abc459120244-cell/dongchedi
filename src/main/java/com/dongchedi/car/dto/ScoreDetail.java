package com.dongchedi.car.dto;

/**
 * 单个维度的得分明细，用于向前端展示推荐的可解释性。
 */
public record ScoreDetail(
        String dimension,
        String label,
        double rawScore,
        double weight,
        double weightedScore) {
}
