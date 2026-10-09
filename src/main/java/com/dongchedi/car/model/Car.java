package com.dongchedi.car.model;

import java.util.List;

/**
 * 车型（车辆配置）信息。
 *
 * <p>使用 record 表达不可变数据，字段与 {@code src/main/resources/data/cars.json} 中的属性一一对应。
 */
public record Car(
        Long id,
        String name,
        String brand,
        CarLevel level,
        BodyType bodyType,
        EnergyType energyType,
        double price,
        int seats,
        int lengthMm,
        int widthMm,
        int heightMm,
        int wheelbaseMm,
        int maxPowerKw,
        Double fuelConsumption,
        Integer electricRangeKm,
        int monthlySales,
        double userScore,
        List<UseScene> scenes,
        List<String> tags,
        String highlight) {

    public String getLevelLabel() {
        return level == null ? null : level.label();
    }

    public String getBodyTypeLabel() {
        return bodyType == null ? null : bodyType.label();
    }

    public String getEnergyTypeLabel() {
        return energyType == null ? null : energyType.label();
    }

    /**
     * 能源类型是否可以上绿牌，前端用于展示新能源标签。
     */
    public boolean isNewEnergy() {
        return energyType != null && energyType.isElectric();
    }

    public String getPriceLabel() {
        return String.format("%.2f 万", price);
    }

    public List<String> getSceneLabels() {
        return scenes == null ? List.of() : scenes.stream().map(UseScene::label).toList();
    }

    /**
     * 能耗描述：纯电车展示续航，燃油/混动车展示百公里油耗。
     */
    public String getEnergyDescription() {
        if (electricRangeKm != null && fuelConsumption != null) {
            return String.format("纯电 %dkm / 亏电 %.1fL", electricRangeKm, fuelConsumption);
        }
        if (electricRangeKm != null) {
            return String.format("续航 %dkm", electricRangeKm);
        }
        if (fuelConsumption != null) {
            return String.format("%.1fL/100km", fuelConsumption);
        }
        return "暂无数据";
    }
}
