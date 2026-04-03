package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 流程实例状态枚举。
 */
public enum WorkflowInstanceStatusEnum implements BaseEnum {

    RUNNING(1, "RUNNING", "进行中"),
    APPROVED(2, "APPROVED", "已通过"),
    REJECTED(3, "REJECTED", "已拒绝"),
    CANCELED(4, "CANCELED", "已撤回"),
    FINISHI(5, "FINISHI", "已完成"),
    INITIATOR_CANCELED(6, "INITIATOR_CANCELED", "已取消")
    ;

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowInstanceStatusEnum(Integer id, String code, String msg) {
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

    public static boolean isRunning(String code) {
        return RUNNING.getCode().equals(code);
    }

    public static boolean isApproved(String code) {
        return APPROVED.getCode().equals(code);
    }

    public static boolean isRejected(String code) {
        return REJECTED.getCode().equals(code);
    }

    public static boolean isCanceled(String code) {
        return CANCELED.getCode().equals(code);
    }

    public static boolean isInitiatorCanceled(String code) {
        return INITIATOR_CANCELED.getCode().equals(code);
    }

    public static boolean isFinished(String code) {
        return FINISHI.getCode().equals(code);
    }

}
