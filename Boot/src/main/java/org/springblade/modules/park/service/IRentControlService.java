/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.park.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.business.pojo.entity.ServiceWorkorder;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractPayment;

import java.util.Map;

/**
 * 租控管理服务类
 *
 * @author Chill
 */
public interface IRentControlService {

	/**
	 * 查询租控看板
	 *
	 * @param parkId 园区ID
	 * @param buildingId 建筑ID
	 * @param floorNo 楼层号
	 * @param keyword 关键字
	 * @param searchType 搜索类型
	 * @param status 房源状态
	 * @param orientation 朝向
	 * @param includeTree 是否返回完整园区树
	 * @return 看板数据
	 */
	Map<String, Object> getBoard(Long parkId, Long buildingId, Integer floorNo, String keyword, String searchType, String status, String orientation, boolean includeTree);

	/**
	 * 查询房源关联合同
	 */
	IPage<Contract> roomContracts(IPage<Contract> page, Long roomId);

	/**
	 * 查询房源关联账单
	 */
	IPage<ContractPayment> roomPayments(IPage<ContractPayment> page, Long roomId);

	/**
	 * 查询物业工单数据
	 *
	 * @return 工单数据
	 */
	Map<String, Object> workorders();

	/**
	 * 上报物业工单
	 *
	 * @param workorder 工单信息
	 * @return 保存结果
	 */
	Map<String, Object> reportWorkorder(ServiceWorkorder workorder);

}
