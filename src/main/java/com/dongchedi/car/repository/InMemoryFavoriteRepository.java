package com.dongchedi.car.repository;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

/**
 * 内存收藏仓库，重启后重置，接口设计可直接替换为数据库实现。
 */
@Repository
public class InMemoryFavoriteRepository implements FavoriteRepository {

    private final Map<Long, Set<Long>> favoritesByUser = new ConcurrentHashMap<>();

    @Override
    public List<Long> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        Set<Long> favorites = favoritesByUser.get(userId);
        if (favorites == null) {
            return List.of();
        }
        synchronized (favorites) {
            return List.copyOf(favorites);
        }
    }

    @Override
    public boolean add(Long userId, Long carId) {
        return bucket(userId).add(carId);
    }

    @Override
    public boolean remove(Long userId, Long carId) {
        Set<Long> favorites = favoritesByUser.get(userId);
        return favorites != null && favorites.remove(carId);
    }

    @Override
    public void clear(Long userId) {
        Set<Long> favorites = favoritesByUser.get(userId);
        if (favorites != null) {
            favorites.clear();
        }
    }

    @Override
    public int countByUserId(Long userId) {
        Set<Long> favorites = favoritesByUser.get(userId);
        return favorites == null ? 0 : favorites.size();
    }

    private Set<Long> bucket(Long userId) {
        return favoritesByUser.computeIfAbsent(userId,
                key -> Collections.synchronizedSet(new LinkedHashSet<>()));
    }
}
