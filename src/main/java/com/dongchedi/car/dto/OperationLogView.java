package com.dongchedi.car.dto;

import java.time.LocalDateTime;

import com.dongchedi.car.entity.OperationLogEntity;

/**
 * 操作日志展示对象。
 */
public record OperationLogView(
        Long id,
        String operatorName,
        String operatorRole,
        String action,
        String targetType,
        Long targetId,
        String detail,
        LocalDateTime createdAt) {

    public static OperationLogView from(OperationLogEntity entity) {
        return new OperationLogView(entity.getId(), entity.getOperatorName(), entity.getOperatorRole(),
                entity.getAction(), entity.getTargetType(), entity.getTargetId(),
                entity.getDetail(), entity.getCreatedAt());
    }
}
