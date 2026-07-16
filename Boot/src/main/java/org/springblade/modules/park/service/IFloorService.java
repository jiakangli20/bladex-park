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
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.park.pojo.entity.Floor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 楼层服务类
 *
 * @author Chill
 */
public interface IFloorService extends IService<Floor> {

	/**
	 * 查询楼层列表
	 *
	 * @param floor 查询条件
	 * @return 楼层集合
	 */
	List<Floor> selectFloorList(Floor floor);

	/**
	 * 查询楼层结构列表，不附带房态统计和房间明细
	 *
	 * @param floor 查询条件
	 * @return 楼层集合
	 */
	List<Floor> selectFloorStructureList(Floor floor);

	/**
	 * 查询楼层分页
	 *
	 * @param page 分页
	 * @param floor 查询条件
	 * @return 楼层分页
	 */
	IPage<Floor> selectFloorPage(IPage<Floor> page, Floor floor);

	/**
	 * 查询楼层详情
	 *
	 * @param id 楼层ID
	 * @param roomStatus 房间状态
	 * @return 楼层
	 */
	Floor selectFloorById(Long id, String roomStatus);

	/**
	 * 查询建筑下指定楼层
	 *
	 * @param buildingId 建筑ID
	 * @param floorNo 楼层号
	 * @return 楼层
	 */
	Floor selectFloorByBuildingAndNo(Long buildingId, Integer floorNo);

	/**
	 * 同步建筑楼层
	 *
	 * @param buildingId 建筑ID
	 * @param operator 操作人
	 */
	void syncBuildingFloors(Long buildingId, String operator);

	/**
	 * 同步全部建筑楼层
	 *
	 * @param operator 操作人
	 */
	void syncAllBuildingFloors(String operator);

	/**
	 * 新增或修改楼层
	 *
	 * @param floor 楼层
	 * @return 是否成功
	 */
	boolean submit(Floor floor);

	/**
	 * 删除楼层
	 *
	 * @param ids 主键集合
	 * @return 是否成功
	 */
	boolean removeFloor(String ids);

	/**
	 * 楼层统计
	 *
	 * @param floor 查询条件
	 * @return 统计信息
	 */
	Map<String, Object> selectFloorStatistics(Floor floor);

	/**
	 * 更新建筑楼层面积
	 *
	 * @param buildingId 建筑ID
	 * @param floorAreaMap 楼层面积
	 * @param operator 操作人
	 */
	void updateBuildingFloorAreas(Long buildingId, Map<Integer, BigDecimal> floorAreaMap, String operator);

}
