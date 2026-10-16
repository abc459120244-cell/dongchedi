package com.dongchedi.car.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dongchedi.car.entity.CarEntity;

/**
 * 车型 Spring Data JPA 仓库。
 */
public interface CarJpaRepository extends JpaRepository<CarEntity, Long> {

    List<CarEntity> findByStatusOrderByIdAsc(String status);

    List<CarEntity> findByDealerIdOrderByIdAsc(Long dealerId);

    long countByDealerId(Long dealerId);

    long countByDealerIdAndStatus(Long dealerId, String status);

    List<CarEntity> findByBrandAndStatusOrderByIdAsc(String brand, String status);
}
