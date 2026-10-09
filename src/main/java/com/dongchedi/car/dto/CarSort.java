package com.dongchedi.car.dto;

import java.util.Comparator;
import java.util.Locale;

import com.dongchedi.car.model.Car;

/**
 * 排序方式。
 */
public enum CarSort {

    SALES_DESC("销量优先", Comparator.comparingInt(Car::monthlySales).reversed()
            .thenComparing(Comparator.comparingDouble(Car::userScore).reversed())),
    PRICE_ASC("价格从低到高", Comparator.comparingDouble(Car::price)),
    PRICE_DESC("价格从高到低", Comparator.comparingDouble(Car::price).reversed()),
    SCORE_DESC("口碑评分优先", Comparator.comparingDouble(Car::userScore).reversed()
            .thenComparing(Comparator.comparingInt(Car::monthlySales).reversed())),
    POWER_DESC("动力优先", Comparator.comparingInt(Car::maxPowerKw).reversed()),
    RANGE_DESC("续航优先", Comparator.comparingInt(CarSort::rangeOf).reversed()),
    ID_ASC("默认排序", Comparator.comparingLong(Car::id));

    private final String label;
    private final Comparator<Car> comparator;

    CarSort(String label, Comparator<Car> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public String label() {
        return label;
    }

    public Comparator<Car> comparator() {
        return comparator;
    }

    /**
     * 解析排序参数，无法识别时回退到「销量优先」。
     */
    public static CarSort of(String code) {
        if (code == null || code.isBlank()) {
            return SALES_DESC;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (CarSort sort : values()) {
            if (sort.name().equals(normalized)) {
                return sort;
            }
        }
        return SALES_DESC;
    }

    private static int rangeOf(Car car) {
        return car.electricRangeKm() == null ? 0 : car.electricRangeKm();
    }
}
