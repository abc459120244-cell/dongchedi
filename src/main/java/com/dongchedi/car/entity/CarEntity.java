package com.dongchedi.car.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dongchedi.car.model.BodyType;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.model.CarLevel;
import com.dongchedi.car.model.EnergyType;
import com.dongchedi.car.model.UseScene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 车型实体（数据库表 car）。
 *
 * <p>对外仍然暴露不可变的 {@link Car}，实体只负责持久化细节；
 * 枚举以字符串入库，多值字段用逗号分隔存储，方便用 SQL 直接查看。
 */
@Entity
@Table(name = "car")
@Getter
@Setter
@NoArgsConstructor
public class CarEntity {

    /** 在售 */
    public static final String STATUS_ON_SALE = "ON_SALE";
    /** 已下架 */
    public static final String STATUS_OFF_SHELF = "OFF_SHELF";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String brand;

    @Column(length = 16)
    private String level;

    @Column(name = "body_type", length = 16)
    private String bodyType;

    @Column(name = "energy_type", length = 24)
    private String energyType;

    private double price;

    private int seats;

    @Column(name = "length_mm")
    private int lengthMm;

    @Column(name = "width_mm")
    private int widthMm;

    @Column(name = "height_mm")
    private int heightMm;

    @Column(name = "wheelbase_mm")
    private int wheelbaseMm;

    @Column(name = "max_power_kw")
    private int maxPowerKw;

    @Column(name = "zero_to_hundred_seconds")
    private double zeroToHundredSeconds;

    @Column(name = "fuel_consumption")
    private Double fuelConsumption;

    @Column(name = "electric_range_km")
    private Integer electricRangeKm;

    @Column(name = "monthly_sales")
    private int monthlySales;

    @Column(name = "user_score")
    private double userScore;

    /** 逗号分隔的标签 */
    @Column(length = 300)
    private String tags;

    /** 逗号分隔的用车场景（UseScene 枚举名） */
    @Column(length = 300)
    private String scenes;

    @Column(length = 300)
    private String highlight;

    /** 所属商家（用户 id），为空表示平台自营 */
    @Column(name = "dealer_id")
    private Long dealerId;

    @Column(nullable = false, length = 16)
    private String status = STATUS_ON_SALE;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public boolean isOnSale() {
        return STATUS_ON_SALE.equals(status);
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 实体 → 领域模型。
     */
    public Car toModel() {
        return new Car(id, name, brand,
                parse(CarLevel.class, level),
                parse(BodyType.class, bodyType),
                parse(EnergyType.class, energyType),
                price, seats, lengthMm, widthMm, heightMm, wheelbaseMm, maxPowerKw,
                zeroToHundredSeconds, fuelConsumption, electricRangeKm, monthlySales, userScore,
                parseScenes(scenes), splitList(tags), highlight);
    }

    /**
     * 领域模型 → 实体（新增或整体覆盖）。
     */
    public static CarEntity fromModel(Car car) {
        CarEntity entity = new CarEntity();
        entity.applyModel(car);
        entity.setId(car.id());
        return entity;
    }

    public void applyModel(Car car) {
        this.name = car.name();
        this.brand = car.brand();
        this.level = car.level() == null ? null : car.level().name();
        this.bodyType = car.bodyType() == null ? null : car.bodyType().name();
        this.energyType = car.energyType() == null ? null : car.energyType().name();
        this.price = car.price();
        this.seats = car.seats();
        this.lengthMm = car.lengthMm();
        this.widthMm = car.widthMm();
        this.heightMm = car.heightMm();
        this.wheelbaseMm = car.wheelbaseMm();
        this.maxPowerKw = car.maxPowerKw();
        this.zeroToHundredSeconds = car.zeroToHundredSeconds();
        this.fuelConsumption = car.fuelConsumption();
        this.electricRangeKm = car.electricRangeKm();
        this.monthlySales = car.monthlySales();
        this.userScore = car.userScore();
        this.tags = joinList(car.tags());
        this.scenes = joinEnums(car.scenes());
        this.highlight = car.highlight();
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static List<UseScene> parseScenes(String value) {
        List<UseScene> scenes = new ArrayList<>();
        for (String item : splitList(value)) {
            UseScene scene = parse(UseScene.class, item);
            if (scene != null) {
                scenes.add(scene);
            }
        }
        return scenes;
    }

    private static List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
    }

    private static String joinList(List<String> values) {
        return values == null ? null : String.join(",", values);
    }

    private static String joinEnums(List<UseScene> values) {
        return values == null ? null : values.stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse(null);
    }
}
