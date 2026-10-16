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
 * 购车订单实体（数据库表 car_order）。
 *
 * <p>买家在车型页提交「预约试驾 / 询价 / 意向定金」后生成订单，归属到该车源的商家。
 */
@Entity
@Table(name = "car_order",
        indexes = {
                @Index(name = "idx_order_dealer", columnList = "dealer_id"),
                @Index(name = "idx_order_buyer", columnList = "buyer_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    @Column(name = "car_name", length = 64)
    private String carName;

    /** 车源所属商家 */
    @Column(name = "dealer_id")
    private Long dealerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "buyer_name", length = 32)
    private String buyerName;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(name = "contact_name", length = 32)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(length = 300)
    private String remark;

    @Column(nullable = false, length = 16)
    private String status;

    /** 商家回复 */
    @Column(name = "dealer_reply", length = 300)
    private String dealerReply;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
