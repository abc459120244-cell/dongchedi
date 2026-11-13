package com.dongchedi.car.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dongchedi.car.dto.CarDetailView;
import com.dongchedi.car.dto.ReviewSummary;
import com.dongchedi.car.dto.ReviewView;
import com.dongchedi.car.entity.CarEntity;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.jpa.CarJpaRepository;
import com.dongchedi.car.repository.jpa.UserJpaRepository;

/**
 * 车型详情聚合服务：车型参数 + 车源商家 + 车主评价。
 */
@Service
public class CarDetailService {

    private final CarQueryService carQueryService;
    private final CarJpaRepository carJpa;
    private final UserJpaRepository userJpa;
    private final ReviewService reviewService;

    public CarDetailService(CarQueryService carQueryService, CarJpaRepository carJpa,
                            UserJpaRepository userJpa, ReviewService reviewService) {
        this.carQueryService = carQueryService;
        this.carJpa = carJpa;
        this.userJpa = userJpa;
        this.reviewService = reviewService;
    }

    public CarDetailView get(Long carId, UserAccount currentUser) {
        Car car = carQueryService.getById(carId);
        CarEntity entity = carJpa.findById(carId).orElse(null);
        Long dealerId = entity == null ? null : entity.getDealerId();
        String dealerName = dealerId == null ? "平台自营"
                : userJpa.findById(dealerId)
                .map(user -> user.getNickname() == null ? user.getUsername() : user.getNickname())
                .orElse("平台自营");

        Long currentUserId = currentUser == null ? null : currentUser.id();
        ReviewSummary summary = reviewService.summary(carId);
        List<ReviewView> reviews = reviewService.list(carId, currentUserId);
        return new CarDetailView(car, dealerId, dealerName, summary, reviews);
    }
}
