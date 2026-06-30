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
package org.springblade.modules.park.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.vo.RoomVO;

import java.util.Objects;

/**
 * 房源包装类
 *
 * @author Chill
 */
public class RoomWrapper extends BaseEntityWrapper<Room, RoomVO> {

	public static RoomWrapper build() {
		return new RoomWrapper();
	}

	@Override
	public RoomVO entityVO(Room room) {
		RoomVO roomVO = Objects.requireNonNull(BeanUtil.copyProperties(room, RoomVO.class));
		roomVO.setStatusName(statusName(roomVO.getStatus()));
		roomVO.setBaseStatusName(statusName(roomVO.getBaseStatus()));
		roomVO.setSyncStatusName("1".equals(roomVO.getSyncStatus()) ? "已同步" : "待同步");
		return roomVO;
	}

	private String statusName(String status) {
		return switch (status == null ? "0" : status) {
			case "1" -> "待清退/短租";
			case "2" -> "预留";
			case "3" -> "待退出";
			case "4" -> "90天内到期";
			case "5" -> "30天内到期";
			case "6" -> "已到期";
			case "7" -> "已出租";
			default -> "空置";
		};
	}

}
