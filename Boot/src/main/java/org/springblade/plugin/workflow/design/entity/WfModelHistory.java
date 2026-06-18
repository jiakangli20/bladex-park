package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程模型定义历史
 *
 * @author ssc
 */
@Data
@TableName("ACT_DE_MODEL_HISTORY")
@Schema(name = "流程模型定义历史")
public class WfModelHistory implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.ASSIGN_UUID)
	private String id;

	@Schema(description = "模型名称")
	private String name;

	@Schema(description = "模型key")
	private String modelKey;

	@Schema(description = "表单key")
	private String formKey;

	@Schema(description = "分类id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long categoryId;

	@Schema(description = "模型描述")
	private String description;

	@Schema(description = "模型评论")
	private String modelComment;

	@Schema(description = "创建时间")
	private Date created;

	@Schema(description = "更新时间")
	private Date lastUpdated;

	@Schema(description = "创建人")
	private String createdBy;

	@Schema(description = "更新人")
	private String lastUpdatedBy;

	@Schema(description = "版本")
	private Integer version;

	@Schema(description = "模型类型")
	private Integer modelType;

	@Schema(description = "移除时间")
	private Date removalDate;

	@Schema(description = "流程设计器xml")
	private String modelXml;

	@Schema(description = "模型编辑器json，可用于flowable-ui")
	private String modelEditorJson;

	@Schema(description = "模型id")
	private String modelId;

	@Schema(description = "租户id")
	private String tenantId;

}
