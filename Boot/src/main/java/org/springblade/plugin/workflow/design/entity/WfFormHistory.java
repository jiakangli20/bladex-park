
package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springblade.core.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 流程表单实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_form_history")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfFormHistory对象", description = "流程表单")
public class WfFormHistory extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "表单id")
	private Long formId;

	@Schema(description = "表单key")
	private String formKey;

	@Schema(description = "表单名称")
	private String name;

	@Schema(description = "表单内容")
	private String content;

	@Schema(description = "app表单内容")
	private String appContent;

	@Schema(description = "版本")
	private Integer version;

	@Schema(description = "分类id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long categoryId;

	@Schema(description = "备注")
	private String remark;


}
