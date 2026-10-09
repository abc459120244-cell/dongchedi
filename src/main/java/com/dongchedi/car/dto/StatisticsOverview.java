package com.dongchedi.car.dto;

import java.util.List;

/**
 * 市场概览统计。
 */
public record StatisticsOverview(
        int totalCars,
        int totalBrands,
        double averagePrice,
        double minPrice,
        double maxPrice,
        double averageScore,
        long totalMonthlySales,
        double newEnergyRatio,
        List<CountItem> energyDistribution,
        List<CountItem> bodyTypeDistribution,
        List<CountItem> levelDistribution,
        List<CountItem> priceDistribution) {
}
