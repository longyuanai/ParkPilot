package com.zhixingyuebo.enums;

/**
 * 充电桩故障代码枚举（对应 tb_pile_fault_log.error_code）
 */
public enum FaultErrorCode {
    E01("E01", "过温过载告警", "充电桩主板温控传感器触发过温熔断告警，设备自动停机保护"),
    E02("E02", "短路接地熔断", "充电主回路检测到相间短路或异常接地漏电，断路器紧急脱扣"),
    E03("E03", "通信网关中断", "充电桩主控单元与园区物联网中心心跳超时，进入离线保护"),
    E04("E04", "枪头电子锁死", "国标电子锁机械卡阻或微动开关故障，防止非正常带电插拔");

    private final String code;
    private final String name;
    private final String defaultDescription;

    FaultErrorCode(String code, String name, String defaultDescription) {
        this.code = code;
        this.name = name;
        this.defaultDescription = defaultDescription;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDefaultDescription() {
        return defaultDescription;
    }

    public static FaultErrorCode fromCode(String code) {
        if (code == null) return null;
        for (FaultErrorCode err : values()) {
            if (err.code.equalsIgnoreCase(code.trim())) {
                return err;
            }
        }
        return null;
    }
}
