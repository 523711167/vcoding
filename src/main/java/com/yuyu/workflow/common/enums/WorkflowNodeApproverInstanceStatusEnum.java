package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 节点审批人实例状态枚举。
 */
public enum WorkflowNodeApproverInstanceStatusEnum implements BaseEnum {

    PENDING(1, "PENDING", "待处理"),
    WAITING_ADD_SIGN(2, "WAITING_ADD_SIGN", "等待加签"),
    APPROVED(3, "APPROVED", "已通过"),
    REJECTED(4, "REJECTED", "已拒绝"),
    DELEGATED(5, "DELEGATED", "已转交"),
    CANCELED(6, "CANCELED", "已取消");

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowNodeApproverInstanceStatusEnum(Integer id, String code, String msg) {
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
