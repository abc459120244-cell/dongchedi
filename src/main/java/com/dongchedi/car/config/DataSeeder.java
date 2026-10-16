package com.dongchedi.car.config;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dongchedi.car.entity.CarEntity;
import com.dongchedi.car.model.Car;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.FavoriteRepository;
import com.dongchedi.car.repository.UserRepository;
import com.dongchedi.car.repository.jpa.CarJpaRepository;
import com.dongchedi.car.repository.jpa.UserJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 启动时初始化数据库：账号、车型种子数据、演示收藏。
 *
 * <p>各项都是「幂等」的：数据库已有数据时直接跳过，不会重复插入。
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** 商家账号 → 负责的品牌（车源自动归属，形成「多商家」结构） */
    private static final Map<String, List<String>> DEALER_BRANDS = new LinkedHashMap<>();

    static {
        DEALER_BRANDS.put("dealer_byd", List.of("比亚迪", "仰望", "方程豹", "腾势"));
        DEALER_BRANDS.put("dealer_ev", List.of("特斯拉", "蔚来", "小鹏", "理想", "小米", "极氪", "问界", "零跑", "智己"));
        DEALER_BRANDS.put("dealer_intl", List.of("大众", "丰田", "本田", "日产", "宝马", "奔驰", "奥迪",
                "保时捷", "沃尔沃", "福特", "现代", "长城", "五菱", "吉利", "长安", "奇瑞", "哈弗", "领克", "别克", "坦克"));
    }

    private final AuthProperties properties;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final CarJpaRepository carJpa;
    private final UserJpaRepository userJpa;
    private final PasswordEncoder passwordEncoder;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final String datasetLocation;

    public DataSeeder(AuthProperties properties, UserRepository userRepository,
                      FavoriteRepository favoriteRepository, CarJpaRepository carJpa,
                      UserJpaRepository userJpa, PasswordEncoder passwordEncoder,
                      ResourceLoader resourceLoader, ObjectMapper objectMapper,
                      @Value("${dongchedi.dataset.location:classpath:data/cars.json}") String datasetLocation) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.carJpa = carJpa;
        this.userJpa = userJpa;
        this.passwordEncoder = passwordEncoder;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.datasetLocation = datasetLocation;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedUsers();
        seedCars();
        seedFavorites();
    }

    private void seedUsers() {
        int created = 0;
        for (AuthProperties.SeedUser seed : properties.getSeedUsers()) {
            if (seed.getUsername() == null || seed.getUsername().isBlank()
                    || seed.getPassword() == null || seed.getPassword().isBlank()) {
                log.warn("跳过不合法的演示账号配置: {}", seed.getUsername());
                continue;
            }
            if (userRepository.existsByUsername(seed.getUsername())) {
                continue;
            }
            userRepository.create(seed.getUsername(), seed.getNickname(), seed.getRole(),
                    passwordEncoder.encode(seed.getPassword()));
            created++;
        }
        if (created > 0) {
            log.info("已初始化 {} 个演示账号（普通用户 / 商家 / 管理员，账号见 README）", created);
        }
    }

    private void seedCars() throws Exception {
        if (carJpa.count() > 0) {
            log.info("车型表已有 {} 条数据，跳过车型初始化", carJpa.count());
            return;
        }
        Resource resource = resourceLoader.getResource(datasetLocation);
        List<Car> cars;
        try (InputStream in = resource.getInputStream()) {
            cars = objectMapper.readValue(in, new TypeReference<List<Car>>() {
            });
        }

        Map<String, Long> dealerIdByBrand = resolveDealerIds();
        for (Car car : cars) {
            CarEntity entity = CarEntity.fromModel(car);
            entity.setId(null);
            entity.setStatus(CarEntity.STATUS_ON_SALE);
            entity.setDealerId(dealerIdByBrand.get(car.brand()));
            carJpa.save(entity);
        }
        log.info("已从 {} 导入 {} 款车型到数据库，其中 {} 款已归属商家车源",
                datasetLocation, cars.size(),
                cars.stream().filter(car -> dealerIdByBrand.containsKey(car.brand())).count());
    }

    private Map<String, Long> resolveDealerIds() {
        Map<String, Long> mapping = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : DEALER_BRANDS.entrySet()) {
            userRepository.findByUsername(entry.getKey()).map(UserAccount::id).ifPresent(dealerId -> {
                for (String brand : entry.getValue()) {
                    mapping.put(brand, dealerId);
                }
            });
        }
        return mapping;
    }

    private void seedFavorites() {
        for (AuthProperties.SeedUser seed : properties.getSeedUsers()) {
            if (seed.getFavorites() == null || seed.getFavorites().isEmpty()) {
                continue;
            }
            userRepository.findByUsername(seed.getUsername()).ifPresent(user -> {
                for (Long carId : seed.getFavorites()) {
                    if (carJpa.existsById(carId)) {
                        favoriteRepository.add(user.id(), carId);
                    }
                }
            });
        }
    }

    /**
     * 供测试与其他组件查询商家数量使用。
     */
    public long merchantCount() {
        return userJpa.countByRole("MERCHANT");
    }
}
