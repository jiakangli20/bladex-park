/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.pojo.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 合同通知文件视图对象.
 *
 * @author BladeX
 */
@Data
@Schema(description = "合同通知文件视图对象")
public class ContractNoticeFileVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "通知类型")
	private String noticeType;

	@Schema(description = "通知名称")
	private String noticeName;

	@Schema(description = "文件名称")
	private String fileName;

	@Schema(description = "文件地址")
	private String fileUrl;

	@Schema(description = "内容类型")
	private String contentType;

	@Schema(description = "生成时间")
	private String generatedAt;

	@JsonIgnore
	@Schema(hidden = true)
	private byte[] fileBytes;

}
