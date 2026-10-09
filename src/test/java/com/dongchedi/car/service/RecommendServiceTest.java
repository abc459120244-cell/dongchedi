package com.dongchedi.car.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.dongchedi.car.dto.RecommendItem;
import com.dongchedi.car.dto.RecommendRequest;
import com.dongchedi.car.dto.RecommendResult;
import com.dongchedi.car.model.ScoreDimension;
import com.dongchedi.car.repository.CarRepository;
import com.dongchedi.car.repository.InMemoryCarRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 推荐服务单元测试：校验候选过滤、加权排序与推荐理由的生成。
 */
class RecommendServiceTest {

    private RecommendService service;

    @BeforeEach
    void setUp() throws Exception {
        CarRepository repository = new InMemoryCarRepository(new DefaultResourceLoader(), new ObjectMapper(),
                "classpath:data/cars.json");
        service = new RecommendService(repository, 5, 20);
    }

    @Test
    @DisplayName("默认返回 5 款车型，得分从高到低排列")
    void shouldReturnLimitedItemsSortedByScore() {
        RecommendResult result = service.recommend(new RecommendRequest());

        assertThat(result.items()).hasSize(5);
        assertThat(result.items()).extracting(RecommendItem::rank).containsExactly(1, 2, 3, 4, 5);
        assertThat(result.items()).isSortedAccordingTo(
                (a, b) -> Double.compare(b.score(), a.score()));
        assertThat(result.candidateCount()).isPositive();
    }

    @Test
    @DisplayName("超出预算 15% 以上的车型不会进入推荐结果")
    void shouldExcludeCarsFarOverBudget() {
        RecommendRequest request = new RecommendRequest();
        request.setMinPrice(10.0);
        request.setMaxPrice(20.0);
        request.setLimit(20);

        RecommendResult result = service.recommend(request);

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).allSatisfy(item -> assertThat(item.car().price()).isLessThanOrEqualTo(20.0 * 1.15));
    }

    @Test
    @DisplayName("选择「只考虑新能源」时结果全部为可上绿牌的车型")
    void shouldRespectEnergyPreference() {
        RecommendRequest request = new RecommendRequest();
        request.setMinPrice(10.0);
        request.setMaxPrice(40.0);
        request.setEnergyPreference("NEW_ENERGY");
        request.setLimit(10);

        RecommendResult result = service.recommend(request);

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).allSatisfy(item -> assertThat(item.car().isNewEnergy()).isTrue());
    }

    @Test
    @DisplayName("动力性能优先 + 性能场景时，首位推荐为高功率性能车")
    void shouldPrioritizePowerWhenRequested() {
        RecommendRequest request = new RecommendRequest();
        request.setMinPrice(30.0);
        request.setMaxPrice(45.0);
        request.setScenes(List.of("PERFORMANCE"));
        request.setPriority("PERFORMANCE");
        request.setEnergyPreference("ANY");

        RecommendResult result = service.recommend(request);

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items().get(0).car().maxPowerKw()).isGreaterThanOrEqualTo(300);
        assertThat(result.items().get(0).details())
                .extracting(detail -> detail.dimension())
                .contains(ScoreDimension.POWER.name(), ScoreDimension.SCENE.name());
    }

    @Test
    @DisplayName("空间优先 + 至少 7 座时，首位推荐为 7 座车型")
    void shouldPrioritizeSpaceWhenRequested() {
        RecommendRequest request = new RecommendRequest();
        request.setMinPrice(20.0);
        request.setMaxPrice(45.0);
        request.setMinSeats(7);
        request.setPriority("SPACE");

        RecommendResult result = service.recommend(request);

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items().get(0).car().seats()).isEqualTo(7);
    }

    @Test
    @DisplayName("口碑优先时高分车型排在前面")
    void shouldPrioritizeReputationWhenRequested() {
        RecommendRequest request = new RecommendRequest();
        request.setMinPrice(20.0);
        request.setMaxPrice(45.0);
        request.setPriority("REPUTATION");

        RecommendResult result = service.recommend(request);

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items().get(0).car().userScore()).isGreaterThanOrEqualTo(4.4);
    }

    @Test
    @DisplayName("每条推荐都带有推荐理由与得分明细")
    void shouldGenerateExplanations() {
        RecommendRequest request = new RecommendRequest();
        request.setScenes(List.of("FAMILY_TRIP"));

        RecommendResult result = service.recommend(request);

        assertThat(result.items()).allSatisfy(item -> {
            assertThat(item.reasons()).hasSizeGreaterThanOrEqualTo(4);
            assertThat(item.details()).isNotEmpty();
            assertThat(item.score()).isBetween(0d, 100d);
        });
    }

    @Test
    @DisplayName("推荐数量受配置上限约束")
    void shouldCapLimitByConfiguration() {
        RecommendRequest request = new RecommendRequest();
        request.setLimit(20);
        request.setMaxPrice(60.0);

        RecommendResult result = service.recommend(request);

        assertThat(result.items()).hasSizeLessThanOrEqualTo(20);
    }
}
