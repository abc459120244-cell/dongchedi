package com.dongchedi.car.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dongchedi.car.entity.FavoriteEntity;

/**
 * 收藏 Spring Data JPA 仓库。
 */
public interface FavoriteJpaRepository extends JpaRepository<FavoriteEntity, Long> {

    List<FavoriteEntity> findByUserIdOrderByIdAsc(Long userId);

    boolean existsByUserIdAndCarId(Long userId, Long carId);

    long deleteByUserIdAndCarId(Long userId, Long carId);

    long deleteByUserId(Long userId);

    long deleteByCarId(Long carId);

    int countByUserId(Long userId);
}
