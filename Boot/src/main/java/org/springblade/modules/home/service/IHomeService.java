/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.service;

import org.springblade.modules.home.pojo.vo.HomeMissingApiVO;
import org.springblade.modules.home.pojo.vo.HomeWorkbenchVO;

import java.util.List;

/**
 * 首页聚合服务.
 *
 * @author BladeX
 */
public interface IHomeService {

	/**
	 * 查询首页工作台聚合数据.
	 */
	HomeWorkbenchVO workbench();

	/**
	 * 查询待补接口清单.
	 */
	List<HomeMissingApiVO> missingApis();

}
