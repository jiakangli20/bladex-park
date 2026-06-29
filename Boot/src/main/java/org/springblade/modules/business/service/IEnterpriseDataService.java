/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import org.springblade.core.tool.support.Kv;

/**
 * 在园企业数据看板服务.
 *
 * @author BladeX
 */
public interface IEnterpriseDataService {

	/**
	 * 获取看板总览.
	 *
	 * @param parkId 园区ID
	 * @return 看板数据
	 */
	Kv overview(Long parkId);

}
