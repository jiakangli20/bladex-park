package org.springblade.plugin.workflow.process.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.io.Serializable;
import java.util.Map;

/**
 * 数据传输对象实体类
 *
 * @author ssc
 */
@Data
@Accessors(chain = true)
public class WfBaseDTO implements Serializable {

    /**
     * 任务
     */
    private Task task;

    /**
     * 任务id
     */
    private String taskId;

    /**
     * 流程实例
     */
    private ProcessInstance processInstance;

    /**
     * 流程实例id
     */
    private String processInstanceId;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

}
