package com.yuyu.workflow.common.validation;

import com.yuyu.workflow.common.enums.BaseEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Objects;

/**
 * 基于枚举编码值的参数合法性校验器。
 */
public class EnumCodeValidator implements ConstraintValidator<EnumCodeValid, String> {

    private Class<? extends Enum<?>> enumClass;
    private boolean allowNull;

    @Override
    public void initialize(EnumCodeValid constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (Objects.isNull(value) || value.isBlank()) {
            return allowNull;
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .map(item -> (BaseEnum) item)
                .anyMatch(item -> Objects.equals(item.getCode(), value));
    }
}
