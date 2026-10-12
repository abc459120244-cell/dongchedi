package com.dongchedi.car.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.BrandRanking;
import com.dongchedi.car.dto.CarRanking;
import com.dongchedi.car.dto.CountItem;
import com.dongchedi.car.dto.StatisticsOverview;
import com.dongchedi.car.service.StatisticsService;

/**
 * 统计与榜单接口，供首页图表与排行榜使用。
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public StatisticsOverview overview() {
        return statisticsService.overview();
    }

    @GetMapping("/brand-ranking")
    public List<BrandRanking> brandRanking(@RequestParam(required = false, defaultValue = "10") int limit) {
        List<BrandRanking> all = statisticsService.brandRankings();
        return all.size() <= limit ? all : all.subList(0, Math.max(limit, 0));
    }

    /**
     * 车型销量排行榜。
     *
     * <pre>GET /api/statistics/car-ranking?limit=10&amp;scope=NEW_ENERGY</pre>
     */
    @GetMapping("/car-ranking")
    public List<CarRanking> carRanking(@RequestParam(required = false, defaultValue = "10") int limit,
                                       @RequestParam(required = false, defaultValue = "ALL") String scope,
                                       @RequestParam(required = false) String brand,
                                       @RequestParam(required = false) String bodyType) {
        return statisticsService.carRankings(limit, scope, brand, bodyType);
    }

    @GetMapping("/scene-distribution")
    public List<CountItem> sceneDistribution() {
        return statisticsService.sceneDistribution();
    }
}
