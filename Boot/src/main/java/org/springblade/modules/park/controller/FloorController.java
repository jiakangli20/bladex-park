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
package org.springblade.modules.park.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.park.constant.ParkConstant;
import org.springblade.modules.park.pojo.entity.Floor;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.service.IFloorService;
import org.springblade.modules.park.service.IRoomService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 楼层管理控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "floor")
@RequestMapping(ParkConstant.APPLICATION_PARK_NAME + "/floor")
@Tag(name = "楼层管理", description = "楼层管理接口")
public class FloorController extends BladeController {

	private final IFloorService floorService;
	private final IRoomService roomService;

	/**
	 * 分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "分页", description = "传入floor")
	public R<IPage<Floor>> page(Floor floor, Query query) {
		return R.data(floorService.selectFloorPage(Condition.getPage(query), floor));
	}

	/**
	 * 列表
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "列表", description = "传入floor")
	public R<List<Floor>> list(Floor floor) {
		return R.data(floorService.selectFloorList(floor));
	}

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "详情", description = "传入id")
	public R<Floor> detail(@RequestParam Long id, @RequestParam(required = false) String roomStatus) {
		return R.data(floorService.selectFloorById(id, roomStatus));
	}

	/**
	 * 楼层统计
	 */
	@GetMapping("/statistics")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "楼层统计", description = "传入floor")
	public R<Map<String, Object>> statistics(Floor floor) {
		return R.data(floorService.selectFloorStatistics(floor));
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增或修改", description = "传入floor")
	public R submit(@RequestBody Floor floor) {
		return R.status(floorService.submit(floor));
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(floorService.removeFloor(ids));
	}

	/**
	 * 新增或修改房源
	 */
	@PostMapping("/room/submit")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "新增或修改房源", description = "传入room")
	public R submitRoom(@RequestBody Room room) {
		return R.status(roomService.submit(room));
	}

	/**
	 * 删除房源
	 */
	@PostMapping("/room/remove")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "删除房源", description = "传入ids")
	public R removeRoom(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(roomService.removeRoom(ids));
	}

	/**
	 * 同步单建筑楼层
	 */
	@PostMapping("/sync/{buildingId}")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "同步单建筑楼层", description = "传入buildingId")
	public R sync(@PathVariable Long buildingId) {
		floorService.syncBuildingFloors(buildingId, AuthUtil.getUserName());
		return R.success("同步成功");
	}

	/**
	 * 同步全部建筑楼层
	 */
	@PostMapping("/sync-all")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "同步全部建筑楼层")
	public R syncAll() {
		floorService.syncAllBuildingFloors(AuthUtil.getUserName());
		return R.success("同步成功");
	}

}
