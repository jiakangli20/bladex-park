package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.springblade.core.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 流程流水号实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_serial")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfSerial对象", description = "流程流水号")
public class WfSerial extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "部署id")
	private String deploymentId;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "前缀")
	private String prefix;

	@Schema(description = "日期格式化")
	private String dateFormat;

	@Schema(description = "后缀位数")
	private Integer suffixLength;

	@Schema(description = "初始数值")
	private Integer startSequence;

	@Schema(description = "连接符")
	private String connector;

	@Schema(description = "当前序列")
	private Integer currentSequence;

	@Schema(description = "重置周期 none不重置 day天 week周 month月 year年")
	private String cycle;


}
