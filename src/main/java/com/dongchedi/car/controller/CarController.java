package com.dongchedi.car.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.CarQuery;
import com.dongchedi.car.dto.FilterOptions;
import com.dongchedi.car.dto.PageResult;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.service.CarQueryService;

/**
 * 车型查询接口。
 */
@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarQueryService carQueryService;

    public CarController(CarQueryService carQueryService) {
        this.carQueryService = carQueryService;
    }

    /**
     * 多条件筛选 + 排序 + 分页。
     *
     * <p>示例：{@code /api/cars?energyType=BEV&minPrice=15&maxPrice=30&sort=PRICE_ASC&page=1&size=12}
     */
    @GetMapping
    public PageResult<Car> list(CarQuery query) {
        return carQueryService.search(query);
    }

    @GetMapping("/filters")
    public FilterOptions filters() {
        return carQueryService.filterOptions();
    }

    @GetMapping("/{id}")
    public Car detail(@PathVariable Long id) {
        return carQueryService.getById(id);
    }

    /**
     * 车型对比，示例：{@code /api/cars/compare?ids=1,3,9}
     */
    @GetMapping("/compare")
    public List<Car> compare(@RequestParam List<String> ids) {
        List<Long> parsed = new ArrayList<>();
        for (String raw : ids) {
            for (String item : raw.split(",")) {
                if (!item.isBlank()) {
                    parsed.add(Long.parseLong(item.trim()));
                }
            }
        }
        return carQueryService.compare(parsed);
    }
}
