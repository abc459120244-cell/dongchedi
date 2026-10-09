package com.dongchedi.car.repository;

import java.util.List;
import java.util.Optional;

import com.dongchedi.car.model.Car;

/**
 * 车型数据访问接口。
 *
 * <p>定义领域所需的查询能力，具体实现可以是内存实现，也可以替换为 JPA / MyBatis 实现。
 */
public interface CarRepository {

    /**
     * 查询全部车型，按 id 升序返回不可变列表。
     */
    List<Car> findAll();

    Optional<Car> findById(Long id);

    /**
     * 批量按 id 查询，用于对比功能；返回顺序与传入顺序一致。
     */
    List<Car> findAllById(Iterable<Long> ids);

    List<String> findAllBrands();
}
