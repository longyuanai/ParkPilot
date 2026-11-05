package com.zhixingyuebo.enums;

/**
 * 用户角色类型枚举（对应 tb_user.user_type）
 */
public enum UserRole {
    OWNER(1, "园区车主/业主"),
    TECHNICIAN(2, "充电桩运维技师"),
    ADMIN(3, "物业综合管理员");

    private final int code;
    private final String description;

    UserRole(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserRole fromCode(Integer code) {
        if (code == null) return null;
        for (UserRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return null;
    }
}
