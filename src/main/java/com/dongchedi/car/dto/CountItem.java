package com.dongchedi.car.dto;

/**
 * 统计图表用的计数项。
 */
public record CountItem(String code, String label, long count, double ratio) {
}
