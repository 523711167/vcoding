package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 角色编码枚举。
 */
public enum RoleCodeEnum implements BaseEnum {

    ADMIN(1, "ADMIN", "系统管理员");

    private final Integer id;
    private final String code;
    private final String msg;

    RoleCodeEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取角色编码主键。
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * 获取角色编码。
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取角色编码说明。
     */
    @Override
    public String getMsg() {
        return msg;
    }

    /**
     * 根据 id 获取角色编码说明。
     */
    public static String getMsgById(Integer id) {
        return EnumUtils.getMsgById(values(), id);
    }

    /**
     * 根据 code 获取角色编码说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }

    /**
     * 判断角色编码是否存在。
     */
    public static boolean containsCode(String code) {
        return Arrays.stream(values()).anyMatch(item -> Objects.equals(item.getCode(), code));
    }
}
