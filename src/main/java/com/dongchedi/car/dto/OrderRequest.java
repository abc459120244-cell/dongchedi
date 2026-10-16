package com.dongchedi.car.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 买家提交订单（预约试驾 / 询价 / 意向定金）。
 */
public class OrderRequest {

    @NotNull(message = "请选择车型")
    private Long carId;

    /** TEST_DRIVE / INQUIRY / DEPOSIT */
    private String type = "TEST_DRIVE";

    @NotBlank(message = "请填写联系人")
    @Size(max = 20, message = "联系人姓名过长")
    private String contactName;

    @NotBlank(message = "请填写联系电话")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请填写正确的 11 位手机号")
    private String contactPhone;

    @Size(max = 200, message = "备注不能超过 200 字")
    private String remark;

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
