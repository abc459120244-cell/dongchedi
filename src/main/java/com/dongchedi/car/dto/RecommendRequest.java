package com.dongchedi.car.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 智能推荐请求。
 */
public class RecommendRequest {

    @DecimalMin(value = "0", message = "最低预算不能为负数")
    private Double minPrice = 10.0;

    @DecimalMin(value = "0", message = "最高预算不能为负数")
    private Double maxPrice = 20.0;

    /** 用车场景，可多选，取值为 UseScene 枚举名 */
    private List<String> scenes = new ArrayList<>();

    /** 能源偏好，取值为 EnergyPreference 枚举名 */
    private String energyPreference = "ANY";

    @Min(value = 2, message = "座位数至少为 2")
    @Max(value = 9, message = "座位数最多为 9")
    private Integer minSeats = 5;

    /** 选车优先级，取值为 RecommendationPriority 枚举名 */
    private String priority = "COMPREHENSIVE";

    @Min(value = 1, message = "推荐数量至少为 1")
    @Max(value = 20, message = "推荐数量最多为 20")
    private Integer limit;

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public List<String> getScenes() {
        return scenes;
    }

    public void setScenes(List<String> scenes) {
        this.scenes = scenes == null ? new ArrayList<>() : scenes;
    }

    public String getEnergyPreference() {
        return energyPreference;
    }

    public void setEnergyPreference(String energyPreference) {
        this.energyPreference = energyPreference;
    }

    public Integer getMinSeats() {
        return minSeats;
    }

    public void setMinSeats(Integer minSeats) {
        this.minSeats = minSeats;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
