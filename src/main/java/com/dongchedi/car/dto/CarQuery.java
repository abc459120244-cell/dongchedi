package com.dongchedi.car.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 车型筛选条件（Web 层绑定对象）。
 *
 * <p>所有字段都是可选的，未传即表示不限制该维度。
 */
public class CarQuery {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 60;

    /** 关键字：匹配车型名、品牌、标签、一句话卖点 */
    private String keyword;

    private List<String> brand = new ArrayList<>();
    private List<String> level = new ArrayList<>();
    private List<String> bodyType = new ArrayList<>();
    private List<String> energyType = new ArrayList<>();
    private List<String> scene = new ArrayList<>();

    private Double minPrice;
    private Double maxPrice;

    private Integer minSeats;
    private Integer minRange;
    private Double maxFuel;
    private Double minScore;

    private String sort = "SALES_DESC";
    private int page = 1;
    private int size = DEFAULT_PAGE_SIZE;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getBrand() {
        return brand;
    }

    public void setBrand(List<String> brand) {
        this.brand = nullSafe(brand);
    }

    public List<String> getLevel() {
        return level;
    }

    public void setLevel(List<String> level) {
        this.level = nullSafe(level);
    }

    public List<String> getBodyType() {
        return bodyType;
    }

    public void setBodyType(List<String> bodyType) {
        this.bodyType = nullSafe(bodyType);
    }

    public List<String> getEnergyType() {
        return energyType;
    }

    public void setEnergyType(List<String> energyType) {
        this.energyType = nullSafe(energyType);
    }

    public List<String> getScene() {
        return scene;
    }

    public void setScene(List<String> scene) {
        this.scene = nullSafe(scene);
    }

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

    public Integer getMinSeats() {
        return minSeats;
    }

    public void setMinSeats(Integer minSeats) {
        this.minSeats = minSeats;
    }

    public Integer getMinRange() {
        return minRange;
    }

    public void setMinRange(Integer minRange) {
        this.minRange = minRange;
    }

    public Double getMaxFuel() {
        return maxFuel;
    }

    public void setMaxFuel(Double maxFuel) {
        this.maxFuel = maxFuel;
    }

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * 归一化分页参数，避免 -1、100000 之类的越界值。
     */
    public int normalizedPage() {
        return Math.max(page, 1);
    }

    public int normalizedSize() {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static List<String> nullSafe(List<String> values) {
        return values == null ? new ArrayList<>() : values;
    }
}
