package org.springblade.plugin.workflow.process.service;

import org.flowable.task.api.Task;
import org.springblade.plugin.workflow.process.model.WfTaskUser;

import org.springframework.lang.Nullable;

public interface IWfTaskService {

    /**
     * 获取指定节点的用户
     *
     * @param processDefId 流程定义id
     * @param processInsId 流程实例id
     * @param nodeId       节点id
     */
    WfTaskUser getTaskUser(String processDefId, @Nullable String processInsId, String nodeId);

    /**
     * 获取指定节点的用户
     *
     * @param processDefId 流程定义id
     * @param processInsId 流程实例id
     * @param nodeId       节点id
     * @param currentUser  当前登录人
     */
    WfTaskUser getTaskUser(String processDefId, @Nullable String processInsId, String nodeId, String currentUser);

    /**
     * 判断当前节点是否是多实例并行
     */
    Boolean isMultiInstance(String taskKey, String processDefId);

    /**
     * 检查是否跳过当前任务
     */
    Boolean checkIsSkip(Task task, String appointUser);

}
