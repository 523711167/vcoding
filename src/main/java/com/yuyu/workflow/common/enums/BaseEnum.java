package com.yuyu.workflow.common.enums;

public interface BaseEnum {

    /**
     * 获取枚举主键值。
     */
    Integer getId();

    /**
     * 获取枚举编码。
     */
    String getCode();

    /**
     * 获取枚举说明。
     */
    String getMsg();
}
