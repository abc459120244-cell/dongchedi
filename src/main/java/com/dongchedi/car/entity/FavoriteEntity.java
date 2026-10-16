package com.dongchedi.car.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 收藏实体（数据库表 favorite），按用户 + 车型唯一。
 */
@Entity
@Table(name = "favorite",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorite_user_car", columnNames = {"user_id", "car_id"}),
        indexes = @Index(name = "idx_favorite_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class FavoriteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public FavoriteEntity(Long userId, Long carId) {
        this.userId = userId;
        this.carId = carId;
    }
}
