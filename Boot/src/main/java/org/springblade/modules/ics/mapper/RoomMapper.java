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
import org.springblade.modules.ics.pojo.entity.Room;
import org.springblade.modules.ics.pojo.vo.RoomVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房源 Mapper 接口
 *
 * @author Chill
 */
public interface RoomMapper extends BaseMapper<Room> {

	/**
	 * 查询房源详情
	 *
	 * @param id 房源ID
	 * @return 房源
	 */
	RoomVO selectRoomById(@Param("id") Long id);

	/**
	 * 查询房源列表
	 *
	 * @param room 查询条件
	 * @return 房源集合
	 */
	List<RoomVO> selectRoomList(@Param("room") Room room);

	/**
	 * 查询房源分页
	 *
	 * @param page 分页
	 * @param room 查询条件
	 * @return 房源集合
	 */
	List<RoomVO> selectRoomPage(IPage page, @Param("room") Room room);

	/**
	 * 汇总指定楼层房源面积
	 *
	 * @param buildingId 建筑ID
	 * @param floor 楼层
	 * @param excludeId 排除房源ID
	 * @return 已用面积
	 */
	BigDecimal sumRoomAreaByFloor(@Param("buildingId") Long buildingId, @Param("floor") Integer floor, @Param("excludeId") Long excludeId);

	/**
	 * 房源状态流转
	 *
	 * @param id 房源ID
	 * @param status 状态
	 * @param updateBy 更新人
	 * @return 更新数量
	 */
	int updateRoomStatus(@Param("id") Long id, @Param("status") String status, @Param("updateBy") String updateBy);

	/**
	 * 标记小程序同步
	 *
	 * @param id 房源ID
	 * @param updateBy 更新人
	 * @return 更新数量
	 */
	int markSyncMini(@Param("id") Long id, @Param("updateBy") String updateBy);

	/**
	 * 判断表是否存在
	 *
	 * @param tableName 表名
	 * @return 数量
	 */
	int countTable(@Param("tableName") String tableName);

	/**
	 * 查询合同引用数量
	 *
	 * @param roomId 房源ID
	 * @return 引用数量
	 */
	int countContractRefs(@Param("roomId") Long roomId);

	/**
	 * 查询工单引用数量
	 *
	 * @param roomId 房源ID
	 * @return 引用数量
	 */
	int countWorkorderRefs(@Param("roomId") Long roomId);

}
