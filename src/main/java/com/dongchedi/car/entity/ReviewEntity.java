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
 * 车主评价实体（数据库表 review）。
 *
 * <p>同一用户对同一车型只保留一条评价，重复提交视为修改。
 */
@Entity
@Table(name = "review",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_user_car", columnNames = {"car_id", "user_id"}),
        indexes = @Index(name = "idx_review_car", columnList = "car_id"))
@Getter
@Setter
@NoArgsConstructor
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", length = 32)
    private String userName;

    /** 1~5 星 */
    @Column(nullable = false)
    private int rating;

    @Column(length = 500)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
