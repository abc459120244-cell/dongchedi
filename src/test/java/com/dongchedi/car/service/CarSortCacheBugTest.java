package com.dongchedi.car.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.dongchedi.car.dto.CarQuery;
import com.dongchedi.car.dto.PageResult;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.repository.CarRepository;
import com.dongchedi.car.repository.InMemoryCarRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 该测试用于暴露「排序结果缓存」引入的缺陷：
 * 缓存只以排序方式作为 key，没有区分筛选条件，导致第二次请求会复用第一次请求的结果。
 */
class CarSortCacheBugTest {

    @Test
    @DisplayName("不同筛选条件下不应复用同一份排序缓存")
    void shouldNotReuseCacheAcrossDifferentFilters() throws Exception {
        CarRepository repository = new InMemoryCarRepository(new DefaultResourceLoader(), new ObjectMapper(),
                "classpath:data/cars.json");
        CarQueryService service = new CarQueryService(repository);

        CarQuery first = new CarQuery();
        first.setEnergyType(List.of("BEV"));
        first.setSort("PRICE_ASC");
        PageResult<Car> firstResult = service.search(first);
        assertThat(firstResult.total()).isEqualTo(10);

        CarQuery second = new CarQuery();
        second.setSort("PRICE_ASC");
        PageResult<Car> secondResult = service.search(second);

        // 期望：不筛选时命中全部 35 款车型；实际：复用了缓存，仍然只有 10 条
        assertThat(secondResult.total()).isEqualTo(35);
    }
}
