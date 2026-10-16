package com.dongchedi.car.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dongchedi.car.entity.OrderEntity;

/**
 * 订单 Spring Data JPA 仓库。
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByBuyerIdOrderByIdDesc(Long buyerId);

    List<OrderEntity> findByDealerIdOrderByIdDesc(Long dealerId);

    List<OrderEntity> findByDealerIdAndStatusOrderByIdDesc(Long dealerId, String status);

    long countByDealerIdAndStatus(Long dealerId, String status);

    long countByStatus(String status);
}
