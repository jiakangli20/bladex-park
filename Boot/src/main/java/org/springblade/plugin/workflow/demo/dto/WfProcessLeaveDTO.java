package org.springblade.plugin.workflow.demo.dto;

import org.springblade.plugin.workflow.demo.entity.WfProcessLeave;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 请假流程业务示例数据传输对象实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfProcessLeaveDTO extends WfProcessLeave {
    @Serial
    private static final long serialVersionUID = 1L;

}
