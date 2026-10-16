package com.dongchedi.car.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.dongchedi.car.entity.CarEntity;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.repository.jpa.CarJpaRepository;

/**
 * 基于数据库的车型仓库实现。
 *
 * <p>对业务层仍然暴露 {@link CarRepository} 接口，只返回「在售」车源；
 * 商家端的车源管理直接使用 {@link CarJpaRepository}，可以看到全部状态。
 */
@Repository
public class JpaCarRepository implements CarRepository {

    private final CarJpaRepository jpa;

    public JpaCarRepository(CarJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Car> findAll() {
        return jpa.findByStatusOrderByIdAsc(CarEntity.STATUS_ON_SALE).stream()
                .map(CarEntity::toModel)
                .toList();
    }

    @Override
    public Optional<Car> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpa.findById(id).filter(CarEntity::isOnSale).map(CarEntity::toModel);
    }

    @Override
    public List<Car> findAllById(Iterable<Long> ids) {
        Map<Long, Car> indexed = new LinkedHashMap<>();
        for (CarEntity entity : jpa.findAllById(ids)) {
            if (entity.isOnSale()) {
                indexed.put(entity.getId(), entity.toModel());
            }
        }
        List<Car> result = new java.util.ArrayList<>();
        for (Long id : ids) {
            Car car = indexed.get(id);
            if (car != null) {
                result.add(car);
            }
        }
        return result;
    }

    @Override
    public List<String> findAllBrands() {
        return findAll().stream().map(Car::brand).distinct().toList();
    }
}
