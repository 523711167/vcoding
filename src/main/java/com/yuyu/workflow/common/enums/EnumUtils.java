package com.yuyu.workflow.common.enums;

import java.util.Objects;

public final class EnumUtils {

    /**
     * 工具类不允许实例化。
     */
    private EnumUtils() {
    }

    /**
     * 根据 id 获取枚举文案。
     */
    public static <E extends Enum<E> & BaseEnum> String getMsgById(E[] values, Integer id) {
        for (E item : values) {
            if (Objects.equals(item.getId(), id)) {
                return item.getName();
            }
        }
        return "";
    }

    /**
     * 根据 code 获取枚举文案。
     */
    public static <E extends Enum<E> & BaseEnum> String getMsgByCode(E[] values, String code) {
        for (E item : values) {
            if (Objects.equals(item.getCode(), code)) {
                return item.getName();
            }
        }
        return "";
    }
}
