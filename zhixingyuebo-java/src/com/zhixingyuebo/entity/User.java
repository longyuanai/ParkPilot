/*
 * Copyright (c) 2026 Jiawei Huang (黄家威). All Rights Reserved.
 *
 * Project: ParkPilot (智泊领航) - Smart Parking & EV Charging Core Engine
 * Licensed under the PolyForm Noncommercial License 1.0.0 (the "License").
 * You may not use this file except in compliance with the License.
 *
 * Commercial Use strictly requires a separate commercial license from the author.
 * For commercial licensing, please refer to COMMERCIAL.md or contact: 1507205727@qq.com
 */
package com.zhixingyuebo.entity;

import com.zhixingyuebo.enums.UserRole;

import java.time.LocalDateTime;

/**
 * 抽象用户基类（面向对象继承与多态设计核心）
 * 派生 OwnerDriver（车主）、Technician（技师）、Admin（管理员）
 */
public abstract class User {

    private Integer userId;
    private String username;
    private String phone;
    private Integer userType; // 1普通车主/业主, 2运维技师, 3物业管理员
    private LocalDateTime createdAt;
    private Integer isDeleted;

    public User() {
    }

    public User(Integer userId, String username, String phone, Integer userType, LocalDateTime createdAt, Integer isDeleted) {
        this.userId = userId;
        this.username = username;
        this.phone = phone;
        this.userType = userType;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
    }

    /**
     * 抽象方法：多态展示不同角色的专属控制台菜单
     */
    public abstract void showMenu();

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getUserType() {
        return userType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    public UserRole getUserRoleEnum() {
        return UserRole.fromCode(userType);
    }

    public void setUserRoleEnum(UserRole role) {
        this.userType = role != null ? role.getCode() : null;
    }

    public String getUserRoleName() {
        UserRole role = getUserRoleEnum();
        return role != null ? role.getDescription() : "普通访客";
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", phone='" + phone + '\'' +
                ", userType=" + userType +
                ", createdAt=" + createdAt +
                ", isDeleted=" + isDeleted +
                '}';
    }
}
