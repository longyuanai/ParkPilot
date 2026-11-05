package com.zhixingyuebo.exception;

/**
 * 充电桩硬件故障/离线不可用异常
 */
public class PileFaultException extends ZhiXingYueBoException {
    private static final long serialVersionUID = 1L;

    private final String pileCode;
    private final int pileStatus;

    public PileFaultException(String pileCode, int pileStatus, String message) {
        super(String.format("充电桩 [%s] 无法启用（状态码: %d）: %s", pileCode, pileStatus, message));
        this.pileCode = pileCode;
        this.pileStatus = pileStatus;
    }

    public String getPileCode() {
        return pileCode;
    }

    public int getPileStatus() {
        return pileStatus;
    }
}
