package org.springblade.plugin.workflow.core.utils;

import org.flowable.common.engine.api.query.Query;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.process.model.WfProcess;

/**
 * 表单搜索条件工具类
 *
 * @author ssc
 */
public class WfSearchUtil {

	private static final String CONDITION_EQUAL = "equal";
	private static final String CONDITION_NOT_EQUAL = "notEqual";
	private static final String CONDITION_LIKE = "like";
	private static final String CONDITION_EXISTS = "exists";
	private static final String CONDITION_NOT_EXISTS = "notExists";
	private static final String CONDITION_GREATER_THAN = "greaterThan";
	private static final String CONDITION_LESS_THAN = "lessThan";
	private static final String CONDITION_GREATER_THAN_OR_EQUAL = "greaterThanOrEqual";
	private static final String CONDITION_LESS_THAN_OR_EQUAL = "lessThanOrEqual";

	public static void buildSearchQuery(Query query, WfProcess process) {
		if (query instanceof HistoricTaskInstanceQuery) { // 我的已办
			buildHistoricTaskQuery((HistoricTaskInstanceQuery) query, process);
		} else if (query instanceof HistoricProcessInstanceQuery) { // 我的请求、办结
			buildHistoricProcessQuery((HistoricProcessInstanceQuery) query, process);
		} else if (query instanceof TaskQuery) { // 我的待办、待签
			buildTaskQuery((TaskQuery) query, process);
		}
	}

	private static void buildHistoricTaskQuery(HistoricTaskInstanceQuery query, WfProcess process) {
		if (StringUtil.isNotBlank(process.getProcessDefinitionName())) { // 流程名称
			query.processDefinitionNameLike("%" + process.getProcessDefinitionName() + "%");
		}
		if (StringUtil.isNotBlank(process.getProcessDefinitionKey())) { // 流程标识
			query.processDefinitionKey(process.getProcessDefinitionKey());
		}
		if (StringUtil.isNotBlank(process.getSerialNumber())) { // 流水号
			query.processVariableValueLike(WfProcessConstant.TASK_VARIABLE_SN, "%" + process.getSerialNumber() + "%");
		}
		if (StringUtil.isNotBlank(process.getStartUsername())) { // 发起人
			query.processVariableValueLike(WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME, "%" + process.getStartUsername() + "%");
		}
		if (StringUtil.isNotBlank(process.getCategory())) { // 分类
			query.processCategoryIn(Func.toStrList(process.getCategory()));
		}
		if (StringUtil.isNotBlank(process.getStartTimeRange())) { // 开始时间
			String[] dates = Func.toStrArray(process.getStartTimeRange());
			if (dates.length == 2) {
				query.taskCreatedBefore(Func.parseDate(dates[1], DateUtil.PATTERN_DATETIME));
				query.taskCreatedAfter(Func.parseDate(dates[0], DateUtil.PATTERN_DATETIME));
			}
		}
		if (StringUtil.isNotBlank(process.getFormSearch())) { // 流程变量
			for (String search : process.getFormSearch().split(",")) {
				String[] arr = search.split(":");
                if (arr.length == 2 || arr.length == 3 || arr.length == 4) {
                    String column = arr[0];
                    String condition = arr[1];
                    Object value = getValue(arr);
					switch (condition) {
						case CONDITION_EQUAL:
							query.processVariableValueEquals(column, value);
							break;
						case CONDITION_NOT_EQUAL:
							query.processVariableValueNotEquals(column, value);
							break;
						case CONDITION_LIKE:
							query.processVariableValueLike(column, "%" + value + "%");
							break;
						case CONDITION_EXISTS:
							query.processVariableExists(column);
							break;
						case CONDITION_NOT_EXISTS:
							query.processVariableNotExists(column);
							break;
						case CONDITION_GREATER_THAN:
							query.processVariableValueGreaterThan(column, value);
							break;
						case CONDITION_LESS_THAN:
							query.processVariableValueLessThan(column, value);
							break;
						case CONDITION_GREATER_THAN_OR_EQUAL:
							query.processVariableValueGreaterThanOrEqual(column, value);
							break;
						case CONDITION_LESS_THAN_OR_EQUAL:
							query.processVariableValueLessThanOrEqual(column, value);
							break;
						default:
							break;
					}
				}
			}
		}

	}

	private static void buildTaskQuery(TaskQuery query, WfProcess process) {
		if (StringUtil.isNotBlank(process.getProcessDefinitionName())) { // 流程名称
			query.processDefinitionNameLike("%" + process.getProcessDefinitionName() + "%");
		}
		if (StringUtil.isNotBlank(process.getProcessDefinitionKey())) { // 流程标识
			query.processDefinitionKey(process.getProcessDefinitionKey());
		}
		if (StringUtil.isNotBlank(process.getSerialNumber())) { // 流水号
			query.processVariableValueLike(WfProcessConstant.TASK_VARIABLE_SN, "%" + process.getSerialNumber() + "%");
		}
		if (StringUtil.isNotBlank(process.getStartUsername())) { // 发起人
			query.processVariableValueLike(WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME, "%" + process.getStartUsername() + "%");
		}
		if (StringUtil.isNotBlank(process.getCategory())) { // 分类
			query.processCategoryIn(Func.toStrList(process.getCategory()));
		}
		if (StringUtil.isNotBlank(process.getStartTimeRange())) { // 开始时间
			String[] dates = Func.toStrArray(process.getStartTimeRange());
			if (dates.length == 2) {
				query.taskCreatedBefore(Func.parseDate(dates[1], DateUtil.PATTERN_DATETIME));
				query.taskCreatedAfter(Func.parseDate(dates[0], DateUtil.PATTERN_DATETIME));
			}
		}
        if (StringUtil.isNotBlank(process.getTaskName())) { // 任务名称
            query.taskNameLike("%" + process.getTaskName() + "%");
        }
        if (StringUtil.isNotBlank(process.getAssignee())) { // 审核人/候选人
            query.taskCandidateOrAssigned(process.getAssignee());
        }
		if (StringUtil.isNotBlank(process.getFormSearch())) { // 流程变量
			for (String search : process.getFormSearch().split(",")) {
				String[] arr = search.split(":");
                if (arr.length == 2 || arr.length == 3 || arr.length == 4) {
                    String column = arr[0];
                    String condition = arr[1];
                    Object value = getValue(arr);
					switch (condition) {
						case CONDITION_EQUAL:
							query.processVariableValueEquals(column, value);
							break;
						case CONDITION_NOT_EQUAL:
							query.processVariableValueNotEquals(column, value);
							break;
						case CONDITION_LIKE:
							query.processVariableValueLike(column, "%" + value + "%");
							break;
						case CONDITION_EXISTS:
							query.processVariableExists(column);
							break;
						case CONDITION_NOT_EXISTS:
							query.processVariableNotExists(column);
							break;
						case CONDITION_GREATER_THAN:
							query.processVariableValueGreaterThan(column, value);
							break;
						case CONDITION_LESS_THAN:
							query.processVariableValueLessThan(column, value);
							break;
						case CONDITION_GREATER_THAN_OR_EQUAL:
							query.processVariableValueGreaterThanOrEqual(column, value);
							break;
						case CONDITION_LESS_THAN_OR_EQUAL:
							query.processVariableValueLessThanOrEqual(column, value);
							break;
						default:
							break;
					}
				}
			}
		}

	}

	private static void buildHistoricProcessQuery(HistoricProcessInstanceQuery query, WfProcess process) {
		if (StringUtil.isNotBlank(process.getProcessDefinitionName())) { // 流程名称
			query.processInstanceNameLike("%" + process.getProcessDefinitionName() + "%");
		}
		if (StringUtil.isNotBlank(process.getProcessDefinitionKey())) { // 流程标识
			query.processDefinitionKey(process.getProcessDefinitionKey());
		}
		if (StringUtil.isNotBlank(process.getSerialNumber())) { // 流水号
			query.variableValueLike(WfProcessConstant.TASK_VARIABLE_SN, "%" + process.getSerialNumber() + "%");
		}
		if (StringUtil.isNotBlank(process.getStartUsername())) { // 申请人
			query.variableValueLike(WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME, "%" + process.getStartUsername() + "%");
		}
		if (StringUtil.isNotBlank(process.getCategory())) { // 分类
			query.processDefinitionCategory(process.getCategory());
		}
		if (StringUtil.isNotBlank(process.getStartTimeRange())) { // 开始时间
			String[] dates = Func.toStrArray(process.getStartTimeRange());
			if (dates.length == 2) {
				query.startedAfter(Func.parseDate(dates[0], DateUtil.PATTERN_DATETIME));
				query.startedBefore(Func.parseDate(dates[1], DateUtil.PATTERN_DATETIME));
			}
		}
		if (StringUtil.isNotBlank(process.getEndTimeRange())) { // 结束时间
			String[] dates = Func.toStrArray(process.getEndTimeRange());
			if (dates.length == 2) {
				query.finishedAfter(Func.parseDate(dates[0], DateUtil.PATTERN_DATETIME));
				query.finishedBefore(Func.parseDate(dates[1], DateUtil.PATTERN_DATETIME));
			}
		}
		if (process.getProcessIsFinished() != null) { // 流程状态
			switch (process.getProcessIsFinished()) {
				case WfProcessConstant.STATUS_UNFINISHED:
					query.unfinished().notDeleted();
					break;
				case WfProcessConstant.STATUS_FINISHED:
					query.finished()
//						.notDeleted() // 排除删除
//						.variableNotExists(WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE) // 排除终结/撤销/撤回/驳回等
					;
					break;
				case WfProcessConstant.STATUS_TERMINATE:
					query.finished().notDeleted();
					query.variableValueEquals(WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE, "true");
					break;
				case WfProcessConstant.STATUS_WITHDRAW:
					query.finished().notDeleted();
					query.variableValueEquals(WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE, WfProcessConstant.STATUS_WITHDRAW);
					break;
				case WfProcessConstant.STATUS_REJECT:
					query.unfinished().notDeleted();
					query.variableValueEquals(WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE, WfProcessConstant.STATUS_REJECT);
					break;
				case WfProcessConstant.STATUS_RECALL:
					query.unfinished().notDeleted();
					query.variableValueEquals(WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE, WfProcessConstant.STATUS_RECALL);
					break;
				case WfProcessConstant.STATUS_DELETED:
					query.deleted();
					break;
			}
		}
		if (StringUtil.isNotBlank(process.getFormSearch())) { // 流程变量
			for (String search : process.getFormSearch().split(",")) {
				String[] arr = search.split(":");
                if (arr.length == 2 || arr.length == 3 || arr.length == 4) {
                    String column = arr[0];
                    String condition = arr[1];
                    Object value = getValue(arr);
					switch (condition) {
						case CONDITION_EQUAL:
							query.variableValueEquals(column, value);
							break;
						case CONDITION_NOT_EQUAL:
							query.variableValueNotEquals(column, value);
							break;
						case CONDITION_LIKE:
							query.variableValueLike(column, "%" + value + "%");
							break;
						case CONDITION_EXISTS:
							query.variableExists(column);
							break;
						case CONDITION_NOT_EXISTS:
							query.variableNotExists(column);
							break;
						case CONDITION_GREATER_THAN:
							query.variableValueGreaterThan(column, value);
							break;
						case CONDITION_LESS_THAN:
							query.variableValueLessThan(column, value);
							break;
						case CONDITION_GREATER_THAN_OR_EQUAL:
							query.variableValueGreaterThanOrEqual(column, value);
							break;
						case CONDITION_LESS_THAN_OR_EQUAL:
							query.variableValueLessThanOrEqual(column, value);
							break;
						default:
							break;
					}
				}
			}
		}
	}

    private static Object getValue(String[] arr) {
        Object value = null;
        String type = "string";
        if (arr.length == 3 || arr.length == 4) {
            if (arr.length == 4) type = arr[3];
            switch (type) {
                case "integer":
                    value = Integer.parseInt(arr[2]);
                    break;
                case "double":
                    value = Double.parseDouble(arr[2]);
                    break;
                case "long":
                    value = Long.parseLong(arr[2]);
                    break;
                default:
                    value = arr[2];
                    break;
            }
        }
        return value;
    }
}
