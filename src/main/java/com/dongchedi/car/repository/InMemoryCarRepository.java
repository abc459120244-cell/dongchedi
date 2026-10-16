package com.dongchedi.car.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.dongchedi.car.model.Car;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于 JSON 数据集的内存实现，应用启动时一次性加载，避免依赖外部数据库即可运行。
 */
public class InMemoryCarRepository implements CarRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCarRepository.class);

    private final List<Car> cars;
    private final Map<Long, Car> carIndex;
    private final List<String> brands;

    public InMemoryCarRepository(ResourceLoader resourceLoader,
                                 ObjectMapper objectMapper,
                                 @Value("${dongchedi.dataset.location:classpath:data/cars.json}") String location)
            throws IOException {
        Resource resource = resourceLoader.getResource(location);
        List<Car> loaded;
        try (InputStream in = resource.getInputStream()) {
            loaded = objectMapper.readValue(in, new TypeReference<List<Car>>() {
            });
        }
        loaded.sort(Comparator.comparing(Car::id));

        this.cars = List.copyOf(loaded);
        Map<Long, Car> index = new LinkedHashMap<>();
        for (Car car : this.cars) {
            index.put(car.id(), car);
        }
        this.carIndex = Map.copyOf(index);
        this.brands = this.cars.stream().map(Car::brand).distinct().toList();

        log.info("已从 {} 加载车型数据 {} 条，覆盖品牌 {} 个", location, this.cars.size(), this.brands.size());
    }

    @Override
    public List<Car> findAll() {
        return cars;
    }

    @Override
    public Optional<Car> findById(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(carIndex.get(id));
    }

    @Override
    public List<Car> findAllById(Iterable<Long> ids) {
        List<Car> result = new ArrayList<>();
        for (Long id : ids) {
            Car car = carIndex.get(id);
            if (car != null) {
                result.add(car);
            }
        }
        return result;
    }

    @Override
    public List<String> findAllBrands() {
        return brands;
    }
}
