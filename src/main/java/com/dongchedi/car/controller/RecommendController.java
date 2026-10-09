package com.dongchedi.car.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dongchedi.car.dto.RecommendRequest;
import com.dongchedi.car.dto.RecommendResult;
import com.dongchedi.car.model.EnergyPreference;
import com.dongchedi.car.model.RecommendationPriority;
import com.dongchedi.car.model.UseScene;
import com.dongchedi.car.service.RecommendService;

import jakarta.validation.Valid;

/**
 * 智能推荐接口。
 */
@RestController
@RequestMapping("/api/recommend")
@Validated
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    /**
     * 按预算、场景、能源偏好、优先级返回推荐车型。
     */
    @PostMapping
    public RecommendResult recommend(@Valid @RequestBody RecommendRequest request) {
        return recommendService.recommend(request);
    }

    /**
     * 快速推荐：使用查询参数直接调用，便于页面首屏与自动跳转。
     */
    @GetMapping
    public RecommendResult quick(@RequestParam(required = false) Double minPrice,
                                 @RequestParam(required = false) Double maxPrice,
                                 @RequestParam(required = false) List<String> scenes,
                                 @RequestParam(required = false) String energyPreference,
                                 @RequestParam(required = false) Integer minSeats,
                                 @RequestParam(required = false) String priority,
                                 @RequestParam(required = false) Integer limit) {
        RecommendRequest request = new RecommendRequest();
        if (minPrice != null) {
            request.setMinPrice(minPrice);
        }
        if (maxPrice != null) {
            request.setMaxPrice(maxPrice);
        }
        if (scenes != null) {
            request.setScenes(scenes);
        }
        if (energyPreference != null) {
            request.setEnergyPreference(energyPreference);
        }
        if (minSeats != null) {
            request.setMinSeats(minSeats);
        }
        if (priority != null) {
            request.setPriority(priority);
        }
        if (limit != null) {
            request.setLimit(limit);
        }
        return recommendService.recommend(request);
    }

    /**
     * 推荐表单的可选项与常用预算档位。
     */
    @GetMapping("/options")
    public Map<String, Object> options() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("scenes", toOptions(UseScene.values(), UseScene::name, UseScene::label));
        options.put("priorities", toOptions(RecommendationPriority.values(), RecommendationPriority::name,
                RecommendationPriority::label));
        options.put("energyPreferences", toOptions(EnergyPreference.values(), EnergyPreference::name,
                EnergyPreference::label));
        options.put("budgetPresets", List.of(
                Map.of("label", "10 万以内", "minPrice", 0, "maxPrice", 10),
                Map.of("label", "10~15 万", "minPrice", 10, "maxPrice", 15),
                Map.of("label", "15~20 万", "minPrice", 15, "maxPrice", 20),
                Map.of("label", "20~30 万", "minPrice", 20, "maxPrice", 30),
                Map.of("label", "30 万以上", "minPrice", 30, "maxPrice", 60)));
        return options;
    }

    private static <E> List<Map<String, Object>> toOptions(E[] values, Function<E, String> code,
                                                           Function<E, String> label) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (E value : values) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", code.apply(value));
            item.put("label", label.apply(value));
            result.add(item);
        }
        return result;
    }
}
