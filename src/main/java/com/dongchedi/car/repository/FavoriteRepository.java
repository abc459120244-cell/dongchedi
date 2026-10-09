package com.dongchedi.car.repository;

import java.util.List;

/**
 * 用户收藏数据访问接口，按用户隔离。
 */
public interface FavoriteRepository {

    /**
     * 返回用户收藏的车型 id，按收藏先后顺序排列。
     */
    List<Long> findByUserId(Long userId);

    boolean add(Long userId, Long carId);

    boolean remove(Long userId, Long carId);

    void clear(Long userId);

    int countByUserId(Long userId);
}
