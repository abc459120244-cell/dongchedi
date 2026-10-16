package com.dongchedi.car.dto;

import java.time.LocalDateTime;

import com.dongchedi.car.entity.NotificationEntity;

/**
 * 站内消息视图。
 */
public record NotificationView(
        Long id,
        String type,
        String title,
        String content,
        Long refId,
        boolean read,
        LocalDateTime createdAt) {

    public static NotificationView from(NotificationEntity entity) {
        return new NotificationView(entity.getId(), entity.getType(), entity.getTitle(),
                entity.getContent(), entity.getRefId(), entity.isReadFlag(), entity.getCreatedAt());
    }
}
