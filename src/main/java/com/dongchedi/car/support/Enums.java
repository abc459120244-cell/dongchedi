package com.dongchedi.car.support;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 枚举解析工具：把前端传来的字符串宽松地解析为枚举，无法识别的值直接忽略，避免接口抛 400。
 */
public final class Enums {

    private static final Logger log = LoggerFactory.getLogger(Enums.class);

    private Enums() {
    }

    public static <E extends Enum<E>> Set<E> parseAll(Class<E> type, Collection<String> rawValues) {
        Set<E> result = new LinkedHashSet<>();
        if (rawValues == null) {
            return result;
        }
        for (String raw : rawValues) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            for (String candidate : raw.split(",")) {
                String value = candidate.trim();
                if (value.isEmpty()) {
                    continue;
                }
                try {
                    result.add(Enum.valueOf(type, value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    log.warn("忽略无法识别的 {} 取值: {}", type.getSimpleName(), value);
                }
            }
        }
        return result;
    }
}
