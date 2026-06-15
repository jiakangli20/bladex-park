/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
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

/**
 * 商机附件实体.
 *
 * @author BladeX
 */
@Data
@TableName("biz_business_opportunity_file")
@Schema(description = "商机附件")
public class BusinessOpportunityFile implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "file_id", type = IdType.AUTO)
	@Schema(description = "附件ID")
	private Long fileId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "商机ID")
	private Long opportunityId;

	@Schema(description = "文件名称")
	private String fileName;

	@Schema(description = "文件地址")
	private String fileUrl;

	@Schema(description = "文件后缀")
	private String fileSuffix;

	@Schema(description = "文件大小")
	private Long fileSize;

	@Schema(description = "创建人")
	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

}
