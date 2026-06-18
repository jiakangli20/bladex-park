package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;

/**
 * 流程表单字段默认值实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_form_default_values")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfFormDefaultValues对象", description = "流程表单字段默认值")
public class WfFormDefaultValues extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "内容")
	private String content;

	@Schema(description = "字段类型")
	private String fieldType;

}
