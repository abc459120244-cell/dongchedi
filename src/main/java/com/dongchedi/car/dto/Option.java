package com.dongchedi.car.dto;

/**
 * 筛选项选项：编码 + 中文名 + 该选项下的车型数量。
 */
public record Option(String code, String label, long count) {
}
