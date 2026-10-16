package com.dongchedi.car.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.dongchedi.car.dto.NotificationView;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.service.NotificationService;
import com.dongchedi.car.service.RealtimeNotifier;
import com.dongchedi.car.support.SessionKeys;

/**
 * 站内消息接口：拉取历史消息 + SSE 实时推送。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final RealtimeNotifier notifier;

    public NotificationController(NotificationService notificationService, RealtimeNotifier notifier) {
        this.notificationService = notificationService;
        this.notifier = notifier;
    }

    @GetMapping
    public List<NotificationView> list(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        return notificationService.list(user.id());
    }

    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        return Map.of("unread", notificationService.unreadCount(user.id()));
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> markRead(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user,
                                        @PathVariable Long id) {
        notificationService.markRead(user.id(), id);
        return Map.of("unread", notificationService.unreadCount(user.id()));
    }

    @PostMapping("/read-all")
    public Map<String, Object> markAllRead(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        notificationService.markAllRead(user.id());
        return Map.of("unread", 0);
    }

    /**
     * SSE 实时消息通道：买家下单会推给商家，商家处理会推给买家。
     */
    @GetMapping("/stream")
    public SseEmitter stream(@RequestAttribute(SessionKeys.CURRENT_USER_ATTRIBUTE) UserAccount user) {
        return notifier.subscribe(user.id());
    }
}
