package com.yuyu.workflow.common.validation;

import com.yuyu.workflow.common.enums.BaseEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Objects;

/**
 * 基于枚举主键值的参数合法性校验器。
 */
public class EnumIdValidator implements ConstraintValidator<EnumIdValid, Integer> {

    private Class<? extends Enum<?>> enumClass;
    private boolean allowNull;

    /**
     * 初始化校验注解配置。
     */
    @Override
    public void initialize(EnumIdValid constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.allowNull = constraintAnnotation.allowNull();
    }

    /**
     * 校验当前字段值是否存在于指定枚举的 id 集合中。
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (Objects.isNull(value)) {
            return allowNull;
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(BaseEnum.class::isInstance)
                .map(BaseEnum.class::cast)
                .anyMatch(item -> Objects.equals(item.getId(), value));
    }
}
