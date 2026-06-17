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
	 * @return 看板数据
	 */
	Map<String, Object> getBoard(Long parkId, Long buildingId, Integer floorNo, String keyword, String searchType, String status, String orientation);

	/**
	 * 查询工单占位数据
	 *
	 * @return 工单占位数据
	 */
	Map<String, Object> workorders();

	/**
	 * 上报工单占位
	 *
	 * @return 提示信息
	 */
	Map<String, Object> reportWorkorder();

}
