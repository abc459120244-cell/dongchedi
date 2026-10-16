package com.dongchedi.car.dto;

import java.util.List;

import com.dongchedi.car.entity.CarEntity;

/**
 * 商家端车源视图（含上架状态与商家归属）。
 */
public record MerchantCarView(
        Long id,
        String name,
        String brand,
        String level,
        String bodyType,
        String bodyTypeLabel,
        String energyTypeLabel,
        double price,
        int seats,
        int maxPowerKw,
        double zeroToHundredSeconds,
        String accelerationLabel,
        int monthlySales,
        double userScore,
        String highlight,
        List<String> tags,
        String status,
        String statusLabel,
        Long dealerId) {

    public static MerchantCarView from(CarEntity entity) {
        var car = entity.toModel();
        return new MerchantCarView(entity.getId(), entity.getName(), entity.getBrand(),
                entity.getLevel(), entity.getBodyType(), car.getBodyTypeLabel(), car.getEnergyTypeLabel(),
                entity.getPrice(), entity.getSeats(), entity.getMaxPowerKw(),
                entity.getZeroToHundredSeconds(), car.getAccelerationLabel(),
                entity.getMonthlySales(), entity.getUserScore(), entity.getHighlight(),
                car.tags(), entity.getStatus(), statusLabel(entity.getStatus()), entity.getDealerId());
    }

    public static String statusLabel(String status) {
        return CarEntity.STATUS_ON_SALE.equals(status) ? "在售" : "已下架";
    }
}
