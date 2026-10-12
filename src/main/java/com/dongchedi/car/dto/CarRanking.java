package com.dongchedi.car.dto;

/**
 * 车型销量排行榜条目。
 */
public record CarRanking(
        int rank,
        Long carId,
        String name,
        String brand,
        String bodyType,
        String bodyTypeLabel,
        String energyTypeLabel,
        double price,
        int monthlySales,
        double userScore,
        /** 该车型销量在统计范围内的占比（0~1） */
        double salesShare) {
}
