package com.dongchedi.car.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.dongchedi.car.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 在拦截器中直接写出与全局异常处理一致的错误响应。
 */
public final class ErrorResponses {

    private ErrorResponses() {
    }

    public static void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                             HttpStatus status, String message) throws IOException {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message, List.of(),
                request.getRequestURI(), LocalDateTime.now());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
