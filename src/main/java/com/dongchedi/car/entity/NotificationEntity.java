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
 * 站内消息实体（数据库表 notification）。
 *
 * <p>买家下单后给商家落一条消息，商家处理后给买家落一条消息，
 * 前端通过 SSE 实时推送 + 页面拉取未读数量两种方式展示。
 */
@Entity
@Table(name = "notification", indexes = @Index(name = "idx_notification_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收者用户 id */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 24)
    private String type;

    @Column(length = 64)
    private String title;

    @Column(length = 300)
    private String content;

    /** 关联业务 id（订单 id 等） */
    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "read_flag", nullable = false)
    private boolean readFlag;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
