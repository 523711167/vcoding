package com.yuyu.workflow.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页返回对象")
public record PageVo<T>(
        @Schema(description = "当前页码", example = "1") long pageNum,
        @Schema(description = "每页条数", example = "10") long pageSize,
        @Schema(description = "总记录数", example = "100") long total,
        @Schema(description = "总页数", example = "10") long totalPages,
        @Schema(description = "当前页数据") List<T> records) {

    /**
     * 构造分页结果。
     */
    public static <T> PageVo<T> of(long pageNum, long pageSize, long total, List<T> records) {
        long totalPages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageVo<>(pageNum, pageSize, total, totalPages, records);
    }
}
