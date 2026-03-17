package com.yuyu.workflow.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 基于 Spring Bean 管理的 ObjectMapper 转换工具。
 *
 * <p>适用场景：</p>
 * <p>1. JSON 字符串与 Java 对象互转</p>
 * <p>2. Map、列表、嵌套结构的 JSON 解析</p>
 * <p>3. 非 HTTP 场景下的序列化与反序列化</p>
 *
 * <p>说明：对象之间的实体转换统一建议使用 MapStruct，而不是 ObjectMapper。</p>
 */
@Component
public class ObjectMapperUtils {

    private final ObjectMapper objectMapper;

    /**
     * 注入 Spring 管理的 ObjectMapper。
     */
    public ObjectMapperUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 JSON 字符串转换为指定对象。
     */
    public <T> T fromJson(String json, Class<T> targetClass) {
        try {
            return objectMapper.readValue(json, targetClass);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON convert to object failed", e);
        }
    }

    /**
     * 按复杂泛型结构将 JSON 字符串转换为对象。
     */
    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON convert to object failed", e);
        }
    }

    /**
     * 将 JSON 数组转换为指定类型列表。
     */
    public <T> List<T> fromJsonToList(String json, Class<T> targetClass) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, targetClass));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 将对象序列化为 JSON 字符串。
     */
    public String toJson(Object source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Object convert to JSON failed", e);
        }
    }
}
