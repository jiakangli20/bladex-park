/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.mapper;

import org.apache.ibatis.annotations.Param;
import org.springblade.modules.home.pojo.vo.HomeEnterpriseVO;

import java.util.List;

/**
 * 首页聚合 Mapper.
 *
 * @author BladeX
 */
public interface HomeMapper {

	/**
	 * 房源数量.
	 */
	Long countRooms(@Param("parkId") Long parkId);

	/**
	 * 客户数量.
	 */
	Long countCustomers(@Param("parkId") Long parkId);

	/**
	 * 即将到期合同数量.
	 */
	Long countExpiringContracts(@Param("parkId") Long parkId);

	/**
	 * 物业工单待办数量.
	 */
	Long countWorkorderTodos(@Param("parkId") Long parkId, @Param("currentUser") String currentUser, @Param("admin") Boolean admin);

	/**
	 * 入驻企业展示.
	 */
	List<HomeEnterpriseVO> selectEnterprises(@Param("parkId") Long parkId);

}
