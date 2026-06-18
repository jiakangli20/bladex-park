package org.springblade.plugin.workflow.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WfProcessStatusEnum {

    /**
     * flowable流程只分为进行中、已结束。其他均为业务状态
     * <p>
     * 小于50，进行中
     * 大于50，已结束
     */
    RUNNING(1, "running", "进行中"),
    REJECT(2, "reject", "驳回"),
    RECALL(3, "recall", "撤回"),
    DELETE(96, "delete", "删除"),
    WITHDRAW(97, "withdraw", "撤销"),
    TERMINATE(98, "true", "终止"),
    FINISH(99, "finish", "结束"),
    ;

    private final Integer code;
    private final String status;
    private final String name;

    /**
     * 根据 status 获取对应的 code
     *
     * @param status 输入的状态字符串
     * @return 对应的 code，如果未找到返回 -1
     */
    public static Integer getCodeByStatus(String status) {
        for (WfProcessStatusEnum value : WfProcessStatusEnum.values()) {
            if (value.getStatus().equalsIgnoreCase(status)) {
                return value.getCode();
            }
        }
        return null;
    }

    /**
     * 根据 code 获取对应的 status
     *
     * @param code 输入的状态代码
     * @return 对应的 status，如果未找到返回 null
     */
    public static String getStatusByCode(int code) {
        for (WfProcessStatusEnum value : WfProcessStatusEnum.values()) {
            if (value.getCode() == code) {
                return value.getStatus();
            }
        }
        return null;
    }
}
