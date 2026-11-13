package com.dongchedi.car.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.ReviewRequest;
import com.dongchedi.car.dto.ReviewView;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.service.ReviewService;
import com.dongchedi.car.support.SessionKeys;

import jakarta.validation.Valid;

/**
 * 车主评价接口（需要登录）。
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 提交或修改自己对某车型的评价。
     */
    @PostMapping
    public ReviewView submit(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                             @Valid @RequestBody ReviewRequest request) {
        return reviewService.submit(user, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                                      @PathVariable Long id) {
        reviewService.delete(user, id);
        return Map.of("message", "评价已删除");
    }
}
