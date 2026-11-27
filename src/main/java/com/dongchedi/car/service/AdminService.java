package com.dongchedi.car.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.dongchedi.car.dto.AdminUserView;
import com.dongchedi.car.entity.UserEntity;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.model.UserRole;
import com.dongchedi.car.repository.jpa.UserJpaRepository;

/**
 * 管理端用户管理：启用 / 禁用账号、调整角色，并记录审计日志。
 */
@Service
public class AdminService {

    private final UserJpaRepository userJpa;
    private final FavoriteService favoriteService;
    private final AuditService auditService;
    private final RealtimeNotifier realtimeNotifier;

    public AdminService(UserJpaRepository userJpa, FavoriteService favoriteService,
                        AuditService auditService, RealtimeNotifier realtimeNotifier) {
        this.userJpa = userJpa;
        this.favoriteService = favoriteService;
        this.auditService = auditService;
        this.realtimeNotifier = realtimeNotifier;
    }

    public List<AdminUserView> users() {
        return userJpa.findAll().stream().map(this::view).toList();
    }

    @Transactional
    public AdminUserView setEnabled(UserAccount operator, Long targetId, boolean enabled) {
        UserEntity target = requireTarget(operator, targetId);
        target.setEnabled(enabled);
        userJpa.save(target);
        auditService.record(operator, enabled ? "启用账号" : "禁用账号", "USER", targetId,
                target.getUsername() + " → " + (enabled ? "启用" : "禁用"));
        if (!enabled) {
            disconnectAfterCommit(targetId);
        }
        return view(target);
    }

    @Transactional
    public AdminUserView setRole(UserAccount operator, Long targetId, String roleCode) {
        UserEntity target = requireTarget(operator, targetId);
        UserRole role;
        try {
            role = UserRole.valueOf(roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw BizException.badRequest("不支持的角色：" + roleCode);
        }
        String old = target.getRole();
        target.setRole(role.name());
        userJpa.save(target);
        auditService.record(operator, "调整角色", "USER", targetId,
                target.getUsername() + "：" + old + " → " + role.name());
        return view(target);
    }

    private UserEntity requireTarget(UserAccount operator, Long targetId) {
        if (operator.id().equals(targetId)) {
            throw BizException.badRequest("不能修改自己的账号状态或角色");
        }
        return userJpa.findById(targetId)
                .orElseThrow(() -> BizException.notFound("用户不存在：" + targetId));
    }

    private void disconnectAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    realtimeNotifier.disconnect(userId, "账号已被禁用，请联系管理员");
                }
            });
        } else {
            realtimeNotifier.disconnect(userId, "账号已被禁用，请联系管理员");
        }
    }

    private AdminUserView view(UserEntity entity) {
        UserAccount account = entity.toModel();
        return AdminUserView.from(account, favoriteService.count(account.id()));
    }
}
