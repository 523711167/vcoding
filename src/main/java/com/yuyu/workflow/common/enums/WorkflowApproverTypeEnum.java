package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 节点审批人类型枚举。
 */
public enum WorkflowApproverTypeEnum implements BaseEnum {

    USER(1, "USER", "指定用户"),
    ROLE(2, "ROLE", "指定角色"),
    DEPT(3, "DEPT", "指定组织"),
    INITIATOR_DEPT_LEADER(4, "INITIATOR_DEPT_LEADER", "发起人主组织主管");

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowApproverTypeEnum(Integer id, String code, String msg) {
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
    public String getMsg() {
        return msg;
    }

    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }

    public static boolean containsCode(String code) {
        return Arrays.stream(values()).anyMatch(item -> Objects.equals(item.getCode(), code));
    }
}
