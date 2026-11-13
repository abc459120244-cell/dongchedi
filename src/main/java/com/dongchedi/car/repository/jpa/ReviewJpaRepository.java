package com.dongchedi.car.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dongchedi.car.entity.ReviewEntity;

/**
 * 评价 Spring Data JPA 仓库。
 */
public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findByCarIdOrderByIdDesc(Long carId);

    Optional<ReviewEntity> findByCarIdAndUserId(Long carId, Long userId);

    long countByCarId(Long carId);
}
