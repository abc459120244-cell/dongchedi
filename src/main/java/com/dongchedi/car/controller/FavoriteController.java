package com.dongchedi.car.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.FavoriteState;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.service.FavoriteService;
import com.dongchedi.car.support.SessionKeys;

/**
 * 用户收藏接口，全部需要登录（由 LoginRequiredInterceptor 统一拦截）。
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * 我的收藏（返回完整车型信息，按收藏顺序）。
     */
    @GetMapping
    public List<Car> list(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        return favoriteService.list(user.id());
    }

    /**
     * 我的收藏 id 列表，前端用于点亮收藏按钮。
     */
    @GetMapping("/ids")
    public List<Long> ids(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        return favoriteService.ids(user.id());
    }

    @PostMapping("/{carId}")
    public FavoriteState add(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                             @PathVariable Long carId) {
        return favoriteService.add(user.id(), carId);
    }

    @DeleteMapping("/{carId}")
    public FavoriteState remove(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                                @PathVariable Long carId) {
        return favoriteService.remove(user.id(), carId);
    }

    @DeleteMapping
    public FavoriteState clear(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        return favoriteService.clear(user.id());
    }
}
