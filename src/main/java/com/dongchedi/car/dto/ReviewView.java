package com.dongchedi.car.dto;

import java.time.LocalDateTime;

import com.dongchedi.car.entity.ReviewEntity;

/**
 * 评价展示对象。
 */
public record ReviewView(
        Long id,
        Long carId,
        Long userId,
        String userName,
        int rating,
        String content,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ReviewView from(ReviewEntity entity, Long currentUserId) {
        return new ReviewView(entity.getId(), entity.getCarId(), entity.getUserId(), entity.getUserName(),
                entity.getRating(), entity.getContent(),
                currentUserId != null && currentUserId.equals(entity.getUserId()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
