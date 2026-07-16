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
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.park.mapper.BuildingMapper;
import org.springblade.modules.park.mapper.FloorMapper;
import org.springblade.modules.park.pojo.entity.Building;
import org.springblade.modules.park.pojo.entity.Floor;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.service.IFloorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 楼层服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class FloorServiceImpl extends ServiceImpl<FloorMapper, Floor> implements IFloorService {

	private final BuildingMapper buildingMapper;

	@Override
	public List<Floor> selectFloorList(Floor floor) {
		List<Floor> floors = baseMapper.selectFloorList(floor);
		attachFloorDetail(floors, floor == null ? null : floor.getRoomStatus());
		return floors;
	}

	@Override
	public List<Floor> selectFloorStructureList(Floor floor) {
		return baseMapper.selectFloorStructureList(floor);
	}

	@Override
	public IPage<Floor> selectFloorPage(IPage<Floor> page, Floor floor) {
		page.setRecords(baseMapper.selectFloorPage(page, floor));
		attachFloorDetail(page.getRecords(), floor == null ? null : floor.getRoomStatus());
		return page;
	}

	@Override
	public Floor selectFloorById(Long id, String roomStatus) {
		Floor floor = baseMapper.selectFloorById(id);
		if (floor == null) {
			throw new ServiceException("楼层不存在");
		}
		attachFloorDetail(List.of(floor), roomStatus);
		return floor;
	}

	@Override
	public Floor selectFloorByBuildingAndNo(Long buildingId, Integer floorNo) {
		Floor floor = baseMapper.selectFloorByBuildingAndNo(buildingId, floorNo);
		if (floor != null) {
			fillOccupancyRate(floor);
		}
		return floor;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncBuildingFloors(Long buildingId, String operator) {
		Building building = buildingMapper.selectById(buildingId);
		if (building == null || building.getFloors() == null || building.getFloors() < 1) {
			throw new ServiceException("建筑不存在或未配置有效楼层数");
		}
		Date now = new Date();
		String userName = currentOperator(operator);
		BigDecimal defaultArea = defaultFloorArea(building);
		List<Floor> floorList = list(Wrappers.<Floor>lambdaQuery()
			.eq(Floor::getBuildingId, buildingId)
			.orderByAsc(Floor::getFloorNo)
			.orderByAsc(Floor::getId));
		Map<Integer, Floor> floorMap = new LinkedHashMap<>();
		List<Long> duplicateIds = new ArrayList<>();
		for (Floor floor : floorList) {
			if (floor.getFloorNo() == null) {
				continue;
			}
			if (floorMap.containsKey(floor.getFloorNo())) {
				duplicateIds.add(floor.getId());
			} else {
				floorMap.put(floor.getFloorNo(), floor);
			}
		}
		if (!duplicateIds.isEmpty()) {
			removeByIds(duplicateIds);
		}

		List<Floor> insertList = new ArrayList<>();
		List<Floor> updateList = new ArrayList<>();
		for (int floorNo = 1; floorNo <= building.getFloors(); floorNo++) {
			Floor existing = floorMap.get(floorNo);
			if (existing == null) {
				Floor floor = new Floor();
				floor.setParkId(building.getParkId());
				floor.setBuildingId(buildingId);
				floor.setFloorNo(floorNo);
				floor.setArea(defaultArea);
				floor.setStatus("0");
				floor.setCreateBy(userName);
				floor.setCreateTime(now);
				insertList.add(floor);
			} else if (existing.getArea() == null || StringUtil.isBlank(existing.getStatus())
				|| !Objects.equals(existing.getParkId(), building.getParkId())) {
				Floor updateFloor = new Floor();
				updateFloor.setId(existing.getId());
				updateFloor.setParkId(building.getParkId());
				updateFloor.setArea(existing.getArea() == null ? defaultArea : existing.getArea());
				updateFloor.setStatus(StringUtil.isBlank(existing.getStatus()) ? "0" : existing.getStatus());
				updateFloor.setUpdateBy(userName);
				updateFloor.setUpdateTime(now);
				updateList.add(updateFloor);
			}
		}
		if (!insertList.isEmpty()) {
			saveBatch(insertList);
		}
		if (!updateList.isEmpty()) {
			updateBatchById(updateList);
		}

		for (Floor floor : floorMap.values()) {
			if (floor.getFloorNo() != null && floor.getFloorNo() > building.getFloors()) {
				if (baseMapper.countRoomsByFloor(buildingId, floor.getFloorNo()) == 0) {
					removeById(floor.getId());
				}
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncAllBuildingFloors(String operator) {
		List<Building> buildingList = buildingMapper.selectList(Wrappers.<Building>lambdaQuery()
			.isNotNull(Building::getId)
			.orderByAsc(Building::getId));
		for (Building building : buildingList) {
			if (building.getId() != null && building.getFloors() != null && building.getFloors() > 0) {
				syncBuildingFloors(building.getId(), operator);
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submit(Floor floor) {
		if (floor == null) {
			throw new ServiceException("楼层数据不能为空");
		}
		Floor oldFloor = null;
		if (floor.getId() != null) {
			oldFloor = baseMapper.selectFloorById(floor.getId());
			if (oldFloor == null) {
				throw new ServiceException("楼层不存在");
			}
		}
		validateFloor(floor);
		Date now = new Date();
		String userName = AuthUtil.getUserName();
		if (StringUtil.isBlank(floor.getStatus())) {
			floor.setStatus("0");
		}
		if (floor.getId() == null) {
			floor.setCreateBy(userName);
			floor.setCreateTime(now);
		} else {
			floor.setUpdateBy(userName);
			floor.setUpdateTime(now);
			if (StringUtil.isBlank(floor.getCreateBy())) {
				floor.setCreateBy(oldFloor.getCreateBy());
			}
			if (floor.getCreateTime() == null) {
				floor.setCreateTime(oldFloor.getCreateTime());
			}
		}
		return saveOrUpdate(floor);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeFloor(String ids) {
		List<Long> idList = Func.toLongList(ids);
		if (idList.isEmpty()) {
			throw new ServiceException("请选择需要删除的楼层");
		}
		for (Long id : idList) {
			Floor floor = baseMapper.selectFloorById(id);
			if (floor == null) {
				throw new ServiceException("楼层不存在");
			}
			if (baseMapper.countRoomsByFloor(floor.getBuildingId(), floor.getFloorNo()) > 0) {
				throw new ServiceException("楼层已存在房源，不能删除");
			}
		}
		return removeByIds(idList);
	}

	@Override
	public Map<String, Object> selectFloorStatistics(Floor floor) {
		List<Floor> floors = baseMapper.selectFloorList(floor);
		BigDecimal totalArea = BigDecimal.ZERO;
		BigDecimal usedArea = BigDecimal.ZERO;
		int totalCount = 0;
		int rentedCount = 0;
		int vacantCount = 0;
		int reservedCount = 0;
		int renovatingCount = 0;
		int disabledCount = 0;
		int expiring90Count = 0;
		int expiring30Count = 0;
		int expiredCount = 0;
		for (Floor item : floors) {
			totalArea = totalArea.add(zeroIfNull(item.getArea()));
			usedArea = usedArea.add(zeroIfNull(item.getUsedArea()));
			totalCount += zeroIfNull(item.getTotalCount());
			rentedCount += zeroIfNull(item.getRentedCount());
			vacantCount += zeroIfNull(item.getVacantCount());
			reservedCount += zeroIfNull(item.getReservedCount());
			renovatingCount += zeroIfNull(item.getRenovatingCount());
			disabledCount += zeroIfNull(item.getDisabledCount());
			expiring90Count += zeroIfNull(item.getExpiring90Count());
			expiring30Count += zeroIfNull(item.getExpiring30Count());
			expiredCount += zeroIfNull(item.getExpiredCount());
		}
		Map<String, Object> statistics = new LinkedHashMap<>();
		statistics.put("floorCount", floors.size());
		statistics.put("totalArea", totalArea);
		statistics.put("usedArea", usedArea);
		statistics.put("totalCount", totalCount);
		statistics.put("rentedCount", rentedCount);
		statistics.put("vacantCount", vacantCount);
		statistics.put("reservedCount", reservedCount);
		statistics.put("renovatingCount", renovatingCount);
		statistics.put("disabledCount", disabledCount);
		statistics.put("expiring90Count", expiring90Count);
		statistics.put("expiring30Count", expiring30Count);
		statistics.put("expiredCount", expiredCount);
		int occupiedCount = rentedCount + renovatingCount + disabledCount + expiring90Count + expiring30Count + expiredCount;
		statistics.put("occupancyRate", totalCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(occupiedCount)
			.multiply(BigDecimal.valueOf(100))
			.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP));
		return statistics;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateBuildingFloorAreas(Long buildingId, Map<Integer, BigDecimal> floorAreaMap, String operator) {
		if (floorAreaMap == null || floorAreaMap.isEmpty()) {
			return;
		}
		Date now = new Date();
		for (Map.Entry<Integer, BigDecimal> entry : floorAreaMap.entrySet()) {
			Integer floorNo = entry.getKey();
			BigDecimal area = entry.getValue();
			if (floorNo == null || area == null) {
				continue;
			}
			Floor floor = selectFloorByBuildingAndNo(buildingId, floorNo);
			if (floor == null) {
				continue;
			}
			BigDecimal usedArea = Objects.requireNonNullElse(floor.getUsedArea(), BigDecimal.ZERO);
			if (area.compareTo(usedArea) < 0) {
				throw new ServiceException(floorNo + "F楼层面积不能小于已配置房源面积：" + usedArea.stripTrailingZeros().toPlainString() + "㎡");
			}
			Floor updateFloor = new Floor();
			updateFloor.setId(floor.getId());
			updateFloor.setArea(area);
			updateFloor.setUpdateBy(currentOperator(operator));
			updateFloor.setUpdateTime(now);
			updateById(updateFloor);
		}
	}

	private void validateFloor(Floor floor) {
		if (floor.getBuildingId() == null) {
			throw new ServiceException("请选择所属建筑");
		}
		Building building = buildingMapper.selectById(floor.getBuildingId());
		if (building == null || building.getParkId() == null) {
			throw new ServiceException("所属建筑不存在");
		}
		if (floor.getFloorNo() == null) {
			throw new ServiceException("请输入楼层号");
		}
		if (building.getFloors() == null || building.getFloors() < 1) {
			throw new ServiceException("所属建筑未配置有效楼层数");
		}
		if (floor.getFloorNo() < 1 || floor.getFloorNo() > building.getFloors()) {
			throw new ServiceException("楼层号必须在1到" + building.getFloors() + "层之间");
		}
		if (floor.getArea() != null && floor.getArea().compareTo(BigDecimal.ZERO) < 0) {
			throw new ServiceException("楼层面积不能小于0");
		}
		floor.setParkId(building.getParkId());
		Floor sameFloor = baseMapper.selectFloorByBuildingAndNo(floor.getBuildingId(), floor.getFloorNo());
		if (sameFloor != null && !Objects.equals(sameFloor.getId(), floor.getId())) {
			throw new ServiceException("该建筑下楼层号已存在");
		}
		BigDecimal usedArea = baseMapper.sumRoomAreaByFloor(floor.getBuildingId(), floor.getFloorNo());
		if (floor.getArea() != null && floor.getArea().compareTo(zeroIfNull(usedArea)) < 0) {
			throw new ServiceException("楼层面积不能小于已配置房源面积：" + usedArea.stripTrailingZeros().toPlainString() + "㎡");
		}
	}

	private void attachFloorDetail(List<Floor> floors, String roomStatus) {
		if (floors == null || floors.isEmpty()) {
			return;
		}
		List<Floor> queryFloors = floors.stream()
			.filter(floor -> floor.getBuildingId() != null && floor.getFloorNo() != null)
			.toList();
		Map<String, List<Room>> roomsByFloor = queryFloors.isEmpty()
			? Map.of()
			: baseMapper.selectRoomsByFloors(queryFloors, roomStatus).stream()
			.collect(Collectors.groupingBy(room -> floorKey(room.getBuildingId(), room.getFloor()), LinkedHashMap::new, Collectors.toList()));
		for (Floor floor : floors) {
			fillOccupancyRate(floor);
			if (floor.getBuildingId() != null && floor.getFloorNo() != null) {
				floor.setRooms(roomsByFloor.getOrDefault(floorKey(floor.getBuildingId(), floor.getFloorNo()), List.of()));
			}
		}
	}

	private String floorKey(Long buildingId, Integer floorNo) {
		return buildingId + ":" + floorNo;
	}

	private void fillOccupancyRate(Floor floor) {
		int totalCount = zeroIfNull(floor.getTotalCount());
		if (totalCount == 0) {
			floor.setOccupancyRate(BigDecimal.ZERO);
			return;
		}
		int occupiedCount = zeroIfNull(floor.getRentedCount())
			+ zeroIfNull(floor.getRenovatingCount())
			+ zeroIfNull(floor.getDisabledCount())
			+ zeroIfNull(floor.getExpiring90Count())
			+ zeroIfNull(floor.getExpiring30Count())
			+ zeroIfNull(floor.getExpiredCount());
		floor.setOccupancyRate(BigDecimal.valueOf(occupiedCount)
			.multiply(BigDecimal.valueOf(100))
			.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP));
	}

	private BigDecimal defaultFloorArea(Building building) {
		if (building.getArea() == null || building.getFloors() == null || building.getFloors() < 1) {
			return null;
		}
		return building.getArea().divide(BigDecimal.valueOf(building.getFloors()), 2, RoundingMode.HALF_UP);
	}

	private String currentOperator(String operator) {
		return StringUtil.isBlank(operator) ? AuthUtil.getUserName() : operator;
	}

	private BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private int zeroIfNull(Integer value) {
		return value == null ? 0 : value;
	}

}
