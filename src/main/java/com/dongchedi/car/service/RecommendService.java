package com.dongchedi.car.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dongchedi.car.dto.RecommendItem;
import com.dongchedi.car.dto.RecommendRequest;
import com.dongchedi.car.dto.RecommendResult;
import com.dongchedi.car.dto.ScoreDetail;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.model.EnergyPreference;
import com.dongchedi.car.model.EnergyType;
import com.dongchedi.car.model.RecommendationPriority;
import com.dongchedi.car.model.ScoreDimension;
import com.dongchedi.car.model.UseScene;
import com.dongchedi.car.repository.CarRepository;
import com.dongchedi.car.support.Enums;

/**
 * 智能推荐服务。
 *
 * <p>采用「内容特征匹配 + 市场热度 + 用户口碑」的混合打分模型：每个维度先归一化到 0~1，
 * 再按选车优先级加权求和，最终得分区间为 0~100。超出预算 15% 以上的车型在候选阶段直接淘汰。
 */
@Service
public class RecommendService {

    /** 月销量归一化基准，约等于数据集中的最高月销量 */
    private static final double SALES_BASELINE = 35000d;

    /** 动力归一化基准（kW） */
    private static final double POWER_BASELINE = 330d;

    /** 允许超出预算的上浮比例 */
    private static final double BUDGET_TOLERANCE = 1.15d;

    /** 得分保留 1 位小数 */
    private static final double SCORE_SCALE = 10d;

    private final CarRepository carRepository;
    private final int defaultLimit;
    private final int maxLimit;

    public RecommendService(CarRepository carRepository,
                            @Value("${dongchedi.recommend.default-limit:5}") int defaultLimit,
                            @Value("${dongchedi.recommend.max-limit:20}") int maxLimit) {
        this.carRepository = carRepository;
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
    }

    /**
     * 执行推荐计算。
     */
    public RecommendResult recommend(RecommendRequest request) {
        RecommendationPriority priority = RecommendationPriority.of(request.getPriority());
        EnergyPreference energyPreference = EnergyPreference.of(request.getEnergyPreference());
        List<UseScene> scenes = List.copyOf(Enums.parseAll(UseScene.class, request.getScenes()));

        double minPrice = request.getMinPrice() == null ? 0d : request.getMinPrice();
        double maxPrice = request.getMaxPrice() == null ? Double.MAX_VALUE : request.getMaxPrice();
        double budgetCeiling = maxPrice == Double.MAX_VALUE ? maxPrice : maxPrice * BUDGET_TOLERANCE;
        int minSeats = request.getMinSeats() == null ? 5 : request.getMinSeats();
        int limit = resolveLimit(request.getLimit());

        long maxSales = carRepository.findAll().stream().mapToInt(Car::monthlySales).max().orElse(1);
        double salesBaseline = Math.max(SALES_BASELINE, maxSales);

        // 第一步：硬性条件过滤，得到候选集
        List<Car> candidates = new ArrayList<>();
        for (Car car : carRepository.findAll()) {
            if (car.price() > budgetCeiling) {
                continue;
            }
            if (energyPreference == EnergyPreference.NEW_ENERGY && !car.isNewEnergy()) {
                continue;
            }
            if (energyPreference == EnergyPreference.TRADITIONAL && car.isNewEnergy()) {
                continue;
            }
            candidates.add(car);
        }

        // 第二步：多维度加权打分
        List<RecommendItem> scored = new ArrayList<>();
        for (Car car : candidates) {
            Map<ScoreDimension, Double> raw = rawScores(car, minPrice, maxPrice, scenes, energyPreference, minSeats,
                    salesBaseline);
            Map<ScoreDimension, Double> weighted = new EnumMap<>(ScoreDimension.class);
            double total = 0d;
            for (Map.Entry<ScoreDimension, Double> entry : priority.weights().entrySet()) {
                double value = raw.getOrDefault(entry.getKey(), 0d) * entry.getValue();
                weighted.put(entry.getKey(), value);
                total += value;
            }
            scored.add(new RecommendItem(0, round(total), car,
                    buildReasons(car, raw, weighted, minPrice, maxPrice, scenes, minSeats, priority),
                    buildDetails(priority, raw, weighted)));
        }

        // 第三步：按得分排序并截取前 N 条
        scored.sort(Comparator.comparingDouble(RecommendItem::score).reversed()
                .thenComparing(item -> item.car().monthlySales(), Comparator.reverseOrder()));

        List<RecommendItem> ranked = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            RecommendItem item = scored.get(i);
            ranked.add(new RecommendItem(i + 1, item.score(), item.car(), item.reasons(), item.details()));
        }

        String summary = buildSummary(minPrice, maxPrice, scenes, energyPreference, minSeats, priority,
                candidates.size(), ranked.size());
        return new RecommendResult("加权混合推荐（内容特征 + 市场热度 + 用户口碑）", priority.label(), summary,
                candidates.size(), ranked);
    }

    private int resolveLimit(Integer requested) {
        int limit = requested == null ? defaultLimit : requested;
        return Math.max(1, Math.min(limit, maxLimit));
    }

    private Map<ScoreDimension, Double> rawScores(Car car, double minPrice, double maxPrice, List<UseScene> scenes,
                                                  EnergyPreference energyPreference, int minSeats,
                                                  double salesBaseline) {
        Map<ScoreDimension, Double> scores = new EnumMap<>(ScoreDimension.class);
        scores.put(ScoreDimension.BUDGET, budgetScore(car, minPrice, maxPrice));
        scores.put(ScoreDimension.SCENE, sceneScore(car, scenes));
        scores.put(ScoreDimension.ENERGY, energyScore(car, energyPreference));
        scores.put(ScoreDimension.REPUTATION, clamp(car.userScore() / 5d));
        scores.put(ScoreDimension.POPULARITY,
                clamp(Math.log10(car.monthlySales() + 1) / Math.log10(salesBaseline + 1)));
        scores.put(ScoreDimension.SPACE, spaceScore(car, minSeats));
        scores.put(ScoreDimension.ECONOMY, economyScore(car));
        scores.put(ScoreDimension.POWER, clamp(car.maxPowerKw() / POWER_BASELINE));
        return scores;
    }

    /**
     * 预算匹配度：落在预算区间内得分最高（越接近区间中值越高）；低于预算下限说明更省钱，同样给较高分。
     */
    private double budgetScore(Car car, double minPrice, double maxPrice) {
        double price = car.price();
        if (maxPrice == Double.MAX_VALUE) {
            return 0.80d;
        }
        double span = Math.max(maxPrice - minPrice, 1d);
        if (price >= minPrice && price <= maxPrice) {
            double mid = (minPrice + maxPrice) / 2d;
            double deviation = Math.abs(price - mid) / (span / 2d);
            return clamp(1d - 0.15d * deviation);
        }
        if (price < minPrice) {
            double below = Math.min((minPrice - price) / span, 1d);
            return clamp(0.85d - 0.35d * below);
        }
        double over = (price - maxPrice) / Math.max(maxPrice, 1d);
        return clamp(0.55d - over * 3d);
    }

    private double sceneScore(Car car, List<UseScene> scenes) {
        if (scenes.isEmpty()) {
            return 0.70d;
        }
        if (car.scenes() == null || car.scenes().isEmpty()) {
            return 0.20d;
        }
        long hits = scenes.stream().filter(car.scenes()::contains).count();
        if (hits == 0) {
            return 0.15d;
        }
        return clamp(0.70d + 0.30d * hits / scenes.size());
    }

    private double energyScore(Car car, EnergyPreference preference) {
        return switch (preference) {
            case ANY -> 0.80d;
            case NEW_ENERGY -> car.isNewEnergy() ? 1d : 0d;
            case TRADITIONAL -> car.isNewEnergy() ? 0d : 1d;
        };
    }

    private double spaceScore(Car car, int minSeats) {
        double seatRatio = clamp((double) car.seats() / Math.max(minSeats, 1));
        double wheelbaseRatio = clamp((car.wheelbaseMm() - 2400d) / 800d);
        return clamp(seatRatio * 0.7d + wheelbaseRatio * 0.3d);
    }

    /**
     * 能耗经济性：纯电看续航，燃油/混动看百公里油耗。
     */
    private double economyScore(Car car) {
        if (car.energyType() == EnergyType.BEV) {
            int range = car.electricRangeKm() == null ? 0 : car.electricRangeKm();
            return clamp(0.75d + (range - 300d) / 2000d);
        }
        if (car.fuelConsumption() == null) {
            return 0.50d;
        }
        return clamp(1d - (car.fuelConsumption() - 3d) / 8d);
    }

    private List<String> buildReasons(Car car, Map<ScoreDimension, Double> raw, Map<ScoreDimension, Double> weighted,
                                      double minPrice, double maxPrice, List<UseScene> scenes, int minSeats,
                                      RecommendationPriority priority) {
        List<String> reasons = new ArrayList<>();
        reasons.add(String.format(Locale.ROOT, "预算匹配度 %.0f%%：指导价 %.2f 万，需求预算 %.0f~%.0f 万",
                raw.getOrDefault(ScoreDimension.BUDGET, 0d) * 100, car.price(), minPrice, maxPrice));

        if (!scenes.isEmpty() && car.scenes() != null) {
            List<String> hit = scenes.stream().filter(car.scenes()::contains).map(UseScene::label).toList();
            if (hit.isEmpty()) {
                reasons.add("场景匹配度较低：该车定位为 " + String.join("、", car.getSceneLabels()));
            } else {
                reasons.add("场景命中：" + String.join("、", hit)
                        + "（该车全部定位：" + String.join("、", car.getSceneLabels()) + "）");
            }
        } else {
            reasons.add("未指定用车场景，主要依据口碑与市场热度排序");
        }

        reasons.add(String.format("用户口碑 %.1f 分（满分 5 分），月销 %s 辆",
                car.userScore(), String.format(Locale.ROOT, "%,d", car.monthlySales())));
        reasons.add("能耗表现：" + car.getEnergyDescription());

        if (car.seats() >= minSeats) {
            reasons.add(String.format("空间满足 %d 座需求，轴距 %dmm", car.seats(), car.wheelbaseMm()));
        } else {
            reasons.add(String.format("空间略低于 %d 座需求，实际为 %d 座", minSeats, car.seats()));
        }
        reasons.add("贡献最大的维度：" + topDimension(weighted, priority));
        return List.copyOf(reasons);
    }

    private String topDimension(Map<ScoreDimension, Double> weighted, RecommendationPriority priority) {
        return weighted.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> String.format(Locale.ROOT, "%s（权重 %.0f，贡献 %.1f 分）", entry.getKey().label(),
                        priority.weights().getOrDefault(entry.getKey(), 0d), entry.getValue()))
                .orElse(priority.label());
    }

    private List<ScoreDetail> buildDetails(RecommendationPriority priority, Map<ScoreDimension, Double> raw,
                                           Map<ScoreDimension, Double> weighted) {
        List<ScoreDetail> details = new ArrayList<>();
        for (ScoreDimension dimension : ScoreDimension.values()) {
            if (!priority.weights().containsKey(dimension)) {
                continue;
            }
            details.add(new ScoreDetail(dimension.name(), dimension.label(),
                    round(raw.getOrDefault(dimension, 0d)),
                    priority.weights().get(dimension),
                    round(weighted.getOrDefault(dimension, 0d))));
        }
        return List.copyOf(details);
    }

    private String buildSummary(double minPrice, double maxPrice, List<UseScene> scenes,
                                EnergyPreference energyPreference, int minSeats,
                                RecommendationPriority priority, int candidateCount, int resultCount) {
        String sceneText = scenes.isEmpty() ? "未指定场景"
                : String.join("、", scenes.stream().map(UseScene::label).toList());
        return String.format(Locale.ROOT,
                "共 %d 款车型进入候选（预算 %.0f~%.0f 万、场景 %s、%s、至少 %d 座），"
                        + "按「%s」策略加权打分后返回前 %d 款。",
                candidateCount, minPrice, maxPrice, sceneText, energyPreference.label(), minSeats,
                priority.label(), resultCount);
    }

    private static double clamp(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    private static double round(double value) {
        return Math.round(value * SCORE_SCALE) / SCORE_SCALE;
    }
}
