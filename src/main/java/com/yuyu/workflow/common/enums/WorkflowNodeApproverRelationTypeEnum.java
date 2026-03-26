package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 节点审批人实例来源关系类型枚举。
 */
public enum WorkflowNodeApproverRelationTypeEnum implements BaseEnum {

    ORIGINAL(1, "ORIGINAL", "原始审批人"),
    ADD_SIGN(2, "ADD_SIGN", "加签审批人");

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowNodeApproverRelationTypeEnum(Integer id, String code, String msg) {
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
}
