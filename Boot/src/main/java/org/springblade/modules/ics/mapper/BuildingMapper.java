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
package org.springblade.modules.ics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.ics.pojo.entity.Building;
import org.springblade.modules.ics.pojo.vo.BuildingVO;

import java.util.List;

/**
 * 建筑 Mapper 接口
 *
 * @author Chill
 */
public interface BuildingMapper extends BaseMapper<Building> {

	/**
	 * 查询建筑列表
	 *
	 * @param building 查询条件
	 * @return 建筑集合
	 */
	List<Building> selectBuildingList(@Param("building") Building building);

	/**
	 * 查询建筑分页
	 *
	 * @param page 分页
	 * @param building 查询条件
	 * @return 建筑集合
	 */
	List<BuildingVO> selectBuildingPage(IPage page, @Param("building") Building building);

	/**
	 * 查询建筑详情
	 *
	 * @param id 建筑ID
	 * @return 建筑
	 */
	BuildingVO selectBuildingById(@Param("id") Long id);

	/**
	 * 查询同园区同名建筑
	 *
	 * @param parkId 园区ID
	 * @param name 建筑名称
	 * @return 建筑
	 */
	Building selectBuildingByParkAndName(@Param("parkId") Long parkId, @Param("name") String name);

	/**
	 * 查询建筑最大房源楼层
	 *
	 * @param buildingId 建筑ID
	 * @return 最大楼层号
	 */
	Integer selectMaxRoomFloor(@Param("buildingId") Long buildingId);

	/**
	 * 查询建筑房源数量
	 *
	 * @param buildingId 建筑ID
	 * @return 房源数量
	 */
	int countRoomsByBuildingId(@Param("buildingId") Long buildingId);

	/**
	 * 查询建筑楼层数量
	 *
	 * @param buildingId 建筑ID
	 * @return 楼层数量
	 */
	int countFloorsByBuildingId(@Param("buildingId") Long buildingId);

	/**
	 * 查询建筑合同引用数量
	 *
	 * @param buildingId 建筑ID
	 * @return 合同数量
	 */
	int countContractRefs(@Param("buildingId") Long buildingId);

	/**
	 * 判断表是否存在
	 *
	 * @param tableName 表名
	 * @return 数量
	 */
	int countTable(@Param("tableName") String tableName);

}
