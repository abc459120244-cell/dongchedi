package com.dongchedi.car.dto;

import java.util.Locale;
import java.util.Set;

import com.dongchedi.car.model.BodyType;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.model.CarLevel;
import com.dongchedi.car.model.EnergyType;
import com.dongchedi.car.model.UseScene;
import com.dongchedi.car.support.Enums;

/**
 * 归一化之后的筛选条件，可安全地复用为内存过滤谓词。
 */
public record CarFilter(
        Set<String> brands,
        Set<CarLevel> levels,
        Set<BodyType> bodyTypes,
        Set<EnergyType> energyTypes,
        Set<UseScene> scenes,
        String keyword,
        Double minPrice,
        Double maxPrice,
        Integer minSeats,
        Integer minRange,
        Double maxFuel,
        Double minScore) {

    public static CarFilter from(CarQuery query) {
        return new CarFilter(
                query.getBrand() == null ? Set.of() : Set.copyOf(query.getBrand()),
                Enums.parseAll(CarLevel.class, query.getLevel()),
                Enums.parseAll(BodyType.class, query.getBodyType()),
                Enums.parseAll(EnergyType.class, query.getEnergyType()),
                Enums.parseAll(UseScene.class, query.getScene()),
                query.getKeyword() == null ? "" : query.getKeyword().trim().toLowerCase(Locale.ROOT),
                query.getMinPrice(),
                query.getMaxPrice(),
                query.getMinSeats(),
                query.getMinRange(),
                query.getMaxFuel(),
                query.getMinScore());
    }

    /**
     * 多条件组合匹配，所有维度之间是「与」的关系，同一维度内的多选是「或」的关系。
     */
    public boolean matches(Car car) {
        if (!brands.isEmpty() && !brands.contains(car.brand())) {
            return false;
        }
        if (!levels.isEmpty() && !levels.contains(car.level())) {
            return false;
        }
        if (!bodyTypes.isEmpty() && !bodyTypes.contains(car.bodyType())) {
            return false;
        }
        if (!energyTypes.isEmpty() && !energyTypes.contains(car.energyType())) {
            return false;
        }
        if (!scenes.isEmpty()) {
            if (car.scenes() == null || scenes.stream().noneMatch(car.scenes()::contains)) {
                return false;
            }
        }
        if (minPrice != null && car.price() < minPrice) {
            return false;
        }
        if (maxPrice != null && car.price() > maxPrice) {
            return false;
        }
        if (minSeats != null && car.seats() < minSeats) {
            return false;
        }
        if (minRange != null && (car.electricRangeKm() == null || car.electricRangeKm() < minRange)) {
            return false;
        }
        if (maxFuel != null && (car.fuelConsumption() == null || car.fuelConsumption() > maxFuel)) {
            return false;
        }
        if (minScore != null && car.userScore() < minScore) {
            return false;
        }
        return matchesKeyword(car);
    }

    private boolean matchesKeyword(Car car) {
        if (keyword.isEmpty()) {
            return true;
        }
        String haystack = String.join(" ",
                car.name(),
                car.brand(),
                car.getBodyTypeLabel() == null ? "" : car.getBodyTypeLabel(),
                car.getLevelLabel() == null ? "" : car.getLevelLabel(),
                car.getEnergyTypeLabel() == null ? "" : car.getEnergyTypeLabel(),
                car.highlight() == null ? "" : car.highlight(),
                car.tags() == null ? "" : String.join(" ", car.tags())).toLowerCase(Locale.ROOT);
        for (String token : keyword.split("\\s+")) {
            if (!token.isBlank() && !haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }
}
