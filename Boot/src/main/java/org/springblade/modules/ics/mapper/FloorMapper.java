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
import org.springblade.modules.ics.pojo.dto.BuildingFloorAreaDTO;
import org.springblade.modules.ics.pojo.entity.Floor;
import org.springblade.modules.ics.pojo.entity.Room;

import java.math.BigDecimal;
import java.util.List;

/**
 * 楼层 Mapper 接口
 *
 * @author Chill
 */
public interface FloorMapper extends BaseMapper<Floor> {

	/**
	 * 查询楼层列表
	 *
	 * @param floor 查询条件
	 * @return 楼层集合
	 */
	List<Floor> selectFloorList(@Param("floor") Floor floor);

	/**
	 * 查询楼层分页
	 *
	 * @param page 分页
	 * @param floor 查询条件
	 * @return 楼层集合
	 */
	List<Floor> selectFloorPage(IPage<Floor> page, @Param("floor") Floor floor);

	/**
	 * 查询楼层详情
	 *
	 * @param id 楼层ID
	 * @return 楼层
	 */
	Floor selectFloorById(@Param("id") Long id);

	/**
	 * 查询建筑下指定楼层
	 *
	 * @param buildingId 建筑ID
	 * @param floorNo 楼层号
	 * @return 楼层
	 */
	Floor selectFloorByBuildingAndNo(@Param("buildingId") Long buildingId, @Param("floorNo") Integer floorNo);

	/**
	 * 查询建筑楼层面积配置
	 *
	 * @param buildingId 建筑ID
	 * @return 楼层面积集合
	 */
	List<BuildingFloorAreaDTO> selectFloorAreaListByBuildingId(@Param("buildingId") Long buildingId);

	/**
	 * 查询指定楼层房源数量
	 *
	 * @param buildingId 建筑ID
	 * @param floorNo 楼层号
	 * @return 房源数量
	 */
	int countRoomsByFloor(@Param("buildingId") Long buildingId, @Param("floorNo") Integer floorNo);

	/**
	 * 查询楼层房间列表
	 *
	 * @param buildingId 建筑ID
	 * @param floorNo 楼层号
	 * @param roomStatus 房间状态
	 * @return 房间集合
	 */
	List<Room> selectRoomsByFloor(@Param("buildingId") Long buildingId, @Param("floorNo") Integer floorNo, @Param("roomStatus") String roomStatus);

	/**
	 * 汇总指定楼层房间面积
	 *
	 * @param buildingId 建筑ID
	 * @param floorNo 楼层号
	 * @return 房间面积
	 */
	BigDecimal sumRoomAreaByFloor(@Param("buildingId") Long buildingId, @Param("floorNo") Integer floorNo);

}
