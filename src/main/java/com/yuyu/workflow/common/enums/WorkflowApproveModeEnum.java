package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 流程审批模式枚举。
 */
public enum WorkflowApproveModeEnum implements BaseEnum {

    AND(1, "AND", "会签"),
    OR(2, "OR", "或签"),
    SEQUENTIAL(3, "SEQUENTIAL", "顺签");

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowApproveModeEnum(Integer id, String code, String msg) {
        this.id = id;
        this.code = code;
        this.msg = msg;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getName() {
        return msg;
    }

    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }

    public static boolean containsCode(String code) {
        return Arrays.stream(values()).anyMatch(item -> Objects.equals(item.getCode(), code));
    }

    public static boolean isAnd(String code) {
        return AND.code.equals(code);
    }

    public static boolean isOr(String code) {
        return OR.code.equals(code);
    }

    public static boolean isSequential(String code) {
        return SEQUENTIAL.code.equals(code);
    }
}
