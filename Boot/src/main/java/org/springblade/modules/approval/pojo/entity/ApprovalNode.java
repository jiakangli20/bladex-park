/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 审批流程节点实体.
 *
 * @author BladeX
 */
@Data
@TableName("biz_approval_node")
@Schema(description = "审批流程节点")
public class ApprovalNode implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "node_id", type = IdType.AUTO)
	@Schema(description = "节点ID")
	private Long nodeId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "流程ID")
	private Long flowId;

	@Schema(description = "节点名称")
	private String nodeName;

	@Schema(description = "节点顺序")
	private Integer nodeOrder;

	@Schema(description = "节点类型")
	private String nodeType;

	@Schema(description = "审批人账号")
	private String approverLogin;

	@Schema(description = "审批人名称")
	private String approverName;

	@Schema(description = "完成条件")
	private String completeCondition;

	@Schema(description = "超时时间")
	private Integer timeLimit;

	@Schema(description = "抄送人")
	private String ccUsers;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

}
