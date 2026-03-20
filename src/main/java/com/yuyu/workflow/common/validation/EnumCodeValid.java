package com.yuyu.workflow.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于枚举编码值的参数合法性校验注解。
 */
@Documented
@Constraint(validatedBy = EnumCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumCodeValid {

    /**
     * 校验失败时的默认提示。
     */
    String message() default "枚举编码不合法";

    /**
     * 指定参与校验的枚举类型。
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * 是否允许空值。
     */
    boolean allowNull() default true;

    /**
     * Bean Validation 分组。
     */
    Class<?>[] groups() default {};

    /**
     * Bean Validation 负载参数。
     */
    Class<? extends Payload>[] payload() default {};
}
