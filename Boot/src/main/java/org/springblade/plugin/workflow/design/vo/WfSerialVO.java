package org.springblade.plugin.workflow.design.vo;

import org.springblade.plugin.workflow.design.entity.WfSerial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 流程流水号视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfSerialVO对象", description = "流程流水号")
public class WfSerialVO extends WfSerial {
	@Serial
	private static final long serialVersionUID = 1L;

}
