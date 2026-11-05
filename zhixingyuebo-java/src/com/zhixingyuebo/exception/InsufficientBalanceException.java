package com.zhixingyuebo.exception;

import java.math.BigDecimal;

/**
 * 钱包可用余额不足受检异常（阻断抬杆/拔枪结算，触发事务回滚）
 */
public class InsufficientBalanceException extends ZhiXingYueBoException {
    private static final long serialVersionUID = 1L;

    private final BigDecimal currentBalance;
    private final BigDecimal requiredAmount;

    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requiredAmount) {
        super(String.format("钱包可用余额不足！当前可用余额: ¥%s，应付金额: ¥%s，尚差: ¥%s",
                currentBalance.setScale(2).toPlainString(),
                requiredAmount.setScale(2).toPlainString(),
                requiredAmount.subtract(currentBalance).setScale(2).toPlainString()));
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }
}
