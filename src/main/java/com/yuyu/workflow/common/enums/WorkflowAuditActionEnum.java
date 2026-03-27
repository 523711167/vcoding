package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 流程审核动作枚举。
 */
public enum WorkflowAuditActionEnum implements BaseEnum {

    APPROVE(1, "APPROVE", "审核通过"),
    REJECT(2, "REJECT", "审核拒绝");

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowAuditActionEnum(Integer id, String code, String msg) {
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

    public static boolean isApprove(String code) {
        return APPROVE.getCode().equals(code);
    }

    public static boolean isReject(String code) {
        return REJECT.getCode().equals(code);
    }
}
