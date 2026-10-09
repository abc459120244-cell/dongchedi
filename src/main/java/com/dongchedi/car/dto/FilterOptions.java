package com.dongchedi.car.dto;

import java.util.List;

/**
 * 前端筛选面板所需的全部可选项。
 */
public record FilterOptions(
        List<Option> brands,
        List<Option> levels,
        List<Option> bodyTypes,
        List<Option> energyTypes,
        List<Option> scenes,
        List<Option> sorts,
        List<Integer> seatOptions,
        double minPrice,
        double maxPrice,
        int totalCars) {
}
