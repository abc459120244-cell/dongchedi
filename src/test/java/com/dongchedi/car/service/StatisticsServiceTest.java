package com.dongchedi.car.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.dongchedi.car.dto.BrandRanking;
import com.dongchedi.car.dto.CountItem;
import com.dongchedi.car.dto.StatisticsOverview;
import com.dongchedi.car.repository.CarRepository;
import com.dongchedi.car.repository.InMemoryCarRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 统计服务单元测试。
 */
class StatisticsServiceTest {

    private StatisticsService service;

    @BeforeEach
    void setUp() throws Exception {
        CarRepository repository = new InMemoryCarRepository(new DefaultResourceLoader(), new ObjectMapper(),
                "classpath:data/cars.json");
        service = new StatisticsService(repository);
    }

    @Test
    @DisplayName("概览统计的车型总数与品牌数正确")
    void shouldSummarizeOverview() {
        StatisticsOverview overview = service.overview();

        assertThat(overview.totalCars()).isEqualTo(63);
        assertThat(overview.totalBrands()).isEqualTo(33);
        assertThat(overview.performanceCarCount()).isGreaterThanOrEqualTo(15);
        assertThat(overview.minPrice()).isLessThan(overview.averagePrice());
        assertThat(overview.averagePrice()).isLessThan(overview.maxPrice());
        assertThat(overview.newEnergyRatio()).isBetween(0.0, 1.0);
        assertThat(overview.energyDistribution()).isNotEmpty();
        assertThat(overview.priceDistribution()).hasSize(6);
    }

    @Test
    @DisplayName("各维度分布数量之和等于车型总数（多值维度除外）")
    void shouldMatchDistributionTotals() {
        StatisticsOverview overview = service.overview();

        assertThat(overview.energyDistribution().stream().mapToLong(CountItem::count).sum())
                .isEqualTo(overview.totalCars());
        assertThat(overview.bodyTypeDistribution().stream().mapToLong(CountItem::count).sum())
                .isEqualTo(overview.totalCars());
        assertThat(overview.priceDistribution().stream().mapToLong(CountItem::count).sum())
                .isEqualTo(overview.totalCars());
    }

    @Test
    @DisplayName("品牌销量榜按销量降序且排名连续")
    void shouldRankBrandsBySales() {
        List<BrandRanking> rankings = service.brandRankings();

        assertThat(rankings).hasSize(33);
        assertThat(rankings).isSortedAccordingTo(
                (a, b) -> Long.compare(b.totalMonthlySales(), a.totalMonthlySales()));
        assertThat(rankings.get(0).rank()).isEqualTo(1);
        assertThat(rankings.get(0).brand()).isEqualTo("比亚迪");
        assertThat(rankings).allSatisfy(item -> assertThat(item.bestSeller()).isNotBlank());
    }

    @Test
    @DisplayName("场景分布包含全部用车场景且数量为正")
    void shouldComputeSceneDistribution() {
        List<CountItem> scenes = service.sceneDistribution();

        assertThat(scenes).hasSize(9);
        assertThat(scenes).allSatisfy(item -> assertThat(item.count()).isPositive());
    }
}
