package com.dongchedi.car.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dongchedi.car.dto.CarFormRequest;
import com.dongchedi.car.dto.MerchantCarView;
import com.dongchedi.car.dto.MerchantSummary;
import com.dongchedi.car.entity.CarEntity;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.jpa.CarJpaRepository;
import com.dongchedi.car.repository.jpa.FavoriteJpaRepository;
import com.dongchedi.car.repository.jpa.OrderJpaRepository;

/**
 * 商家端车源管理：新增、修改、上下架、删除。
 *
 * <p>商家只能管理自己名下的车源，管理员可以管理全部车源。
 */
@Service
public class MerchantCarService {

    /** 新增车源时未填写的尺寸给默认值，避免前端展示 0 */
    private static final int DEFAULT_LENGTH_MM = 4650;
    private static final int DEFAULT_WIDTH_MM = 1850;
    private static final int DEFAULT_HEIGHT_MM = 1550;
    private static final int DEFAULT_WHEELBASE_MM = 2750;

    private final CarJpaRepository carJpa;
    private final FavoriteJpaRepository favoriteJpa;
    private final OrderJpaRepository orderJpa;
    private final NotificationService notifications;

    public MerchantCarService(CarJpaRepository carJpa, FavoriteJpaRepository favoriteJpa,
                              OrderJpaRepository orderJpa, NotificationService notifications) {
        this.carJpa = carJpa;
        this.favoriteJpa = favoriteJpa;
        this.orderJpa = orderJpa;
        this.notifications = notifications;
    }

    public List<MerchantCarView> list(UserAccount user) {
        List<CarEntity> cars = user.isAdmin()
                ? carJpa.findAll(Sort.by(Sort.Direction.ASC, "id"))
                : carJpa.findByDealerIdOrderByIdAsc(user.id());
        return cars.stream().map(MerchantCarView::from).toList();
    }

    @Transactional
    public MerchantCarView create(UserAccount user, CarFormRequest form) {
        CarEntity entity = new CarEntity();
        entity.setDealerId(user.id());
        entity.setLengthMm(DEFAULT_LENGTH_MM);
        entity.setWidthMm(DEFAULT_WIDTH_MM);
        entity.setHeightMm(DEFAULT_HEIGHT_MM);
        entity.setWheelbaseMm(DEFAULT_WHEELBASE_MM);
        applyForm(entity, form);
        CarEntity saved = carJpa.save(entity);
        return MerchantCarView.from(saved);
    }

    @Transactional
    public MerchantCarView update(UserAccount user, Long id, CarFormRequest form) {
        CarEntity entity = requireOwned(user, id);
        applyForm(entity, form);
        entity.touch();
        return MerchantCarView.from(carJpa.save(entity));
    }

    /**
     * 上架 / 下架切换，避免删除历史数据时丢失订单关联。
     */
    @Transactional
    public MerchantCarView updateStatus(UserAccount user, Long id, String status) {
        CarEntity entity = requireOwned(user, id);
        entity.setStatus(normalizeStatus(status));
        entity.touch();
        return MerchantCarView.from(carJpa.save(entity));
    }

    @Transactional
    public void delete(UserAccount user, Long id) {
        CarEntity entity = requireOwned(user, id);
        favoriteJpa.deleteByCarId(id);
        carJpa.delete(entity);
    }

    public MerchantSummary summary(UserAccount user) {
        List<CarEntity> cars = user.isAdmin()
                ? carJpa.findAll()
                : carJpa.findByDealerIdOrderByIdAsc(user.id());
        long onSale = cars.stream().filter(CarEntity::isOnSale).count();
        int totalSales = cars.stream().mapToInt(CarEntity::getMonthlySales).sum();
        long pending = user.isAdmin()
                ? orderJpa.countByStatus("PENDING")
                : orderJpa.countByDealerIdAndStatus(user.id(), "PENDING");
        long total = user.isAdmin()
                ? orderJpa.count()
                : orderJpa.findByDealerIdOrderByIdDesc(user.id()).size();
        return new MerchantSummary(user.displayName(), onSale, cars.size() - onSale,
                pending, total, notifications.unreadCount(user.id()), totalSales);
    }

    private CarEntity requireOwned(UserAccount user, Long id) {
        CarEntity entity = carJpa.findById(id)
                .orElseThrow(() -> BizException.notFound("车源不存在：" + id));
        if (!user.isAdmin() && !user.id().equals(entity.getDealerId())) {
            throw new BizException(HttpStatus.FORBIDDEN, "只能管理自己名下的车源");
        }
        return entity;
    }

    private void applyForm(CarEntity entity, CarFormRequest form) {
        entity.setName(form.getName().trim());
        entity.setBrand(form.getBrand().trim());
        entity.setLevel(form.getLevel());
        entity.setBodyType(form.getBodyType());
        entity.setEnergyType(form.getEnergyType());
        entity.setPrice(form.getPrice());
        entity.setSeats(form.getSeats());
        entity.setMaxPowerKw(form.getMaxPowerKw());
        entity.setZeroToHundredSeconds(form.getZeroToHundredSeconds());
        entity.setMonthlySales(form.getMonthlySales());
        entity.setUserScore(form.getUserScore());
        entity.setFuelConsumption(form.getFuelConsumption());
        entity.setElectricRangeKm(form.getElectricRangeKm());
        entity.setHighlight(form.getHighlight() == null ? "" : form.getHighlight().trim());
        entity.setTags(String.join(",", form.getTags()));
        entity.setScenes(String.join(",", form.getScenes()));
        entity.setStatus(normalizeStatus(form.getStatus()));
    }

    private String normalizeStatus(String status) {
        return CarEntity.STATUS_OFF_SHELF.equalsIgnoreCase(status) ? CarEntity.STATUS_OFF_SHELF
                : CarEntity.STATUS_ON_SALE;
    }
}
