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
package org.springblade.modules.ics.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.ics.mapper.BuildingMapper;
import org.springblade.modules.ics.mapper.FloorMapper;
import org.springblade.modules.ics.pojo.entity.Building;
import org.springblade.modules.ics.pojo.entity.Floor;
import org.springblade.modules.ics.service.IFloorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
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
		return baseMapper.selectFloorList(floor);
	}

	@Override
	public Floor selectFloorByBuildingAndNo(Long buildingId, Integer floorNo) {
		return baseMapper.selectFloorByBuildingAndNo(buildingId, floorNo);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncBuildingFloors(Long buildingId, String operator) {
		Building building = buildingMapper.selectById(buildingId);
		if (building == null || building.getFloors() == null || building.getFloors() < 1) {
			return;
		}
		Date now = new Date();
		List<Floor> floorList = list(Wrappers.<Floor>lambdaQuery()
			.eq(Floor::getBuildingId, buildingId)
			.orderByAsc(Floor::getFloorNo));
		Map<Integer, Floor> floorMap = floorList.stream()
			.filter(floor -> floor.getFloorNo() != null)
			.collect(Collectors.toMap(Floor::getFloorNo, item -> item, (left, right) -> left));

		List<Floor> insertList = new ArrayList<>();
		for (int floorNo = 1; floorNo <= building.getFloors(); floorNo++) {
			if (!floorMap.containsKey(floorNo)) {
				Floor floor = new Floor();
				floor.setParkId(building.getParkId());
				floor.setBuildingId(buildingId);
				floor.setFloorNo(floorNo);
				floor.setStatus("0");
				floor.setCreateBy(operator);
				floor.setCreateTime(now);
				insertList.add(floor);
			}
		}
		if (!insertList.isEmpty()) {
			saveBatch(insertList);
		}

		for (Floor floor : floorList) {
			if (floor.getFloorNo() != null && floor.getFloorNo() > building.getFloors()) {
				if (baseMapper.countRoomsByFloor(buildingId, floor.getFloorNo()) > 0) {
					throw new ServiceException("建筑" + floor.getFloorNo() + "层存在房源，不能删除该楼层");
				}
				removeById(floor.getId());
			}
		}
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
			updateFloor.setUpdateBy(operator);
			updateFloor.setUpdateTime(now);
			updateById(updateFloor);
		}
	}

}
