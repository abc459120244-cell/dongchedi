package com.dongchedi.car.dto;

import java.util.List;

/**
 * 统一分页返回结构。
 */
public record PageResult<T>(
        List<T> items,
        long total,
        int page,
        int size,
        int totalPages,
        boolean hasNext) {

    public static <T> PageResult<T> of(List<T> all, int page, int size) {
        int total = all.size();
        int totalPages = size <= 0 ? 0 : (total + size - 1) / size;
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<T> items = from >= to ? List.of() : List.copyOf(all.subList(from, to));
        return new PageResult<>(items, total, page, size, totalPages, page < totalPages);
    }
}
