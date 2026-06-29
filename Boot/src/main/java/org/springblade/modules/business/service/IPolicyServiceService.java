/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.pojo.entity.PolicyService;

import java.util.List;

/**
 * 政策服务发布服务.
 *
 * @author BladeX
 */
public interface IPolicyServiceService extends IService<PolicyService> {

	PolicyService selectPolicyById(Long policyId);

	PolicyService selectMiniAppPolicyById(Long policyId);

	List<PolicyService> selectPolicyList(PolicyService policy);

	IPage<PolicyService> selectPolicyPage(IPage<PolicyService> page, PolicyService policy);

	Kv selectPolicyStatistics(PolicyService policy);

	boolean insertPolicy(PolicyService policy);

	boolean updatePolicy(PolicyService policy);

	boolean submitPolicy(PolicyService policy);

	boolean deletePolicyByIds(String ids);

	boolean changeOnlineFlag(Long policyId, String onlineFlag);

	boolean copyPolicy(Long policyId);

}
