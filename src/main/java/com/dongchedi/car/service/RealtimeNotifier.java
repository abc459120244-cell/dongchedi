package com.dongchedi.car.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 基于 SSE（Server-Sent Events）的实时消息推送。
 *
 * <p>买家下单后立即推送给对应商家，商家处理订单后推送给买家，
 * 实现「多端互动」；同时消息也会落库，页面刷新后依然能看到未读消息。
 */
@Component
public class RealtimeNotifier {

    private static final Logger log = LoggerFactory.getLogger(RealtimeNotifier.class);

    /** SSE 连接超时时间：30 分钟 */
    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("订阅成功"));
        } catch (IOException ex) {
            remove(userId, emitter);
        }
        log.debug("用户 {} 建立实时消息连接，当前连接数 {}", userId, emitters.get(userId).size());
        return emitter;
    }

    public void push(Long userId, String event, Object payload) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(event).data(payload));
            } catch (Exception ex) {
                remove(userId, emitter);
            }
        }
    }

    /**
     * 账号被禁用时通知所有端并主动结束连接，避免已打开的 SSE 继续接收消息。
     */
    public void disconnect(Long userId, String reason) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> list = emitters.remove(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("account-disabled")
                        .data(reason == null || reason.isBlank() ? "账号已被禁用，请联系管理员" : reason));
            } catch (Exception ignored) {
                /* 连接可能已经断开，下面统一结束即可 */
            } finally {
                emitter.complete();
            }
        }
        log.info("用户 {} 的 {} 个实时连接已断开", userId, list.size());
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    public int connectionCount(Long userId) {
        List<SseEmitter> list = emitters.get(userId);
        return list == null ? 0 : list.size();
    }
}
