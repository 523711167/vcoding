package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

public enum OrgTypeEnum implements BaseEnum {

    GROUP(1, "GROUP", "集团"),
    COMPANY(2, "COMPANY", "公司"),
    DEPT(3, "DEPT", "部门"),
    POST(4, "POST", "岗位");

    /**
     * 组织类型正则表达式，供注解校验复用。
     */
    public static final String REGEXP = "^(GROUP|COMPANY|DEPT|POST)$";

    private final Integer id;
    private final String code;
    private final String msg;

    OrgTypeEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取枚举主键。
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * 获取枚举编码。
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取枚举说明。
     */
    @Override
    public String getName() {
        return msg;
    }

    /**
     * 根据编码获取说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }

    /**
     * 判断编码是否存在。
     */
    public static boolean containsCode(String code) {
        return Arrays.stream(values()).anyMatch(item -> Objects.equals(item.getCode(), code));
    }
}
