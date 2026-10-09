package com.dongchedi.car.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.dongchedi.car.dto.CarFilter;
import com.dongchedi.car.dto.CarQuery;
import com.dongchedi.car.dto.CarSort;
import com.dongchedi.car.dto.FilterOptions;
import com.dongchedi.car.dto.Option;
import com.dongchedi.car.dto.PageResult;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.BodyType;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.model.CarLevel;
import com.dongchedi.car.model.EnergyType;
import com.dongchedi.car.model.UseScene;
import com.dongchedi.car.repository.CarRepository;
import com.dongchedi.car.support.Enums;

/**
 * 车型筛选查询服务：负责多条件过滤、排序与分页。
 */
@Service
public class CarQueryService {

    private static final List<Integer> SEAT_OPTIONS = List.of(4, 5, 6, 7);

    private final CarRepository carRepository;

    /** 排序结果缓存，避免每次请求都重新排序（⚠️ 本次提交引入了缺陷，见下文测试） */
    private final Map<String, List<Car>> sortCache = new ConcurrentHashMap<>();

    public CarQueryService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public PageResult<Car> search(CarQuery query) {
        CarFilter filter = CarFilter.from(query);
        List<Car> matched = new ArrayList<>();
        for (Car car : carRepository.findAll()) {
            if (filter.matches(car)) {
                matched.add(car);
            }
        }
        CarSort sort = CarSort.of(query.getSort());
        List<Car> sorted = sortCache.computeIfAbsent(sort.name(), key -> {
            List<Car> copy = new ArrayList<>(matched);
            Comparator<Car> comparator = sort.comparator();
            copy.sort(comparator);
            return copy;
        });
        return PageResult.of(sorted, query.normalizedPage(), query.normalizedSize());
    }

    public Car getById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("车型不存在: " + id));
    }

    /**
     * 车型对比：2~3 款车横向对比。
     */
    public List<Car> compare(List<Long> ids) {
        if (ids == null || ids.size() < 2) {
            throw BizException.badRequest("车型对比至少需要选择 2 款车");
        }
        if (ids.size() > 4) {
            throw BizException.badRequest("车型对比最多支持 4 款车");
        }
        List<Car> cars = carRepository.findAllById(ids);
        if (cars.size() != ids.size()) {
            throw BizException.notFound("部分车型不存在，请重新选择");
        }
        return cars;
    }

    /**
     * 筛选面板可选项，附带每个选项的车型数量，供前端展示。
     */
    public FilterOptions filterOptions() {
        List<Car> all = carRepository.findAll();
        double minPrice = all.stream().mapToDouble(Car::price).min().orElse(0);
        double maxPrice = all.stream().mapToDouble(Car::price).max().orElse(0);

        List<Option> brands = carRepository.findAllBrands().stream()
                .map(brand -> new Option(brand, brand, countBy(all, car -> car.brand().equals(brand))))
                .toList();
        List<Option> levels = toOptions(CarLevel.values(), all, value -> car -> value.equals(car.level()));
        List<Option> bodyTypes = toOptions(BodyType.values(), all, value -> car -> value.equals(car.bodyType()));
        List<Option> energyTypes = toOptions(EnergyType.values(), all, value -> car -> value.equals(car.energyType()));
        List<Option> scenes = toOptions(UseScene.values(), all,
                value -> car -> car.scenes() != null && car.scenes().contains(value));
        List<Option> sorts = List.of(CarSort.values()).stream()
                .map(sort -> new Option(sort.name(), sort.label(), 0))
                .toList();

        return new FilterOptions(brands, levels, bodyTypes, energyTypes, scenes, sorts,
                SEAT_OPTIONS, minPrice, maxPrice, all.size());
    }

    private static <E extends Enum<E>> List<Option> toOptions(E[] values, List<Car> cars,
                                                              java.util.function.Function<E,
                                                                      java.util.function.Predicate<Car>> matcher) {
        List<Option> options = new ArrayList<>();
        for (E value : values) {
            long count = countBy(cars, matcher.apply(value));
            if (count > 0) {
                options.add(new Option(value.name(), labelOf(value), count));
            }
        }
        return List.copyOf(options);
    }

    private static String labelOf(Enum<?> value) {
        if (value instanceof CarLevel level) {
            return level.label();
        }
        if (value instanceof BodyType bodyType) {
            return bodyType.label();
        }
        if (value instanceof EnergyType energyType) {
            return energyType.label();
        }
        if (value instanceof UseScene scene) {
            return scene.label();
        }
        return value.name();
    }

    private static long countBy(List<Car> cars, java.util.function.Predicate<Car> predicate) {
        return cars.stream().filter(predicate).count();
    }
}
