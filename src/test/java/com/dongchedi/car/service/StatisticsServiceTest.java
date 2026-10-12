package com.dongchedi.car.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.dongchedi.car.dto.BrandRanking;
import com.dongchedi.car.dto.CarRanking;
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

    @Test
    @DisplayName("车型销量榜按销量降序、排名连续且占比介于 0~1")
    void shouldRankCarsBySales() {
        List<CarRanking> rankings = service.carRankings(10, "ALL", null, null);

        assertThat(rankings).hasSize(10);
        assertThat(rankings).extracting(CarRanking::rank).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(rankings).isSortedAccordingTo(
                (a, b) -> Integer.compare(b.monthlySales(), a.monthlySales()));
        assertThat(rankings.get(0).name()).isEqualTo("比亚迪 秦PLUS DM-i");
        assertThat(rankings.get(0).salesShare()).isBetween(0.0, 1.0);
        assertThat(rankings).allSatisfy(item -> {
            assertThat(item.monthlySales()).isPositive();
            assertThat(item.price()).isPositive();
            assertThat(item.bodyType()).isNotBlank();
        });
    }

    @Test
    @DisplayName("车型销量榜支持新能源 / 燃油 / 性能车范围与品牌、车身形式过滤")
    void shouldFilterCarRanking() {
        List<CarRanking> newEnergy = service.carRankings(50, "NEW_ENERGY", null, null);
        assertThat(newEnergy).isNotEmpty();
        assertThat(newEnergy).allSatisfy(item -> assertThat(item.energyTypeLabel())
                .isIn("纯电动", "插电式混合动力", "增程式电动"));

        List<CarRanking> fuel = service.carRankings(50, "TRADITIONAL", null, null);
        assertThat(fuel).isNotEmpty();
        assertThat(fuel).allSatisfy(item -> assertThat(item.energyTypeLabel())
                .isIn("汽油", "柴油", "油电混合"));

        List<CarRanking> performance = service.carRankings(50, "PERFORMANCE", null, null);
        assertThat(performance).isNotEmpty();
        assertThat(performance).hasSizeLessThan(63);

        List<CarRanking> suv = service.carRankings(50, "ALL", null, "SUV");
        assertThat(suv).isNotEmpty();
        assertThat(suv).allSatisfy(item -> assertThat(item.bodyType()).isEqualTo("SUV"));

        List<CarRanking> byd = service.carRankings(50, "ALL", "比亚迪", null);
        assertThat(byd).isNotEmpty();
        assertThat(byd).allSatisfy(item -> assertThat(item.brand()).isEqualTo("比亚迪"));
    }

    @Test
    @DisplayName("车型销量榜的 limit 生效，并且最大返回量受上限约束（50）")
    void shouldRespectCarRankingLimit() {
        assertThat(service.carRankings(3, "ALL", null, null)).hasSize(3);
        assertThat(service.carRankings(999, "ALL", null, null)).hasSize(50);
    }
}
