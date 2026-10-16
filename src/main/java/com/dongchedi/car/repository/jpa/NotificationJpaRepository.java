package com.dongchedi.car.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dongchedi.car.entity.NotificationEntity;

/**
 * 站内消息 Spring Data JPA 仓库。
 */
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByUserIdOrderByIdDesc(Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);
}
