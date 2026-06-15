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
package org.springblade.modules.ics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.ics.pojo.entity.Room;
import org.springblade.modules.ics.pojo.vo.RoomVO;

import java.util.List;

/**
 * 房源服务类
 *
 * @author Chill
 */
public interface IRoomService extends IService<Room> {

	/**
	 * 查询房源详情
	 *
	 * @param id 房源ID
	 * @return 房源详情
	 */
	RoomVO selectRoomById(Long id);

	/**
	 * 查询房源列表
	 *
	 * @param room 查询条件
	 * @return 房源集合
	 */
	List<RoomVO> selectRoomList(Room room);

	/**
	 * 查询房源分页
	 *
	 * @param page 分页
	 * @param room 查询条件
	 * @return 分页
	 */
	IPage<RoomVO> selectRoomPage(IPage<RoomVO> page, Room room);

	/**
	 * 新增或修改房源
	 *
	 * @param room 房源
	 * @return 是否成功
	 */
	boolean submit(Room room);

	/**
	 * 删除房源
	 *
	 * @param ids 主键集合
	 * @return 是否成功
	 */
	boolean removeRoom(String ids);

	/**
	 * 状态流转
	 *
	 * @param id 房源ID
	 * @param status 状态
	 * @return 是否成功
	 */
	boolean changeStatus(Long id, String status);

	/**
	 * 标记同步小程序
	 *
	 * @param id 房源ID
	 * @return 是否成功
	 */
	boolean syncMini(Long id);

}
