package com.dongchedi.car.dto;

/**
 * 商家工作台概览数据。
 */
public record MerchantSummary(
        String dealerName,
        long onSaleCars,
        long offShelfCars,
        long pendingOrders,
        long totalOrders,
        long unreadNotifications,
        int totalMonthlySales) {
}
