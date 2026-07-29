package org.springblade.modules.contract.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("biz_contract_print_template")
public class ContractPrintTemplate implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(value = "template_id", type = IdType.AUTO)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long templateId;
	private String businessType;
	private String templateName;
	private String versionNo;
	private String fileName;
	private String fileUrl;
	private String fileObjectName;
	private String fileSuffix;
	private Long fileSize;
	private String enabledFlag;
	private String builtinFlag;
	private String remark;
	private String delFlag;
	private String createBy;
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date createTime;
	private String updateBy;
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date updateTime;
}
