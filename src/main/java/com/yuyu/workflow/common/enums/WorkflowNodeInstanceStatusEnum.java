package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 节点实例状态枚举。
 */
public enum WorkflowNodeInstanceStatusEnum implements BaseEnum {

    PENDING(1, "PENDING", "待激活"),
    ACTIVE(2, "ACTIVE", "进行中"),
    PENDING_APPROVAL(3, "PENDING_APPROVAL", "待定/审批中"),
    APPROVED(4, "APPROVED", "已通过"),
    REJECTED(5, "REJECTED", "已拒绝"),
    SKIPPED(6, "SKIPPED", "已跳过"),
    CANCELED(7, "CANCELED", "已取消"),
    TIMEOUT(8, "TIMEOUT", "已超时");

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowNodeInstanceStatusEnum(Integer id, String code, String msg) {
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

    public static boolean isPending(String code) {
        return PENDING.getCode().equals(code);
    }

    public static boolean isActive(String code) {
        return ACTIVE.getCode().equals(code);
    }

    public static boolean isPendingApproval(String code) {
        return PENDING_APPROVAL.getCode().equals(code);
    }

    public static boolean isApproved(String code) {
        return APPROVED.getCode().equals(code);
    }



    public static boolean isRejected(String code) {
        return REJECTED.getCode().equals(code);
    }

    public static boolean isSkipped(String code) {
        return SKIPPED.getCode().equals(code);
    }

    public static boolean isCanceled(String code) {
        return CANCELED.getCode().equals(code);
    }

    public static boolean isTimeout(String code) {
        return TIMEOUT.getCode().equals(code);
    }

    public static boolean isRunningNodeInstance(String code) {
        return WorkflowNodeInstanceStatusEnum.PENDING.getCode().equals(code)
                || WorkflowNodeInstanceStatusEnum.ACTIVE.getCode().equals(code);
    }
}
