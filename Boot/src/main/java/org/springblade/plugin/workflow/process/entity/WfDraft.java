package org.springblade.plugin.workflow.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.Accessors;

/**
 * 流程草稿箱实体类
 *
 * @author ssc
 */
@Data
@Accessors(chain = true)
@TableName("blade_wf_draft")
@Schema(name = "WfDraft对象", description = "流程草稿箱")
public class WfDraft implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	@Schema(description = "用户id")
	private Long userId;

	@Schema(description = "表单key")
	private String formKey;

	@Schema(description = "流程定义id")
	private String processDefId;

	@Schema(description = "任务id")
	private String taskId;

	@Schema(description = "表单变量")
	private String variables;

	@Schema(description = "平台 pc/app")
	private String platform;


}
