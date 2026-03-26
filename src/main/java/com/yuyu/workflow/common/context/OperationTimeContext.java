package com.yuyu.workflow.common.context;

import java.time.LocalDateTime;

public class OperationTimeContext {


    private static final ThreadLocal<LocalDateTime> HOLDER = new ThreadLocal<>();

    private OperationTimeContext() {
    }

    public static void set(LocalDateTime operateAt) {
        HOLDER.set(operateAt);
    }

    public static LocalDateTime get() {
        return HOLDER.get();
    }

    public static LocalDateTime getOrNow() {
        LocalDateTime operateAt = HOLDER.get();
        return operateAt != null ? operateAt : LocalDateTime.now();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
