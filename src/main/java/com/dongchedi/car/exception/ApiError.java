package com.dongchedi.car.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一错误响应。
 */
public record ApiError(
        int status,
        String error,
        String message,
        List<String> details,
        String path,
        LocalDateTime timestamp) {
}
