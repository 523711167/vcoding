package com.yuyu.workflow.common.enums;

/**
 * 业务申请状态枚举。
 */
public enum BizApplyStatusEnum implements BaseEnum {

    DRAFT(0, "DRAFT", "草稿"),
    PENDING(1, "PENDING", "审批中"),
    APPROVED(2, "APPROVED", "已通过"),
    REJECTED(3, "REJECTED", "已拒绝"),
    CANCELED(4, "CANCELED", "已撤回");

    private final Integer id;
    private final String code;
    private final String msg;

    BizApplyStatusEnum(Integer id, String code, String msg) {
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
    public String getName() {
        return msg;
    }

    /**
     * 根据 id 获取状态说明。
     */
    public static String getMsgById(Integer id) {
        return EnumUtils.getMsgById(values(), id);
    }

    /**
     * 根据 code 获取状态说明。
     */
    public static String getMsgByCode(String code) {
        return EnumUtils.getMsgByCode(values(), code);
    }

    /**
     * 判断是否为草稿状态。
     */
    public static boolean isDraft(String code) {
        return DRAFT.getCode().equals(code);
    }

    public static BizApplyStatusEnum toBizApplyStatusEnum(String code) {
        if (WorkflowNodeInstanceStatusEnum.isRejected(code)) {
            return REJECTED;
        } else if (WorkflowNodeInstanceStatusEnum.isApproved(code)) {
            return APPROVED;
        } else {
            return PENDING;
        }
    }
}
