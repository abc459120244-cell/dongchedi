package com.dongchedi.car;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 汽车筛选与推荐系统启动类。
 *
 * <p>启动后访问 <a href="http://localhost:8080">http://localhost:8080</a> 即可打开前端页面。
 */
@SpringBootApplication
public class DongchediApplication {

    public static void main(String[] args) {
        SpringApplication.run(DongchediApplication.class, args);
    }
}
