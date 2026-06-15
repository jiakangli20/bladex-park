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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.ics.excel.BuildingExcel;
import org.springblade.modules.ics.mapper.BuildingMapper;
import org.springblade.modules.ics.mapper.FloorMapper;
import org.springblade.modules.ics.pojo.dto.BuildingDTO;
import org.springblade.modules.ics.pojo.dto.BuildingFloorAreaDTO;
import org.springblade.modules.ics.pojo.entity.Building;
import org.springblade.modules.ics.pojo.vo.BuildingVO;
import org.springblade.modules.ics.service.IBuildingService;
import org.springblade.modules.ics.service.IFloorService;
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.service.IParkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 建筑服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements IBuildingService {

	private final IParkService parkService;
	private final IFloorService floorService;
	private final FloorMapper floorMapper;

	@Override
	public List<Building> selectBuildingList(Building building) {
		return baseMapper.selectBuildingList(building);
	}

	@Override
	public IPage<BuildingVO> selectBuildingPage(IPage<BuildingVO> page, Building building) {
		page.setRecords(baseMapper.selectBuildingPage(page, building));
		return page;
	}

	@Override
	public BuildingVO selectBuildingById(Long id) {
		BuildingVO building = baseMapper.selectBuildingById(id);
		if (building != null) {
			building.setFloorAreas(floorMapper.selectFloorAreaListByBuildingId(id));
		}
		return building;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submit(BuildingDTO buildingDTO) {
		validateBuilding(buildingDTO);
		Date now = new Date();
		String userName = AuthUtil.getUserName();
		Building building = BeanUtil.copyProperties(buildingDTO, Building.class);
		if (building == null) {
			throw new ServiceException("建筑数据转换失败");
		}
		if (building.getId() == null) {
			building.setCreateBy(userName);
			building.setCreateTime(now);
		} else {
			BuildingVO oldBuilding = selectBuildingById(building.getId());
			if (oldBuilding == null) {
				throw new ServiceException("建筑不存在");
			}
			building.setUpdateBy(userName);
			building.setUpdateTime(now);
		}
		boolean success = saveOrUpdate(building);
		if (!success) {
			return false;
		}
		floorService.syncBuildingFloors(building.getId(), userName);
		applyFloorAreas(building.getId(), buildingDTO.getFloorAreas(), userName);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeBuilding(String ids) {
		List<Long> idList = Func.toLongList(ids);
		if (idList.isEmpty()) {
			throw new ServiceException("请选择需要删除的建筑");
		}
		for (Long buildingId : idList) {
			BuildingVO building = selectBuildingById(buildingId);
			if (building == null) {
				throw new ServiceException("建筑不存在");
			}
			validateRemoveBuilding(buildingId);
		}
		return removeByIds(idList);
	}

	@Override
	public List<BuildingExcel> exportBuilding(Building building) {
		return baseMapper.selectBuildingList(building).stream().map(item -> {
			BuildingExcel excel = Objects.requireNonNull(BeanUtil.copyProperties(item, BuildingExcel.class));
			excel.setParkName(item.getParkName());
			return excel;
		}).collect(Collectors.toList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void importBuilding(List<BuildingExcel> data) {
		if (Func.isEmpty(data)) {
			return;
		}
		Map<String, Long> parkMap = parkService.list().stream()
			.collect(Collectors.toMap(Park::getName, Park::getId, (left, right) -> left, LinkedHashMap::new));
		List<BuildingDTO> buildingList = new ArrayList<>();
		for (BuildingExcel excel : data) {
			Long parkId = parkMap.get(excel.getParkName());
			if (parkId == null) {
				throw new ServiceException("导入失败，建筑【" + excel.getName() + "】所属园区不存在");
			}
			BuildingDTO dto = BeanUtil.copyProperties(excel, BuildingDTO.class);
			dto.setParkId(parkId);
			buildingList.add(dto);
		}
		for (BuildingDTO dto : buildingList) {
			submit(dto);
		}
	}

	private void validateBuilding(Building building) {
		if (building.getParkId() == null) {
			throw new ServiceException("请选择所属园区");
		}
		Park park = parkService.getById(building.getParkId());
		if (park == null) {
			throw new ServiceException("所属园区不存在");
		}
		if (StringUtil.isBlank(building.getCode())) {
			throw new ServiceException("请输入建筑编码");
		}
		if (StringUtil.isBlank(building.getName())) {
			throw new ServiceException("请输入建筑名称");
		}
		if (StringUtil.isBlank(building.getRegion())) {
			throw new ServiceException("请输入所属地区");
		}
		if (StringUtil.isBlank(building.getBuildingType())) {
			throw new ServiceException("请选择产权性质");
		}
		if (building.getSortNum() == null) {
			throw new ServiceException("请输入排序值");
		}
		if (building.getFloors() == null || building.getFloors() < 1) {
			throw new ServiceException("请输入有效楼层数");
		}
		if (building.getArea() == null || building.getArea().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("请输入建筑面积");
		}
		if (building.getPropertyArea() == null || building.getPropertyArea().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("请输入产权面积");
		}
		Building sameName = baseMapper.selectBuildingByParkAndName(building.getParkId(), building.getName());
		if (sameName != null && !Objects.equals(sameName.getId(), building.getId())) {
			throw new ServiceException("同一园区下建筑名称已存在");
		}
		if (building.getId() != null) {
			Integer maxRoomFloor = baseMapper.selectMaxRoomFloor(building.getId());
			if (maxRoomFloor != null && maxRoomFloor > building.getFloors()) {
				throw new ServiceException("已有" + maxRoomFloor + "层房源，建筑楼层数不能小于" + maxRoomFloor);
			}
		}
	}

	private void applyFloorAreas(Long buildingId, List<BuildingFloorAreaDTO> floorAreas, String operator) {
		if (Func.isEmpty(floorAreas)) {
			return;
		}
		Map<Integer, BigDecimal> floorAreaMap = floorAreas.stream()
			.filter(item -> item != null && item.getFloorNo() != null && item.getArea() != null)
			.collect(Collectors.toMap(BuildingFloorAreaDTO::getFloorNo, BuildingFloorAreaDTO::getArea, (left, right) -> right, LinkedHashMap::new));
		floorService.updateBuildingFloorAreas(buildingId, floorAreaMap, operator);
	}

	private void validateRemoveBuilding(Long buildingId) {
		if (baseMapper.countFloorsByBuildingId(buildingId) > 0) {
			throw new ServiceException("建筑下存在楼层，不能删除");
		}
		if (baseMapper.countRoomsByBuildingId(buildingId) > 0) {
			throw new ServiceException("建筑下存在房源，不能删除");
		}
		if (baseMapper.countTable("biz_contract") == 1 && baseMapper.countContractRefs(buildingId) > 0) {
			throw new ServiceException("建筑已被合同引用，不能删除");
		}
	}

}
