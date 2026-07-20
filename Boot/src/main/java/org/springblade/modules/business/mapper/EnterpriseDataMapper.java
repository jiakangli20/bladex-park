/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 在园企业数据看板 Mapper.
 *
 * @author BladeX
 */
public interface EnterpriseDataMapper {

	Map<String, Object> selectFinanceOverview(@Param("parkId") Long parkId);

	Map<String, Object> selectContractExecution(@Param("parkId") Long parkId);

	Map<String, Object> selectRoomSummary(@Param("parkId") Long parkId);

	List<Map<String, Object>> selectVacancyWarning(@Param("parkId") Long parkId);

	List<Map<String, Object>> selectRentalTrend(@Param("parkId") Long parkId);

	List<Map<String, Object>> selectContractDealTrend(@Param("parkId") Long parkId);

	List<Map<String, Object>> selectNoticeTenantList(@Param("parkId") Long parkId);

	List<Map<String, Object>> selectOpportunityReminderList(@Param("parkId") Long parkId);

}
