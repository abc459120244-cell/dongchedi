package com.dongchedi.car.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.dongchedi.car.dto.FavoriteState;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.repository.CarRepository;
import com.dongchedi.car.repository.FavoriteRepository;

/**
 * 收藏服务：按登录用户隔离，数据通过 {@link FavoriteRepository} 存储。
 */
@Service
public class FavoriteService {

    /** 单个用户最多收藏的车型数量 */
    private static final int MAX_FAVORITES = 50;

    private final FavoriteRepository favoriteRepository;
    private final CarRepository carRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, CarRepository carRepository) {
        this.favoriteRepository = favoriteRepository;
        this.carRepository = carRepository;
    }

    public List<Car> list(Long userId) {
        return carRepository.findAllById(favoriteRepository.findByUserId(userId));
    }

    public List<Long> ids(Long userId) {
        return favoriteRepository.findByUserId(userId);
    }

    public FavoriteState add(Long userId, Long carId) {
        requireCar(carId);
        if (!favoriteRepository.findByUserId(userId).contains(carId)
                && favoriteRepository.countByUserId(userId) >= MAX_FAVORITES) {
            throw new BizException(HttpStatus.BAD_REQUEST, "收藏数量已达上限（" + MAX_FAVORITES + " 款）");
        }
        favoriteRepository.add(userId, carId);
        return new FavoriteState(true, favoriteRepository.countByUserId(userId),
                favoriteRepository.findByUserId(userId));
    }

    public FavoriteState remove(Long userId, Long carId) {
        favoriteRepository.remove(userId, carId);
        return new FavoriteState(false, favoriteRepository.countByUserId(userId),
                favoriteRepository.findByUserId(userId));
    }

    public FavoriteState clear(Long userId) {
        favoriteRepository.clear(userId);
        return new FavoriteState(false, 0, List.of());
    }

    public int count(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }

    private void requireCar(Long carId) {
        if (carId == null || carRepository.findById(carId).isEmpty()) {
            throw BizException.notFound("车型不存在: " + carId);
        }
    }
}
