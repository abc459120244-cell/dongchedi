package com.dongchedi.car.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.dongchedi.car.dto.CarQuery;
import com.dongchedi.car.dto.FilterOptions;
import com.dongchedi.car.dto.Option;
import com.dongchedi.car.dto.PageResult;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.repository.CarRepository;
import com.dongchedi.car.repository.InMemoryCarRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 筛选服务单元测试，直接使用真实的种子数据集。
 */
class CarQueryServiceTest {

    private CarQueryService service;

    @BeforeEach
    void setUp() throws Exception {
        CarRepository repository = new InMemoryCarRepository(new DefaultResourceLoader(), new ObjectMapper(),
                "classpath:data/cars.json");
        service = new CarQueryService(repository);
    }

    @Test
    @DisplayName("默认查询返回按销量倒序排列的第一页数据")
    void shouldReturnFirstPageSortedBySales() {
        PageResult<Car> result = service.search(new CarQuery());

        assertThat(result.total()).isEqualTo(63);
        assertThat(result.items()).hasSize(12);
        assertThat(result.totalPages()).isEqualTo(6);
        assertThat(result.items()).isSortedAccordingTo((a, b) -> Integer.compare(b.monthlySales(), a.monthlySales()));
    }

    @Test
    @DisplayName("按价格区间筛选时结果都落在区间内")
    void shouldFilterByPriceRange() {
        CarQuery query = new CarQuery();
        query.setMinPrice(15.0);
        query.setMaxPrice(25.0);

        PageResult<Car> result = service.search(query);

        assertThat(result.total()).isPositive();
        assertThat(result.items()).allSatisfy(car -> assertThat(car.price()).isBetween(15.0, 25.0));
    }

    @Test
    @DisplayName("能源类型可以多选，同一维度内取并集")
    void shouldFilterByMultipleEnergyTypes() {
        CarQuery query = new CarQuery();
        query.setEnergyType(List.of("BEV", "EREV"));

        PageResult<Car> result = service.search(query);

        assertThat(result.total()).isPositive();
        assertThat(result.items()).allSatisfy(car -> assertThat(car.energyType().name()).isIn("BEV", "EREV"));
    }

    @Test
    @DisplayName("关键字可以命中品牌与车型名")
    void shouldSearchByKeyword() {
        CarQuery query = new CarQuery();
        query.setKeyword("比亚迪");

        PageResult<Car> result = service.search(query);

        assertThat(result.total()).isEqualTo(5);
        assertThat(result.items()).allSatisfy(car -> assertThat(car.brand()).isEqualTo("比亚迪"));
    }

    @Test
    @DisplayName("价格升序排序生效")
    void shouldSortByPriceAscending() {
        CarQuery query = new CarQuery();
        query.setSort("PRICE_ASC");
        query.setSize(5);

        PageResult<Car> result = service.search(query);

        assertThat(result.items()).isSortedAccordingTo((a, b) -> Double.compare(a.price(), b.price()));
        assertThat(result.items().get(0).name()).isEqualTo("五菱 宏光MINIEV");
    }

    @Test
    @DisplayName("多维度组合筛选：新能源 + 6 座以上 + 45 万以内")
    void shouldCombineMultipleDimensions() {
        CarQuery query = new CarQuery();
        query.setEnergyType(List.of("PHEV", "EREV", "BEV"));
        query.setMinSeats(6);
        query.setMaxPrice(45.0);

        PageResult<Car> result = service.search(query);

        assertThat(result.items()).extracting(Car::name)
                .containsExactlyInAnyOrder("理想 L9", "问界 M7", "零跑 C16", "腾势 D9");
        assertThat(result.items()).allSatisfy(car -> {
            assertThat(car.seats()).isGreaterThanOrEqualTo(6);
            assertThat(car.price()).isLessThanOrEqualTo(45.0);
            assertThat(car.isNewEnergy()).isTrue();
        });
    }

    @Test
    @DisplayName("无法识别的枚举值被忽略而不是抛异常")
    void shouldIgnoreUnknownEnumValue() {
        CarQuery query = new CarQuery();
        query.setEnergyType(List.of("UNKNOWN_TYPE"));

        PageResult<Car> result = service.search(query);

        assertThat(result.total()).isEqualTo(63);
    }

    @Test
    @DisplayName("可以按零百加速筛选性能车，并支持按加速排序")
    void shouldFilterPerformanceCarsByAcceleration() {
        CarQuery query = new CarQuery();
        query.setMaxAcceleration(4.0);
        query.setSort("ACCELERATION_ASC");
        query.setSize(60);

        PageResult<Car> result = service.search(query);

        assertThat(result.total()).isGreaterThanOrEqualTo(10);
        assertThat(result.items()).allSatisfy(car -> {
            assertThat(car.zeroToHundredSeconds()).isLessThanOrEqualTo(4.0);
            assertThat(car.isPerformanceCar()).isTrue();
        });
        assertThat(result.items()).isSortedAccordingTo(
                (a, b) -> Double.compare(a.zeroToHundredSeconds(), b.zeroToHundredSeconds()));
        assertThat(result.items().get(0).name()).isEqualTo("特斯拉 Model S Plaid");
        assertThat(result.items().get(0).getAccelerationLabel()).isEqualTo("零百 2.1s");
    }

    @Test
    @DisplayName("新增了皮卡与旅行车车身形式，以及加速排序选项")
    void shouldExposeNewFilterOptions() {
        FilterOptions options = service.filterOptions();

        assertThat(options.bodyTypes()).extracting(Option::code)
                .contains("PICKUP", "WAGON", "SPORTS", "COUPE");
        assertThat(options.sorts()).extracting(Option::code)
                .contains("ACCELERATION_ASC", "POWER_DESC");
    }

    @Test
    @DisplayName("不存在的车型抛 404 业务异常")
    void shouldThrowNotFound() {
        assertThatThrownBy(() -> service.getById(9999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("车型不存在");
    }

    @Test
    @DisplayName("对比车型数量不足时抛 400")
    void shouldRejectCompareWithTooFewCars() {
        assertThatThrownBy(() -> service.compare(List.of(1L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("至少需要选择 2 款车");
    }

    @Test
    @DisplayName("对比返回顺序与传入顺序一致")
    void shouldKeepCompareOrder() {
        List<Car> cars = service.compare(List.of(9L, 1L, 5L));

        assertThat(cars).extracting(Car::id).containsExactly(9L, 1L, 5L);
    }
}
