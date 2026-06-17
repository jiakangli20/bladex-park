/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页工作台聚合数据.
 *
 * @author BladeX
 */
@Data
@Schema(description = "首页工作台聚合数据")
public class HomeWorkbenchVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "统计概览")
	private HomeOverviewVO overview = new HomeOverviewVO();

	@Schema(description = "Banner")
	private HomeBannerVO banner = HomeBannerVO.defaults();

	@Schema(description = "政策通知")
	private List<HomePolicyNoticeVO> policyNotices = new ArrayList<>();

	@Schema(description = "入驻企业")
	private List<HomeEnterpriseVO> enterprises = new ArrayList<>();

	@Schema(description = "待办提醒")
	private HomeTodoVO todos = new HomeTodoVO();

	@Schema(description = "待补接口")
	private List<HomeMissingApiVO> missingApis = new ArrayList<>();

}
