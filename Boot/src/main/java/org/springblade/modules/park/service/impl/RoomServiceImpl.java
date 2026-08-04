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
package org.springblade.modules.park.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.park.mapper.RoomMapper;
import org.springblade.modules.park.pojo.entity.Building;
import org.springblade.modules.park.pojo.entity.Floor;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.vo.RoomVO;
import org.springblade.modules.park.service.IBuildingService;
import org.springblade.modules.park.service.IFloorService;
import org.springblade.modules.park.service.IRoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 房源服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements IRoomService {

	private static final String STATUS_VACANT = "0";
	private static final String SYNC_PENDING = "0";
	private static final BigDecimal AREA_COMPARE_TOLERANCE = new BigDecimal("0.01");

	private final IBuildingService buildingService;
	private final IFloorService floorService;

	@Override
	public RoomVO selectRoomById(Long id) {
		RoomVO room = baseMapper.selectRoomById(id);
		return room;
	}

	@Override
	public List<RoomVO> selectRoomList(Room room) {
		return baseMapper.selectRoomList(room);
	}

	@Override
	public IPage<RoomVO> selectRoomPage(IPage<RoomVO> page, Room room) {
		return page.setRecords(baseMapper.selectRoomPage(page, room));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submit(Room room) {
		RoomVO oldRoom = null;
		if (room != null && room.getId() != null) {
			oldRoom = selectRoomById(room.getId());
			if (oldRoom == null) {
				throw new ServiceException("房源不存在");
			}
			// 房源主键可能已被合同、账单和工单引用，编辑时保持原物理位置不变。
			room.setParkId(oldRoom.getParkId());
			room.setBuildingId(oldRoom.getBuildingId());
			room.setFloor(oldRoom.getFloor());
		}
		validateRoom(room);
		Date now = new Date();
		String userName = AuthUtil.getUserName();
		if (Func.isEmpty(room.getStatus())) {
			room.setStatus(STATUS_VACANT);
		}
		room.setSyncStatus(SYNC_PENDING);
		if (room.getId() == null) {
			room.setVacantSince(STATUS_VACANT.equals(room.getStatus()) ? now : null);
			room.setCreateBy(userName);
			room.setCreateTime(now);
		} else {
			if (STATUS_VACANT.equals(room.getStatus())) {
				room.setVacantSince(STATUS_VACANT.equals(oldRoom.getBaseStatus()) ? oldRoom.getVacantSince() : now);
			} else {
				room.setVacantSince(null);
			}
			room.setUpdateBy(userName);
			room.setUpdateTime(now);
		}
		return saveOrUpdate(room);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeRoom(String ids) {
		List<Long> idList = Func.toLongList(ids);
		if (idList.isEmpty()) {
			throw new ServiceException("请选择需要删除的房源");
		}
		for (Long id : idList) {
			RoomVO room = selectRoomById(id);
			if (room == null) {
				throw new ServiceException("房源不存在");
			}
			validateReferenceBeforeRemove(id);
		}
		return removeByIds(idList);
	}

	@Override
	public boolean changeStatus(Long id, String status) {
		RoomVO room = selectRoomById(id);
		if (room == null) {
			throw new ServiceException("房源不存在");
		}
		if (!List.of("0", "1", "2", "3", "4", "5", "6", "7").contains(status)) {
			throw new ServiceException("房源状态不正确");
		}
		boolean resetVacantSince = STATUS_VACANT.equals(status) && !STATUS_VACANT.equals(room.getBaseStatus());
		return baseMapper.updateRoomStatus(id, status, AuthUtil.getUserName(), resetVacantSince) > 0;
	}

	@Override
	public boolean syncMini(Long id) {
		RoomVO room = selectRoomById(id);
		if (room == null) {
			throw new ServiceException("房源不存在");
		}
		return baseMapper.markSyncMini(id, AuthUtil.getUserName()) > 0;
	}

	private void validateRoom(Room room) {
		if (room.getBuildingId() == null) {
			throw new ServiceException("请选择所属建筑");
		}
		Building building = buildingService.selectBuildingById(room.getBuildingId());
		if (building == null || building.getParkId() == null) {
			throw new ServiceException("所属建筑不存在");
		}
		room.setParkId(building.getParkId());
		if (StringUtil.isBlank(room.getName())) {
			throw new ServiceException("请输入房间名称");
		}
		room.setName(room.getName().trim());
		if (room.getFloor() == null) {
			throw new ServiceException("请输入楼层");
		}
		Integer maxFloors = building.getFloors();
		if (maxFloors == null || maxFloors < 1) {
			throw new ServiceException("所属建筑未配置有效楼层数");
		}
		if (room.getFloor() < 1 || room.getFloor() > maxFloors) {
			throw new ServiceException("房间楼层必须在1到" + maxFloors + "层之间");
		}
		if (room.getArea() == null || room.getArea().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("房源面积必须大于0");
		}
		if (StringUtil.isNotBlank(room.getStatus())
			&& !List.of("0", "1", "2", "3", "4", "5", "6", "7").contains(room.getStatus())) {
			throw new ServiceException("房源状态不正确");
		}
		long duplicateCount = count(Wrappers.<Room>lambdaQuery()
			.eq(Room::getBuildingId, room.getBuildingId())
			.eq(Room::getName, room.getName())
			.ne(room.getId() != null, Room::getId, room.getId()));
		if (duplicateCount > 0) {
			throw new ServiceException("同一建筑下房间名称已存在");
		}
		validateRoomArea(room);
	}

	private void validateRoomArea(Room room) {
		Floor floor = floorService.selectFloorByBuildingAndNo(room.getBuildingId(), room.getFloor());
		if (floor == null) {
			throw new ServiceException("所属楼层不存在，请先同步建筑楼层");
		}
		if (floor.getArea() == null || floor.getArea().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("所属楼层未配置有效面积");
		}
		BigDecimal usedArea = baseMapper.sumRoomAreaByFloor(room.getBuildingId(), room.getFloor(), room.getId());
		BigDecimal remainingArea = floor.getArea().subtract(usedArea == null ? BigDecimal.ZERO : usedArea);
		if (room.getArea().compareTo(remainingArea.add(AREA_COMPARE_TOLERANCE)) > 0) {
			throw new ServiceException("房源面积不能超过当前楼层剩余面积：" + remainingArea.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString() + "㎡");
		}
	}

	private void validateReferenceBeforeRemove(Long roomId) {
		if (tableExists("biz_contract") && baseMapper.countContractRefs(roomId) > 0) {
			throw new ServiceException("房源已被合同引用，不能删除");
		}
		if (tableExists("biz_service_workorder") && baseMapper.countWorkorderRefs(roomId) > 0) {
			throw new ServiceException("房源已被工单引用，不能删除");
		}
	}

	private boolean tableExists(String tableName) {
		return Objects.equals(baseMapper.countTable(tableName), 1);
	}

}
