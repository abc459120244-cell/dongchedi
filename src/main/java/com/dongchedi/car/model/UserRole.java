package com.dongchedi.car.model;

/**
 * 用户角色。
 */
public enum UserRole {

    USER("普通用户"),
    MERCHANT("商家"),
    ADMIN("管理员");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
