package com.yuyu.workflow.common.enums;

/**
 * 流程定义状态枚举。
 */
public enum WorkflowDefinitionStatusEnum implements BaseEnum {

    DRAFT(0, "DRAFT", "草稿"),
    PUBLISHED(1, "PUBLISHED", "已发布"),
    DISABLED(2, "DISABLED", "已停用");

    private final Integer id;
    private final String code;
    private final String msg;

    WorkflowDefinitionStatusEnum(Integer id, String code, String msg) {
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

    public static String getMsgById(Integer id) {
        return EnumUtils.getMsgById(values(), id);
    }

    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }
}
