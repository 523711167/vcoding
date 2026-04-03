package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 审批操作记录动作枚举。
 */
public enum WorkflowApprovalActionEnum implements BaseEnum {

    SUBMIT(1, "SUBMIT", "提交申请"),
    APPROVE(2, "APPROVE", "审批通过"),
    REJECT(3, "REJECT", "审批拒绝"),
    DELEGATE(4, "DELEGATE", "审批转交"),
    RECALL(5, "RECALL", "发起人撤回"),
    ADD_SIGN(6, "ADD_SIGN", "发起加签"),
    ROUTE(7, "ROUTE", "系统自动路由"),
    SPLIT_TRIGGER(8, "SPLIT_TRIGGER", "系统触发并行拆分"),
    JOIN_ARRIVE(9, "JOIN_ARRIVE", "分支到达并行聚合节点"),
    JOIN_PASS(10, "JOIN_PASS", "并行聚合完成并继续流转"),
    AUTO_APPROVE(11, "AUTO_APPROVE", "系统自动审核通过"),
    AUTO_REJECT(12, "AUTO_REJECT", "系统自动审批拒绝"),
    TIMEOUT(13, "TIMEOUT", "节点超时自动处理触发记录"),
    REMIND(14, "REMIND", "节点超时后发送提醒"),
    CANCEL(15, "CANCEL", "发起人取消");

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowApprovalActionEnum(Integer id, String code, String msg) {
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
