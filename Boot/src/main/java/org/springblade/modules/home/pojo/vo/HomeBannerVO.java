/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页 Banner.
 *
 * @author BladeX
 */
@Data
@Schema(description = "首页Banner")
public class HomeBannerVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "标题")
	private String name;

	@Schema(description = "描述")
	private String bannerDesc;

	@Schema(description = "图片地址")
	private String imageUrl;

	public static HomeBannerVO defaults() {
		HomeBannerVO banner = new HomeBannerVO();
		banner.setName("智慧园区工作台");
		banner.setBannerDesc("聚合房源、客户、合同、审批与任务，助力园区高效运营");
		banner.setImageUrl("/img/bg/bg1.jpg");
		return banner;
	}

}
