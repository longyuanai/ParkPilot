package com.zhixingyuebo.exception;

/**
 * 车位已被预约/已被占用/维护禁用冲突异常
 */
public class SpotNotAvailableException extends ZhiXingYueBoException {
    private static final long serialVersionUID = 1L;

    private final String spotNumber;
    private final int currentStatus;

    public SpotNotAvailableException(String spotNumber, int currentStatus, String message) {
        super(String.format("车位 [%s] 当前不可用（状态码: %d）: %s", spotNumber, currentStatus, message));
        this.spotNumber = spotNumber;
        this.currentStatus = currentStatus;
    }

    public String getSpotNumber() {
        return spotNumber;
    }

    public int getCurrentStatus() {
        return currentStatus;
    }
}
