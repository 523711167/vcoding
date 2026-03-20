package com.yuyu.workflow.common.enums;

public enum DeptRelationTypeEnum implements BaseEnum {

    SELF(1, "SELF", "自身"),
    DESCENDANT(2, "DESCENDANT", "下级展开");

    private final Integer id;
    private final String code;
    private final String msg;

    DeptRelationTypeEnum(Integer id, String code, String msg) {
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
}
