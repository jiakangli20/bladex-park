package org.springblade.modules.ai.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/** 持久化的企业 AI 分析报告。 */
@Data
@TableName("biz_ai_enterprise_report")
public class AiEnterpriseReport {

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(type = IdType.AUTO)
	private Long id;
	@JsonIgnore
	private String tenantId;
	@JsonSerialize(using = ToStringSerializer.class)
	@JsonIgnore
	private Long userId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;
	private String enterpriseName;
	private String title;
	private String requestContent;
	private String companyOverview;
	private String riskAnalysis;
	private String htmlContent;
	private String status;
	private Date generatedTime;
	private Date createTime;
	private Date updateTime;
}
