package com.yuyu.workflow.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 流程节点类型枚举。
 */
public enum WorkflowNodeTypeEnum implements BaseEnum {

    START(1, "START", "开始节点"),
    APPROVAL(2, "APPROVAL", "审批节点"),
    CONDITION(3, "CONDITION", "条件节点"),
    PARALLEL_SPLIT(4, "PARALLEL_SPLIT", "并行拆分"),
    PARALLEL_JOIN(5, "PARALLEL_JOIN", "并行聚合"),
    END(6, "END", "结束节点");

    private final Integer id;
    private final String code;
    private final String name;

    WorkflowNodeTypeEnum(Integer id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
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
        return name;
    }

    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }

    public static boolean containsCode(String code) {
        return Arrays.stream(values()).anyMatch(item -> Objects.equals(item.getCode(), code));
    }

    public static boolean isStart(String code) {
        return START.getCode().equals(code);
    }

    public static boolean isApproval(String code) {
        return APPROVAL.getCode().equals(code);
    }

    public static boolean isCondition(String code) {
        return CONDITION.getCode().equals(code);
    }

    public static boolean isParallelSplit(String code) {
        return PARALLEL_SPLIT.getCode().equals(code);
    }

    public static boolean isParallelJoin(String code) {
        return PARALLEL_JOIN.getCode().equals(code);
    }

    public static boolean isEnd(String code) {
        return END.getCode().equals(code);
    }

    public static boolean isComposite(String code) {
        return CONDITION.getCode().equals(code) || PARALLEL_SPLIT.getCode().equals(code);
    }

}
