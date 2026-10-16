package com.dongchedi.car.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dongchedi.car.dto.NotificationView;
import com.dongchedi.car.entity.NotificationEntity;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.repository.jpa.NotificationJpaRepository;

/**
 * 站内消息服务：下单 / 处理订单等动作会产生消息，落库的同时通过 SSE 实时推送。
 */
@Service
public class NotificationService {

    private final NotificationJpaRepository jpa;
    private final RealtimeNotifier notifier;

    public NotificationService(NotificationJpaRepository jpa, RealtimeNotifier notifier) {
        this.jpa = jpa;
        this.notifier = notifier;
    }

    @Transactional
    public NotificationView notify(Long userId, String type, String title, String content, Long refId) {
        if (userId == null) {
            return null;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(userId);
        entity.setType(type);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setRefId(refId);
        entity.setReadFlag(false);
        NotificationEntity saved = jpa.save(entity);
        NotificationView view = NotificationView.from(saved);
        notifier.push(userId, "notification", view);
        return view;
    }

    public List<NotificationView> list(Long userId) {
        return jpa.findByUserIdOrderByIdDesc(userId).stream().map(NotificationView::from).toList();
    }

    public long unreadCount(Long userId) {
        return jpa.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        NotificationEntity entity = jpa.findById(id)
                .orElseThrow(() -> BizException.notFound("消息不存在: " + id));
        if (!entity.getUserId().equals(userId)) {
            throw BizException.notFound("消息不存在: " + id);
        }
        entity.setReadFlag(true);
        jpa.save(entity);
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<NotificationEntity> list = jpa.findByUserIdOrderByIdDesc(userId);
        list.forEach(item -> item.setReadFlag(true));
        jpa.saveAll(list);
    }
}
