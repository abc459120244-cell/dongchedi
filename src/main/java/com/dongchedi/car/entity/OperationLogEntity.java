package com.dongchedi.car.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 操作审计日志（数据库表 operation_log）。
 *
 * <p>商家每一次新增 / 修改 / 上下架 / 删除车源、处理订单都会落一条日志，
 * 用于证明「前端操作 → 后端业务处理 → 数据库变化」的完整链路。
 */
@Entity
@Table(name = "operation_log", indexes = @Index(name = "idx_log_operator", columnList = "operator_id"))
@Getter
@Setter
@NoArgsConstructor
public class OperationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operator_name", length = 32)
    private String operatorName;

    @Column(name = "operator_role", length = 16)
    private String operatorRole;

    /** 操作名称，例如「新增车源」「处理订单」 */
    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "target_type", length = 16)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 300)
    private String detail;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
