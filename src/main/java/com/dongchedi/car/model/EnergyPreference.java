package com.dongchedi.car.model;

import java.util.Locale;

/**
 * 能源偏好。
 */
public enum EnergyPreference {

    ANY("不限能源形式"),
    NEW_ENERGY("只考虑新能源"),
    TRADITIONAL("只考虑燃油/油混");

    private final String label;

    EnergyPreference(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static EnergyPreference of(String code) {
        if (code == null || code.isBlank()) {
            return ANY;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (EnergyPreference preference : values()) {
            if (preference.name().equals(normalized)) {
                return preference;
            }
        }
        return ANY;
    }
}
