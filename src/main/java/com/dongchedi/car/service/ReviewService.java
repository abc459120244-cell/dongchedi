package com.dongchedi.car.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dongchedi.car.dto.ReviewRequest;
import com.dongchedi.car.dto.ReviewSummary;
import com.dongchedi.car.dto.ReviewView;
import com.dongchedi.car.entity.ReviewEntity;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.CarRepository;
import com.dongchedi.car.repository.jpa.ReviewJpaRepository;

/**
 * 车主评价服务：同一用户对同一车型只保留一条评价，重复提交即修改。
 */
@Service
public class ReviewService {

    private final ReviewJpaRepository jpa;
    private final CarRepository carRepository;

    public ReviewService(ReviewJpaRepository jpa, CarRepository carRepository) {
        this.jpa = jpa;
        this.carRepository = carRepository;
    }

    @Transactional
    public ReviewView submit(UserAccount user, ReviewRequest request) {
        if (request.getCarId() == null) {
            throw BizException.badRequest("请选择车型");
        }
        carRepository.findById(request.getCarId())
                .orElseThrow(() -> BizException.notFound("车型不存在或已下架：" + request.getCarId()));
        int rating = Math.max(1, Math.min(5, request.getRating()));

        ReviewEntity entity = jpa.findByCarIdAndUserId(request.getCarId(), user.id()).orElseGet(() -> {
            ReviewEntity created = new ReviewEntity();
            created.setCarId(request.getCarId());
            created.setUserId(user.id());
            return created;
        });
        entity.setUserName(user.displayName());
        entity.setRating(rating);
        entity.setContent(request.getContent() == null ? "" : request.getContent().trim());
        entity.setUpdatedAt(LocalDateTime.now());
        return ReviewView.from(jpa.save(entity), user.id());
    }

    public List<ReviewView> list(Long carId, Long currentUserId) {
        return jpa.findByCarIdOrderByIdDesc(carId).stream()
                .map(entity -> ReviewView.from(entity, currentUserId))
                .toList();
    }

    public ReviewSummary summary(Long carId) {
        List<ReviewEntity> reviews = jpa.findByCarIdOrderByIdDesc(carId);
        if (reviews.isEmpty()) {
            return ReviewSummary.empty();
        }
        int total = reviews.size();
        int five = 0;
        int four = 0;
        int three = 0;
        int two = 0;
        int one = 0;
        int sum = 0;
        for (ReviewEntity review : reviews) {
            sum += review.getRating();
            switch (review.getRating()) {
                case 5 -> five++;
                case 4 -> four++;
                case 3 -> three++;
                case 2 -> two++;
                default -> one++;
            }
        }
        double average = Math.round((double) sum / total * 10d) / 10d;
        return new ReviewSummary(average, total, five, four, three, two, one);
    }

    @Transactional
    public void delete(UserAccount user, Long reviewId) {
        ReviewEntity entity = jpa.findById(reviewId)
                .orElseThrow(() -> BizException.notFound("评价不存在：" + reviewId));
        if (!user.isAdmin() && !user.id().equals(entity.getUserId())) {
            throw new BizException(HttpStatus.FORBIDDEN, "只能删除自己的评价");
        }
        jpa.delete(entity);
    }
}
