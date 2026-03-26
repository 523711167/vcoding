package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 节点实例状态枚举。
 */
public enum WorkflowNodeInstanceStatusEnum implements BaseEnum {

    PENDING(1, "PENDING", "待激活"),
    ACTIVE(2, "ACTIVE", "进行中"),
    APPROVED(3, "APPROVED", "已通过"),
    REJECTED(4, "REJECTED", "已拒绝"),
    SKIPPED(5, "SKIPPED", "已跳过"),
    CANCELED(6, "CANCELED", "已取消"),
    TIMEOUT(7, "TIMEOUT", "已超时");

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
}
