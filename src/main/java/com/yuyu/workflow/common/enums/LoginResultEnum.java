package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 登录结果枚举。
 */
public enum LoginResultEnum implements BaseEnum {

    SUCCESS(1, "SUCCESS", "登录成功"),
    FAIL(2, "FAIL", "登录失败");

    private final Integer id;
    private final String code;
    private final String msg;

    LoginResultEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取登录结果主键。
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * 获取登录结果编码。
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取登录结果说明。
     */
    @Override
    public String getName() {
        return msg;
    }

    /**
     * 根据 code 判断登录结果是否存在。
     */
    public static boolean containsCode(String code) {
        return Arrays.stream(values()).anyMatch(item -> Objects.equals(item.getCode(), code));
    }

    /**
     * 根据 code 获取登录结果说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }
}
