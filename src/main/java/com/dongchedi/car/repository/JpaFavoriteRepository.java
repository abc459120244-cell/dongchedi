package com.dongchedi.car.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dongchedi.car.entity.FavoriteEntity;
import com.dongchedi.car.repository.jpa.FavoriteJpaRepository;

/**
 * 基于数据库的收藏仓库实现。
 */
@Repository
public class JpaFavoriteRepository implements FavoriteRepository {

    private final FavoriteJpaRepository jpa;

    public JpaFavoriteRepository(FavoriteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Long> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return jpa.findByUserIdOrderByIdAsc(userId).stream().map(FavoriteEntity::getCarId).toList();
    }

    @Override
    public boolean add(Long userId, Long carId) {
        if (jpa.existsByUserIdAndCarId(userId, carId)) {
            return false;
        }
        jpa.save(new FavoriteEntity(userId, carId));
        return true;
    }

    @Override
    @Transactional
    public boolean remove(Long userId, Long carId) {
        return jpa.deleteByUserIdAndCarId(userId, carId) > 0;
    }

    @Override
    @Transactional
    public void clear(Long userId) {
        jpa.deleteByUserId(userId);
    }

    @Override
    public int countByUserId(Long userId) {
        return jpa.countByUserId(userId);
    }
}
