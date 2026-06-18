package org.springblade.plugin.workflow.core.constant;

/**
 * 流程扩展字段常量
 */
public interface WfExtendConstant {

	/**
	 * 人员
	 */
	String ASSIGNEE = "assignee";

	/**
	 * 按钮
	 */
	String BUTTON = "button";

	/**
	 * 流水号
	 */
	String SERIAL = "serial";

	/**
	 * 默认驳回节点
	 */
	String ROLLBACK_NODE = "rollbackNode";

	/**
	 * 是否跳过第一节点
	 */
	String SKIP_FIRST_NODE = "skipFirstNode";

	/**
	 * 任务处理人为发起人时跳过
	 */
	String SKIP_WHEN_START_USER = "skipWhenStartUser";

	/**
	 * 之前任务已处理过时跳过
	 */
	String SKIP_WHEN_ASSIGNEE = "skipWhenAssignee";

	/**
	 * 外置表单key
	 */
	String EX_FORM_KEY = "exFormKey";

	/**
	 * 外置表单url
	 */
	String EX_FORM_URL = "exFormUrl";

	/**
	 * 外置app表单url
	 */
	String EX_APP_FORM_URL = "exAppFormUrl";

	/**
	 * 隐藏评论附件选项
	 */
	String HIDE_ATTACHMENT = "hideAttachment";

	/**
	 * 隐藏抄送人选项
	 */
	String HIDE_COPY = "hideCopy";

	/**
	 * 隐藏选择下一步审核人选项
	 */
	String HIDE_EXAMINE = "hideExamine";

	/**
	 * 默认抄送人
	 */
	String COPY_USER = "copyUser";

	/**
	 * 重新提交回到驳回者
	 */
	String BACK_TO_REJECTER = "backToRejecter";

	/**
	 * 节点独立表单key
	 */
	String INDEP_FORM_KEY = "indepFormKey";

	/**
	 * 节点独立表单汇总
	 */
	String INDEP_FORM_SUMMARY = "indepFormSummary";

	/**
	 * 节点独立表单汇总节点
	 */
	String INDEP_FORM_SUMMARY_NODE = "indepSummaryNode";

	/**
	 * 人员配置交集
	 */
	String ASSIGNEE_RETAIN = "assigneeRetain";

	/**
	 * 表单属性
	 */
	String FORM_PROP = "formProp";
}
