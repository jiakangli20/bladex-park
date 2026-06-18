package org.springblade.plugin.workflow.ops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;

/**
 * 流程代理实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_proxy")
@Schema(name = "WfProxy对象", description = "流程代理")
public class WfProxy implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "主键id")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	@Schema(description = "委托人")
	private Long userId;

	@Schema(description = "代理人")
	private Long proxyUserId;

	@Schema(description = "流程定义key")
	private String processDefKey;

	@Schema(description = "1永久 2定时")
	private String type;

	@Schema(description = "开始时间")
	private LocalDateTime startTime;

	@Schema(description = "结束时间")
	private LocalDateTime endTime;

	@Schema(description = "业务状态")
	private Integer status;
}
