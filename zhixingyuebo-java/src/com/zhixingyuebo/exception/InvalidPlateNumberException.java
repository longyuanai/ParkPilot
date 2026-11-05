package com.zhixingyuebo.exception;

/**
 * 车牌号码格式校验不合规异常
 */
public class InvalidPlateNumberException extends ZhiXingYueBoException {
    private static final long serialVersionUID = 1L;

    private final String plateNumber;

    public InvalidPlateNumberException(String plateNumber, String message) {
        super(String.format("车牌号码 [%s] 不合规: %s", plateNumber, message));
        this.plateNumber = plateNumber;
    }

    public String getPlateNumber() {
        return plateNumber;
    }
}
