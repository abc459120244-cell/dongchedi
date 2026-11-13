package com.dongchedi.car.repository.jpa;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dongchedi.car.entity.OperationLogEntity;

/**
 * 审计日志 Spring Data JPA 仓库。
 */
public interface OperationLogJpaRepository extends JpaRepository<OperationLogEntity, Long> {

    List<OperationLogEntity> findAllByOrderByIdDesc(Pageable pageable);

    List<OperationLogEntity> findByOperatorIdOrderByIdDesc(Long operatorId, Pageable pageable);
}
