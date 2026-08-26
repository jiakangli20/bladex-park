/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.PolicyService;

import java.util.List;
import java.util.Map;

/**
 * 政策服务发布 Mapper.
 *
 * @author BladeX
 */
public interface PolicyServiceMapper extends BaseMapper<PolicyService> {

	PolicyService selectPolicyById(@Param("policyId") Long policyId);

	List<PolicyService> selectPolicyList(@Param("policy") PolicyService policy,
										 @Param("authorizedParkIds") List<Long> authorizedParkIds);

	IPage<PolicyService> selectPolicyPage(IPage<PolicyService> page, @Param("policy") PolicyService policy,
										 @Param("authorizedParkIds") List<Long> authorizedParkIds);

	Map<String, Object> selectPolicyStatistics(@Param("policy") PolicyService policy,
											  @Param("authorizedParkIds") List<Long> authorizedParkIds);

	int insertPolicy(PolicyService policy);

	int updatePolicy(PolicyService policy);

	int deletePolicyByIds(@Param("ids") List<Long> ids, @Param("parkId") Long parkId, @Param("updateBy") String updateBy);

	int updateOnlineFlag(@Param("policyId") Long policyId, @Param("onlineFlag") String onlineFlag, @Param("updateBy") String updateBy);

	int increaseViewCount(@Param("policyId") Long policyId);

}
