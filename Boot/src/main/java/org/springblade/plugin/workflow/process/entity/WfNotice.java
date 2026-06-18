package org.springblade.plugin.workflow.process.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 流程消息实体类
 *
 * @author ssc
 */
@Data
@Accessors(chain = true)
public class WfNotice implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "发送者")
	private String fromUserId;

	@Schema(description = "接受者")
	private String toUserId;

	@Schema(description = "评论")
	private String comment;

	@Schema(description = "任务id")
	private String taskId;

	@Schema(description = "当前任务")
	private Task task;

	@Schema(description = "流程实例id")
	private String processInsId;

	@Schema(description = "流程实例")
	private ProcessInstance processInstance;

	@Schema(description = "流程变量")
	private Map<String, Object> variables;

	@Schema(description = "消息类型")
	private Type type;

	@Getter
	@AllArgsConstructor
	public enum Type {

		START("start", "发起流程", 0),
		TASK_CREATE("task_create", "任务创建", 1),
		TASK_COMPLETE("task_complete", "任务完成", 49),
		REJECT("reject", "审核驳回", 2),
		RECALL("recall", "撤回流程", 3),
		TRANSFER("transfer", "转办任务", 4),
		DELEGATE("delegate", "委托任务", 5),
		COPY("copy", "抄送任务", 6),
		ASSIGNEE("assignee", "变更审核人", 7),
		SUSPEND("suspend", "流程挂起", 8),
		ACTIVE("active", "流程激活", 9),
		URGE("urge", "催办任务", 10),
		ADD_MULTI_INSTANCE("addMultiInstance", "加签任务", 11),
		DELETE_MULTI_INSTANCE("deleteMultiInstance", "减签任务", 12),
		DELETE_PROCESS("delete", "删除流程", 96),
		WITHDRAW("withdraw", "撤销流程", 97),
		TERMINATE("terminate", "流程终止", 98),
		FINISH("finish", "流程结束", 99)
		;

		private final String type;
		private final String name;
		private final Integer code;
	}

	public static Type getTypeByCode(Integer code) {
		for (Type type : Type.values()) {
			if (type.getCode().equals(code)) {
				return type;
			}
		}
		return null;
	}
}
