package com.dongchedi.car.model;

/**
 * 能源类型。
 */
public enum EnergyType {

    GASOLINE("汽油", false),
    DIESEL("柴油", false),
    HEV("油电混合", false),
    PHEV("插电式混合动力", true),
    EREV("增程式电动", true),
    BEV("纯电动", true);

    private final String label;
    private final boolean electric;

    EnergyType(String label, boolean electric) {
        this.label = label;
        this.electric = electric;
    }

    public String label() {
        return label;
    }

    /**
     * 是否可以外部充电（挂绿牌的新能源车）。
     */
    public boolean isElectric() {
        return electric;
    }
}
