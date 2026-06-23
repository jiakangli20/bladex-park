/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFile;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFollow;
import org.springblade.modules.business.pojo.entity.Tag;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 商机服务.
 *
 * @author BladeX
 */
public interface IBusinessOpportunityService extends IService<BusinessOpportunity> {

	BusinessOpportunity selectBusinessOpportunityById(Long opportunityId);

	List<BusinessOpportunity> selectBusinessOpportunityList(BusinessOpportunity opportunity);

	IPage<BusinessOpportunity> selectBusinessOpportunityPage(IPage<BusinessOpportunity> page, BusinessOpportunity opportunity);

	boolean insertBusinessOpportunity(BusinessOpportunity opportunity);

	boolean updateBusinessOpportunity(BusinessOpportunity opportunity);

	boolean submitBusinessOpportunity(BusinessOpportunity opportunity);

	boolean deleteBusinessOpportunityByIds(String ids);

	boolean addFollowRecord(Long opportunityId, BusinessOpportunityFollow follow);

	List<BusinessOpportunityFollow> selectFollowList(Long opportunityId);

	BusinessOpportunityFile uploadFile(Long opportunityId, MultipartFile file);

	List<BusinessOpportunityFile> selectFileList(Long opportunityId);

	List<Tag> selectTagsByOpportunityId(Long opportunityId);

	boolean setOpportunityTags(Long opportunityId, List<Long> tagIds);

	Map<String, Object> queryBackgroundInvestigation(Long opportunityId);

	Map<String, Object> queryBackgroundInvestigationByName(String enterpriseName);

	BusinessOpportunity createApprovalProjectFromOpportunity(Long opportunityId, Long flowId);

	Map<String, Object> exportTenantEntryApprovalForm(Long opportunityId, String processInsId);

	Map<String, Object> selectOpportunityStatistics();

}
