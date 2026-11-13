package com.dongchedi.car.dto;

import java.util.List;

import com.dongchedi.car.model.Car;

/**
 * 车型详情聚合：车型参数 + 商家信息 + 评价汇总与列表。
 */
public record CarDetailView(
        Car car,
        Long dealerId,
        String dealerName,
        ReviewSummary reviewSummary,
        List<ReviewView> reviews) {
}
