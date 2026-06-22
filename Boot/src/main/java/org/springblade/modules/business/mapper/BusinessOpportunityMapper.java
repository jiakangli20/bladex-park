/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFile;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFollow;

import java.util.List;
import java.util.Map;

/**
 * 商机 Mapper.
 *
 * @author BladeX
 */
public interface BusinessOpportunityMapper extends BaseMapper<BusinessOpportunity> {

	BusinessOpportunity selectBusinessOpportunityById(@Param("opportunityId") Long opportunityId);

	List<BusinessOpportunity> selectBusinessOpportunityList(@Param("opportunity") BusinessOpportunity opportunity);

	IPage<BusinessOpportunity> selectBusinessOpportunityPage(IPage<BusinessOpportunity> page,
															 @Param("opportunity") BusinessOpportunity opportunity);

	int insertBusinessOpportunity(BusinessOpportunity opportunity);

	int updateBusinessOpportunity(BusinessOpportunity opportunity);

	int deleteBusinessOpportunityByIds(@Param("ids") List<Long> ids);

	String selectLastOpportunityNoByDate(@Param("datePrefix") String datePrefix);

	Integer countByEnterpriseName(@Param("enterpriseName") String enterpriseName,
								  @Param("excludeOpportunityId") Long excludeOpportunityId);

	List<BusinessOpportunityFollow> selectFollowList(@Param("opportunityId") Long opportunityId);

	int insertFollow(BusinessOpportunityFollow follow);

	List<BusinessOpportunityFile> selectFileList(@Param("opportunityId") Long opportunityId);

	int insertFile(BusinessOpportunityFile file);

	Map<String, Object> selectOpportunityStatistics();

	Long selectCustomerIdById(@Param("customerId") Long customerId);

	Long selectCustomerIdByCreditCode(@Param("creditCode") String creditCode);

	Integer countTableRows(@Param("tableName") String tableName);

	Long selectApprovalFlowId(@Param("parkId") Long parkId, @Param("flowId") Long flowId);

	Long selectExistingApprovalProjectId(@Param("opportunityId") Long opportunityId);

	List<Map<String, Object>> selectApprovalNodeCandidates(@Param("flowId") Long flowId);

	String selectApprovalFlowNodeConfig(@Param("flowId") Long flowId);

	int insertApprovalProject(@Param("opportunity") BusinessOpportunity opportunity,
							  @Param("flowId") Long flowId,
							  @Param("loginName") String loginName);

	int updateApprovalProjectStatus(@Param("projectId") Long projectId,
									@Param("loginName") String loginName,
									@Param("currentNode") String currentNode,
									@Param("currentNodeName") String currentNodeName);

	int insertApprovalLog(@Param("opportunity") BusinessOpportunity opportunity,
						  @Param("projectId") Long projectId,
						  @Param("flowId") Long flowId,
						  @Param("loginName") String loginName);

}
