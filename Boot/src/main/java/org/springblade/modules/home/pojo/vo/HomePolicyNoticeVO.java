/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 首页政策通知.
 *
 * @author BladeX
 */
@Data
@Schema(description = "首页政策通知")
public class HomePolicyNoticeVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "标题")
	private String title;

	@Schema(description = "链接")
	private String linkUrl;

	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "发布日期")
	private Date publishTime;

}
