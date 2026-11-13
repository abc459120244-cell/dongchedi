package com.dongchedi.car.dto;

/**
 * 车型评价汇总（平均分 + 星级分布）。
 */
public record ReviewSummary(
        double average,
        int total,
        int fiveStar,
        int fourStar,
        int threeStar,
        int twoStar,
        int oneStar) {

    public static ReviewSummary empty() {
        return new ReviewSummary(0, 0, 0, 0, 0, 0, 0);
    }
}
