package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 节点超时处理策略枚举。
 */
public enum WorkflowTimeoutActionEnum implements BaseEnum {

    AUTO_APPROVE(1, "AUTO_APPROVE", "自动通过"),
    AUTO_REJECT(2, "AUTO_REJECT", "自动拒绝"),
    NOTIFY_ONLY(3, "NOTIFY_ONLY", "仅提醒");

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowTimeoutActionEnum(Integer id, String code, String msg) {
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
