package com.dongchedi.car.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.dongchedi.car.dto.BrandRanking;
import com.dongchedi.car.dto.CarRanking;
import com.dongchedi.car.dto.CountItem;
import com.dongchedi.car.dto.StatisticsOverview;
import com.dongchedi.car.model.BodyType;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.model.CarLevel;
import com.dongchedi.car.model.EnergyType;
import com.dongchedi.car.model.UseScene;
import com.dongchedi.car.repository.CarRepository;

/**
 * 统计分析服务：市场概览、品牌销量榜与各类分布统计。
 */
@Service
public class StatisticsService {

    /** 价格分布区间：上限为 null 表示不封顶 */
    private static final List<PriceBucket> PRICE_BUCKETS = List.of(
            new PriceBucket("10 万以内", 0d, 10d),
            new PriceBucket("10~15 万", 10d, 15d),
            new PriceBucket("15~20 万", 15d, 20d),
            new PriceBucket("20~30 万", 20d, 30d),
            new PriceBucket("30~40 万", 30d, 40d),
            new PriceBucket("40 万以上", 40d, null));

    private final CarRepository carRepository;

    public StatisticsService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public StatisticsOverview overview() {
        List<Car> cars = carRepository.findAll();
        int total = cars.size();
        double averagePrice = cars.stream().mapToDouble(Car::price).average().orElse(0);
        double minPrice = cars.stream().mapToDouble(Car::price).min().orElse(0);
        double maxPrice = cars.stream().mapToDouble(Car::price).max().orElse(0);
        double averageScore = cars.stream().mapToDouble(Car::userScore).average().orElse(0);
        long totalSales = cars.stream().mapToLong(Car::monthlySales).sum();
        long newEnergyCount = cars.stream().filter(Car::isNewEnergy).count();

        return new StatisticsOverview(
                total,
                (int) cars.stream().map(Car::brand).distinct().count(),
                (int) cars.stream().filter(Car::isPerformanceCar).count(),
                round(averagePrice),
                round(minPrice),
                round(maxPrice),
                round(averageScore),
                totalSales,
                round((double) newEnergyCount / Math.max(total, 1)),
                countByEnum(cars, EnergyType.class, Car::energyType),
                countByEnum(cars, BodyType.class, Car::bodyType),
                countByEnum(cars, CarLevel.class, Car::level),
                priceDistribution(cars));
    }

    /**
     * 品牌销量排行榜，按品牌当月总销量降序。
     */
    public List<BrandRanking> brandRankings() {
        Map<String, List<Car>> grouped = new LinkedHashMap<>();
        for (Car car : carRepository.findAll()) {
            grouped.computeIfAbsent(car.brand(), key -> new ArrayList<>()).add(car);
        }

        List<BrandRanking> rankings = new ArrayList<>();
        for (Map.Entry<String, List<Car>> entry : grouped.entrySet()) {
            List<Car> cars = entry.getValue();
            long totalSales = cars.stream().mapToLong(Car::monthlySales).sum();
            String bestSeller = cars.stream()
                    .max(Comparator.comparingInt(Car::monthlySales))
                    .map(Car::name)
                    .orElse("-");
            rankings.add(new BrandRanking(0, entry.getKey(), cars.size(), totalSales,
                    round(cars.stream().mapToDouble(Car::userScore).average().orElse(0)),
                    round(cars.stream().mapToDouble(Car::price).average().orElse(0)),
                    bestSeller));
        }
        rankings.sort(Comparator.comparingLong(BrandRanking::totalMonthlySales).reversed());

        List<BrandRanking> ranked = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) {
            BrandRanking item = rankings.get(i);
            ranked.add(new BrandRanking(i + 1, item.brand(), item.carCount(), item.totalMonthlySales(),
                    item.averageScore(), item.averagePrice(), item.bestSeller()));
        }
        return List.copyOf(ranked);
    }

    /**
     * 用车场景分布，一辆车可能命中多个场景。
     */
    public List<CountItem> sceneDistribution() {
        List<Car> cars = carRepository.findAll();
        List<CountItem> items = new ArrayList<>();
        for (UseScene scene : UseScene.values()) {
            long count = cars.stream()
                    .filter(car -> car.scenes() != null && car.scenes().contains(scene))
                    .count();
            if (count > 0) {
                items.add(new CountItem(scene.name(), scene.label(), count,
                        round((double) count / Math.max(cars.size(), 1))));
            }
        }
        items.sort(Comparator.comparingLong(CountItem::count).reversed());
        return List.copyOf(items);
    }

    /**
     * 车型销量排行榜。
     *
     * @param limit 返回条数，1~50
     * @param scope 统计范围：ALL 全部、NEW_ENERGY 新能源、TRADITIONAL 燃油/油混
     * @param brand 可选，限定品牌
     * @param bodyType 可选，限定车身形式（如 SUV、SEDAN）
     */
    public List<CarRanking> carRankings(int limit, String scope, String brand, String bodyType) {
        List<Car> cars = carRepository.findAll().stream()
                .filter(car -> matchesScope(car, scope))
                .filter(car -> brand == null || brand.isBlank() || car.brand().equals(brand.trim()))
                .filter(car -> matchesBodyType(car, bodyType))
                .sorted(Comparator.comparingInt(Car::monthlySales).reversed()
                        .thenComparing(Comparator.comparingDouble(Car::userScore).reversed()))
                .toList();

        long totalSales = cars.stream().mapToLong(Car::monthlySales).sum();
        int size = Math.max(1, Math.min(limit, 50));
        List<CarRanking> rankings = new ArrayList<>();
        for (int i = 0; i < Math.min(size, cars.size()); i++) {
            Car car = cars.get(i);
            rankings.add(new CarRanking(
                    i + 1,
                    car.id(),
                    car.name(),
                    car.brand(),
                    car.bodyType() == null ? null : car.bodyType().name(),
                    car.getBodyTypeLabel(),
                    car.getEnergyTypeLabel(),
                    car.price(),
                    car.monthlySales(),
                    car.userScore(),
                    totalSales == 0 ? 0 : round((double) car.monthlySales() / totalSales)));
        }
        return List.copyOf(rankings);
    }

    private boolean matchesScope(Car car, String scope) {
        if (scope == null || scope.isBlank() || "ALL".equalsIgnoreCase(scope)) {
            return true;
        }
        if ("NEW_ENERGY".equalsIgnoreCase(scope)) {
            return car.isNewEnergy();
        }
        if ("TRADITIONAL".equalsIgnoreCase(scope)) {
            return !car.isNewEnergy();
        }
        if ("PERFORMANCE".equalsIgnoreCase(scope)) {
            return car.isPerformanceCar();
        }
        return true;
    }

    private boolean matchesBodyType(Car car, String bodyType) {
        if (bodyType == null || bodyType.isBlank()) {
            return true;
        }
        return car.bodyType() != null && car.bodyType().name().equalsIgnoreCase(bodyType.trim());
    }

    private List<CountItem> priceDistribution(List<Car> cars) {
        List<CountItem> items = new ArrayList<>();
        for (PriceBucket bucket : PRICE_BUCKETS) {
            long count = cars.stream().filter(bucket::contains).count();
            items.add(new CountItem(bucket.label(), bucket.label(), count,
                    round((double) count / Math.max(cars.size(), 1))));
        }
        return List.copyOf(items);
    }

    private <E extends Enum<E>> List<CountItem> countByEnum(List<Car> cars, Class<E> type,
                                                            Function<Car, E> extractor) {
        List<CountItem> items = new ArrayList<>();
        for (E value : type.getEnumConstants()) {
            long count = cars.stream().filter(car -> value.equals(extractor.apply(car))).count();
            if (count > 0) {
                items.add(new CountItem(value.name(), labelOf(value), count,
                        round((double) count / Math.max(cars.size(), 1))));
            }
        }
        items.sort(Comparator.comparingLong(CountItem::count).reversed());
        return List.copyOf(items);
    }

    private static String labelOf(Enum<?> value) {
        if (value instanceof EnergyType energyType) {
            return energyType.label();
        }
        if (value instanceof BodyType bodyType) {
            return bodyType.label();
        }
        if (value instanceof CarLevel level) {
            return level.label();
        }
        return value.name();
    }

    private static double round(double value) {
        return Math.round(value * 100d) / 100d;
    }

    /**
     * 价格区间。
     */
    private record PriceBucket(String label, double min, Double max) {

        boolean contains(Car car) {
            if (car.price() < min) {
                return false;
            }
            return max == null || car.price() < max;
        }
    }
}
