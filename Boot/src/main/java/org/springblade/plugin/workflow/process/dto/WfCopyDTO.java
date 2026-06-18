package org.springblade.plugin.workflow.process.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.io.Serializable;

/**
 * 流程抄送数据传输对象实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class WfCopyDTO extends WfBaseDTO {

    /**
     * 抄送人
     */
    private String copyUser;

}
