package com.zhixingyuebo.enums;

/**
 * 结算状态枚举（对应 tb_parking_record.pay_status）
 */
public enum PayStatus {
    UNPAID(0, "未结算/在场"),
    MONTHLY_CARD_FREE(1, "月卡全免"),
    WALLET_PAID(2, "钱包成功扣款"),
    OVERDUE_PAID(3, "已补缴出场");

    private final int code;
    private final String description;

    PayStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PayStatus fromCode(Integer code) {
        if (code == null) return null;
        for (PayStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
