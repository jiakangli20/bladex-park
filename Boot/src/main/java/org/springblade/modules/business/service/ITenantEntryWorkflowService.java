/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import org.springblade.plugin.workflow.process.dto.WfNoticeDTO;

/**
 * 入驻审批工作流业务服务.
 *
 * @author BladeX
 */
public interface ITenantEntryWorkflowService {

	/**
	 * 是否入驻审批流程.
	 *
	 * @param notice 流程通知
	 * @return 是否支持
	 */
	boolean supports(WfNoticeDTO notice);

	/**
	 * Flowable 流程通知回调.
	 *
	 * @param notice 流程通知
	 */
	void businessWithNotice(WfNoticeDTO notice);

}
