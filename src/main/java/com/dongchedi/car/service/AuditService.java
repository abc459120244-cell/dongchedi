package com.dongchedi.car.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dongchedi.car.dto.OperationLogView;
import com.dongchedi.car.entity.OperationLogEntity;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.jpa.OperationLogJpaRepository;

/**
 * 操作审计：把商家的写操作记录到数据库，便于追溯「谁在什么时候改了什么」。
 */
@Service
public class AuditService {

    private final OperationLogJpaRepository jpa;

    public AuditService(OperationLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Transactional
    public void record(UserAccount operator, String action, String targetType, Long targetId, String detail) {
        if (operator == null) {
            return;
        }
        OperationLogEntity entity = new OperationLogEntity();
        entity.setOperatorId(operator.id());
        entity.setOperatorName(operator.displayName());
        entity.setOperatorRole(operator.role().name());
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setDetail(detail == null ? "" : detail.length() > 300 ? detail.substring(0, 300) : detail);
        jpa.save(entity);
    }

    public List<OperationLogView> list(UserAccount user, int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        PageRequest page = PageRequest.of(0, size);
        List<OperationLogEntity> logs = user.isAdmin()
                ? jpa.findAllByOrderByIdDesc(page)
                : jpa.findByOperatorIdOrderByIdDesc(user.id(), page);
        return logs.stream().map(OperationLogView::from).toList();
    }
}
