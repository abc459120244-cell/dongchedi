package com.dongchedi.car.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 商家端新增 / 编辑车源的请求。
 */
public class CarFormRequest {

    @NotBlank(message = "车型名称不能为空")
    private String name;

    @NotBlank(message = "品牌不能为空")
    private String brand;

    private String level = "COMPACT";
    private String bodyType = "SUV";
    private String energyType = "GASOLINE";

    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private double price;

    @Min(value = 1, message = "座位数至少为 1")
    private int seats = 5;

    private int maxPowerKw = 150;
    private double zeroToHundredSeconds = 8.0;
    private int monthlySales;
    private double userScore = 4.0;
    private Double fuelConsumption;
    private Integer electricRangeKm;
    private String highlight;
    private List<String> tags = new ArrayList<>();
    private List<String> scenes = new ArrayList<>();

    /** ON_SALE / OFF_SHELF */
    private String status = "ON_SALE";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getEnergyType() {
        return energyType;
    }

    public void setEnergyType(String energyType) {
        this.energyType = energyType;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public int getMaxPowerKw() {
        return maxPowerKw;
    }

    public void setMaxPowerKw(int maxPowerKw) {
        this.maxPowerKw = maxPowerKw;
    }

    public double getZeroToHundredSeconds() {
        return zeroToHundredSeconds;
    }

    public void setZeroToHundredSeconds(double zeroToHundredSeconds) {
        this.zeroToHundredSeconds = zeroToHundredSeconds;
    }

    public int getMonthlySales() {
        return monthlySales;
    }

    public void setMonthlySales(int monthlySales) {
        this.monthlySales = monthlySales;
    }

    public double getUserScore() {
        return userScore;
    }

    public void setUserScore(double userScore) {
        this.userScore = userScore;
    }

    public Double getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(Double fuelConsumption) {
        this.fuelConsumption = fuelConsumption;
    }

    public Integer getElectricRangeKm() {
        return electricRangeKm;
    }

    public void setElectricRangeKm(Integer electricRangeKm) {
        this.electricRangeKm = electricRangeKm;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public List<String> getScenes() {
        return scenes;
    }

    public void setScenes(List<String> scenes) {
        this.scenes = scenes == null ? new ArrayList<>() : scenes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
