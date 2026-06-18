package org.springblade.plugin.workflow.design.dto;

import org.springblade.plugin.workflow.design.entity.WfListenerEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 任务/执行监听器 数据传输对象实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfListenerDTO extends WfListenerEntity {
	@Serial
	private static final long serialVersionUID = 1L;

}
