package org.springblade.modules.business.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("biz_background_investigation")
@Schema(description = "企业背景调查人工核验记录")
public class BackgroundInvestigation implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(value = "investigation_id", type = IdType.AUTO)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long investigationId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long opportunityId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;

	private String enterpriseName;
	private String verifyStatus;
	private String riskLevel;
	private String legalRiskFlag;
	private String executiveRiskFlag;
	private String shareholderRiskFlag;
	private String riskSummary;
	private String sourceRemark;
	private String riskDetailJson;
	private String externalStatus;
	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date createTime;
}
