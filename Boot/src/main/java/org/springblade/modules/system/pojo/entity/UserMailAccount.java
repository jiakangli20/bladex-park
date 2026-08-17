/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.system.pojo.entity;

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

/**
 * 用户SMTP邮箱绑定.
 */
@Data
@TableName("biz_user_mail_account")
public class UserMailAccount implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(value = "account_id", type = IdType.AUTO)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long accountId;

	private String tenantId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long userId;

	private String emailAddress;

	private String authCodeCiphertext;

	private String enabled;

	private String lastTestStatus;

	private String lastTestMessage;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date lastTestTime;

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
