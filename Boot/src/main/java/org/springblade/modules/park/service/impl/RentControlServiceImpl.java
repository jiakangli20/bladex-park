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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.park.pojo.entity.Building;
import org.springblade.modules.park.pojo.entity.Floor;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.vo.RoomVO;
import org.springblade.modules.park.service.IBuildingService;
import org.springblade.modules.park.service.IFloorService;
import org.springblade.modules.park.service.IRentControlService;
import org.springblade.modules.park.service.IRoomService;
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.service.IParkService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 租控管理服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class RentControlServiceImpl implements IRentControlService {

	private final IParkService parkService;
	private final IBuildingService buildingService;
	private final IFloorService floorService;
	private final IRoomService roomService;

	@Override
	public Map<String, Object> getBoard(Long parkId, Long buildingId, Integer floorNo, String keyword, String searchType, String status, String orientation) {
		Building buildingQuery = new Building();
		buildingQuery.setParkId(parkId);
		if ("building".equals(searchType) && hasText(keyword)) {
			buildingQuery.setName(keyword);
		}
		List<Building> buildingList = buildingService.selectBuildingList(buildingQuery);
		List<Park> parkList = parkService.list(Wrappers.<Park>lambdaQuery()
			.eq(parkId != null, Park::getId, parkId)
			.orderByAsc(Park::getId));

		Long selectedBuildingId = normalizeSelectedBuildingId(buildingList, buildingId);
		Building currentBuilding = selectedBuildingId == null ? null : findBuilding(buildingList, selectedBuildingId);
		Long currentParkId = parkId != null ? parkId : (currentBuilding == null ? null : currentBuilding.getParkId());
		Park currentPark = currentParkId == null ? null : parkService.getById(currentParkId);

		Floor floorQuery = new Floor();
		floorQuery.setParkId(parkId);
		floorQuery.setBuildingId(selectedBuildingId);
		floorQuery.setFloorNo(floorNo);
		List<Floor> floorList = floorService.selectFloorList(floorQuery);

		Floor allFloorQuery = new Floor();
		allFloorQuery.setParkId(parkId);
		List<Floor> allFloorList = floorService.selectFloorList(allFloorQuery);

		Room allRoomQuery = new Room();
		allRoomQuery.setParkId(parkId);
		List<RoomVO> allRoomList = roomService.selectRoomList(allRoomQuery);

		Room roomQuery = new Room();
		roomQuery.setParkId(parkId);
		roomQuery.setBuildingId(selectedBuildingId);
		roomQuery.setFloor(floorNo);
		roomQuery.setStatus(status);
		roomQuery.setOrientation(orientation);
		if (!"building".equals(searchType) && hasText(keyword)) {
			roomQuery.setName(keyword);
		}
		List<RoomVO> roomList = roomService.selectRoomList(roomQuery);
		if ("building".equals(searchType)) {
			List<Long> buildingIds = buildingList.stream().map(Building::getId).filter(Objects::nonNull).toList();
			roomList = roomList.stream()
				.filter(room -> room.getBuildingId() != null && buildingIds.contains(room.getBuildingId()))
				.toList();
			floorList = floorList.stream()
				.filter(floor -> floor.getBuildingId() != null && buildingIds.contains(floor.getBuildingId()))
				.toList();
		}

		Map<String, Object> result = new LinkedHashMap<>();
		List<Map<String, Object>> buildingTree = buildBuildingTree(buildingList, allRoomList, allFloorList);
		result.put("parkName", currentPark == null ? null : currentPark.getName());
		result.put("currentPark", currentPark);
		result.put("currentBuilding", currentBuilding);
		result.put("parks", buildParkTree(parkList, buildingTree));
		result.put("buildings", buildingTree);
		result.put("overview", buildOverview(floorList, roomList));
		result.put("analysis", buildAnalysis(roomList));
		result.put("floors", buildFloorRows(selectedBuildingId, floorList, roomList, buildingList));
		result.put("summary", buildStatusSummary(roomList));
		return result;
	}

	@Override
	public Map<String, Object> workorders() {
		Map<String, Object> result = new LinkedHashMap<>();
		Map<String, Object> stats = new LinkedHashMap<>();
		stats.put("pendingAssign", 0);
		stats.put("overdueOpen", 0);
		stats.put("processing", 0);
		stats.put("monthOverdueRate", 0);
		stats.put("monthOverdue", 0);
		stats.put("monthSatisfaction", 0);
		stats.put("monthRated", 0);
		result.put("records", new ArrayList<>());
		result.put("stats", stats);
		result.put("message", "工单模块暂未迁移，当前为租控入口占位");
		return result;
	}

	@Override
	public Map<String, Object> reportWorkorder() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("success", false);
		result.put("message", "工单模块暂未迁移，暂不支持上报工单");
		return result;
	}

	private Long normalizeSelectedBuildingId(List<Building> buildingList, Long buildingId) {
		if (buildingId == null) {
			return null;
		}
		return buildingList.stream().anyMatch(building -> Objects.equals(building.getId(), buildingId)) ? buildingId : null;
	}

	private Building findBuilding(List<Building> buildingList, Long buildingId) {
		return buildingList.stream()
			.filter(building -> Objects.equals(building.getId(), buildingId))
			.findFirst()
			.orElse(null);
	}

	private List<Map<String, Object>> buildParkTree(List<Park> parkList, List<Map<String, Object>> buildingTree) {
		Map<Long, List<Map<String, Object>>> buildingsByPark = buildingTree.stream()
			.filter(building -> building.get("parkId") != null)
			.collect(Collectors.groupingBy(building -> (Long) building.get("parkId"), LinkedHashMap::new, Collectors.toList()));
		List<Map<String, Object>> result = new ArrayList<>();
		for (Park park : parkList) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", park.getId());
			item.put("name", park.getName());
			item.put("code", park.getCode());
			item.put("buildings", buildingsByPark.getOrDefault(park.getId(), new ArrayList<>()));
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildBuildingTree(List<Building> buildingList, List<RoomVO> allRoomList, List<Floor> allFloorList) {
		Map<Long, List<RoomVO>> roomsByBuilding = allRoomList.stream()
			.filter(room -> room.getBuildingId() != null)
			.collect(Collectors.groupingBy(Room::getBuildingId));
		Map<Long, List<Floor>> floorsByBuilding = allFloorList.stream()
			.filter(floor -> floor.getBuildingId() != null)
			.collect(Collectors.groupingBy(Floor::getBuildingId));
		List<Map<String, Object>> result = new ArrayList<>();
		for (Building building : buildingList) {
			List<Integer> floors = new ArrayList<>();
			for (Floor floor : floorsByBuilding.getOrDefault(building.getId(), new ArrayList<>())) {
				if (floor.getFloorNo() != null && !floors.contains(floor.getFloorNo())) {
					floors.add(floor.getFloorNo());
				}
			}
			for (RoomVO room : roomsByBuilding.getOrDefault(building.getId(), new ArrayList<>())) {
				if (room.getFloor() != null && !floors.contains(room.getFloor())) {
					floors.add(room.getFloor());
				}
			}
			if (building.getFloors() != null && building.getFloors() > 0) {
				for (int floor = building.getFloors(); floor >= 1; floor--) {
					if (!floors.contains(floor)) {
						floors.add(floor);
					}
				}
			}
			floors.sort(Comparator.reverseOrder());

			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", building.getId());
			item.put("parkId", building.getParkId());
			item.put("parkName", building.getParkName());
			item.put("name", building.getName());
			item.put("code", building.getCode());
			item.put("floors", floors);
			item.put("roomCount", roomsByBuilding.getOrDefault(building.getId(), new ArrayList<>()).size());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildFloorRows(Long selectedBuildingId, List<Floor> floorList, List<RoomVO> roomList, List<Building> buildingList) {
		Map<Long, Building> buildingMap = buildingList.stream()
			.filter(building -> building.getId() != null)
			.collect(Collectors.toMap(Building::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
		Map<String, List<RoomVO>> roomGroup = roomList.stream()
			.collect(Collectors.groupingBy(room -> buildFloorKey(selectedBuildingId, room.getBuildingId(), room.getFloor()), LinkedHashMap::new, Collectors.toList()));
		Map<String, Floor> floorMap = floorList.stream()
			.collect(Collectors.toMap(floor -> buildFloorKey(selectedBuildingId, floor.getBuildingId(), floor.getFloorNo()), item -> item, (left, right) -> left, LinkedHashMap::new));
		List<String> floorKeys = new ArrayList<>(floorMap.keySet());
		for (String roomKey : roomGroup.keySet()) {
			if (!floorKeys.contains(roomKey)) {
				floorKeys.add(roomKey);
			}
		}
		floorKeys.sort((left, right) -> compareFloorKey(left, right, selectedBuildingId == null));

		List<Map<String, Object>> floors = new ArrayList<>();
		for (String floorKey : floorKeys) {
			Floor floorInfo = floorMap.get(floorKey);
			List<RoomVO> rooms = roomGroup.getOrDefault(floorKey, new ArrayList<>());
			RoomVO firstRoom = rooms.isEmpty() ? null : rooms.get(0);
			Long rowBuildingId = selectedBuildingId != null ? selectedBuildingId : firstNonNull(firstRoom == null ? null : firstRoom.getBuildingId(), floorInfo == null ? null : floorInfo.getBuildingId());
			Integer rowFloorNo = firstNonNull(firstRoom == null ? null : firstRoom.getFloor(), floorInfo == null ? null : floorInfo.getFloorNo(), 0);
			Building building = buildingMap.get(rowBuildingId);
			BigDecimal usedArea = rooms.stream()
				.map(Room::getArea)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal floorArea = floorInfo != null && floorInfo.getArea() != null ? floorInfo.getArea() : usedArea;
			BigDecimal totalArea = floorArea.compareTo(usedArea) >= 0 ? floorArea : usedArea;

			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", floorInfo == null ? null : floorInfo.getId());
			item.put("buildingId", rowBuildingId);
			item.put("buildingName", building == null ? null : building.getName());
			item.put("floor", rowFloorNo);
			item.put("totalRooms", rooms.size());
			item.put("vacantRooms", rooms.stream().filter(room -> "0".equals(room.getStatus())).count());
			item.put("rentedRooms", rooms.stream().filter(room -> "1".equals(room.getStatus())).count());
			item.put("reservedRooms", rooms.stream().filter(room -> "2".equals(room.getStatus())).count());
			item.put("renovatingRooms", rooms.stream().filter(room -> "3".equals(room.getStatus())).count());
			item.put("disabledRooms", rooms.stream().filter(room -> "4".equals(room.getStatus())).count());
			item.put("totalArea", totalArea);
			item.put("usedArea", usedArea);
			item.put("rooms", rooms);
			floors.add(item);
		}
		return floors;
	}

	private Map<String, Object> buildOverview(List<Floor> floorList, List<RoomVO> roomList) {
		BigDecimal floorArea = floorList.stream()
			.map(Floor::getArea)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal rentedArea = roomList.stream()
			.filter(room -> "1".equals(room.getStatus()))
			.map(Room::getArea)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal vacantArea = roomList.stream()
			.filter(room -> "0".equals(room.getStatus()))
			.map(Room::getArea)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal rentTotal = roomList.stream()
			.map(Room::getRentPrice)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal avgRentPrice = roomList.isEmpty() ? BigDecimal.ZERO : rentTotal.divide(BigDecimal.valueOf(roomList.size()), 2, RoundingMode.HALF_UP);
		long rentedCount = roomList.stream().filter(room -> "1".equals(room.getStatus())).count();

		Map<String, Object> overview = new LinkedHashMap<>();
		overview.put("rentedRoomCount", rentedCount);
		overview.put("totalRoomCount", roomList.size());
		overview.put("managementArea", floorArea);
		overview.put("buildingArea", floorArea);
		overview.put("floorArea", floorArea);
		overview.put("avgRentPrice", avgRentPrice);
		overview.put("rentedArea", rentedArea);
		overview.put("vacantArea", vacantArea);
		overview.put("vacantRoomCount", roomList.stream().filter(room -> "0".equals(room.getStatus())).count());
		overview.put("rentRate", roomList.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(rentedCount * 100).divide(BigDecimal.valueOf(roomList.size()), 0, RoundingMode.HALF_UP));
		return overview;
	}

	private Map<String, Object> buildAnalysis(List<RoomVO> roomList) {
		List<Map<String, Object>> rentAreaRanking = roomList.stream()
			.filter(room -> room.getArea() != null)
			.sorted((left, right) -> right.getArea().compareTo(left.getArea()))
			.limit(5)
			.map(room -> {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("name", room.getName());
				item.put("area", room.getArea());
				item.put("resource", Func.toStr(room.getBuildingName()) + "-" + Func.toStr(room.getName()));
				return item;
			})
			.toList();
		List<Map<String, Object>> rentUnitPriceRanking = roomList.stream()
			.filter(room -> room.getRentPrice() != null)
			.sorted((left, right) -> right.getRentPrice().compareTo(left.getRentPrice()))
			.limit(5)
			.map(room -> {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("name", room.getName());
				item.put("rentPrice", room.getRentPrice());
				item.put("area", room.getArea());
				item.put("resource", Func.toStr(room.getBuildingName()) + "-" + Func.toStr(room.getName()));
				return item;
			})
			.toList();

		Map<String, Object> analysis = new LinkedHashMap<>();
		analysis.put("industry", new ArrayList<>());
		analysis.put("tenantTags", new ArrayList<>());
		analysis.put("rentAreaRanking", rentAreaRanking);
		analysis.put("rentUnitPriceRanking", rentUnitPriceRanking);
		return analysis;
	}

	private Map<String, Long> buildStatusSummary(List<RoomVO> roomList) {
		Map<String, Long> summary = new HashMap<>();
		summary.put("vacant", roomList.stream().filter(room -> "0".equals(room.getStatus())).count());
		summary.put("rented", roomList.stream().filter(room -> "1".equals(room.getStatus())).count());
		summary.put("reserved", roomList.stream().filter(room -> "2".equals(room.getStatus())).count());
		summary.put("renovating", roomList.stream().filter(room -> "3".equals(room.getStatus())).count());
		summary.put("disabled", roomList.stream().filter(room -> "4".equals(room.getStatus())).count());
		return summary;
	}

	private String buildFloorKey(Long selectedBuildingId, Long buildingId, Integer floorNo) {
		Integer floor = floorNo == null ? 0 : floorNo;
		if (selectedBuildingId != null) {
			return String.valueOf(floor);
		}
		return (buildingId == null ? 0 : buildingId) + "-" + floor;
	}

	private int compareFloorKey(String left, String right, boolean includeBuilding) {
		if (!includeBuilding) {
			return Integer.compare(parseInt(right), parseInt(left));
		}
		String[] leftParts = left.split("-");
		String[] rightParts = right.split("-");
		int buildingCompare = Long.compare(parseLong(leftParts[0]), parseLong(rightParts[0]));
		if (buildingCompare != 0) {
			return buildingCompare;
		}
		return Integer.compare(parseInt(rightParts[1]), parseInt(leftParts[1]));
	}

	private int parseInt(String value) {
		return Integer.parseInt(value == null || value.isBlank() ? "0" : value);
	}

	private long parseLong(String value) {
		return Long.parseLong(value == null || value.isBlank() ? "0" : value);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	@SafeVarargs
	private final <T> T firstNonNull(T... values) {
		for (T value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

}
