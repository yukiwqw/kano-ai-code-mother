package com.yupi.yuaicodemother.model.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    /**
     * 普通用户
     */
    USER("用户", "user"),
    /**
     * 管理员
     */
    ADMIN("管理员", "admin");

    private final String desc;
    private final String value;

    UserRoleEnum(String desc, String value) {
        this.desc = desc;
        this.value = value;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        for (UserRoleEnum roleEnum : UserRoleEnum.values()) {
            if (roleEnum.getValue().equals(value)) {
                return roleEnum;
            }
        }
        return null;
    }
}
