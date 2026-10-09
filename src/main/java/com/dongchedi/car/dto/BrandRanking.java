package com.dongchedi.car.dto;

/**
 * 品牌销量排行榜条目。
 */
public record BrandRanking(
        int rank,
        String brand,
        int carCount,
        long totalMonthlySales,
        double averageScore,
        double averagePrice,
        String bestSeller) {
}
