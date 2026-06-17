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
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.park.constant.ParkConstant;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.vo.RoomVO;
import org.springblade.modules.park.service.IRoomService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房源控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "rent_control")
@RequestMapping(ParkConstant.APPLICATION_PARK_NAME + "/room")
@Tag(name = "房源管理", description = "房源管理接口")
public class RoomController extends BladeController {

	private final IRoomService roomService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入id")
	public R<RoomVO> detail(@RequestParam Long id) {
		return R.data(roomService.selectRoomById(id));
	}

	/**
	 * 列表
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "列表", description = "传入room")
	public R<List<RoomVO>> list(Room room) {
		return R.data(roomService.selectRoomList(room));
	}

	/**
	 * 分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入room")
	public R<IPage<RoomVO>> page(Room room, Query query) {
		return R.data(roomService.selectRoomPage(Condition.getPage(query), room));
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增或修改", description = "传入room")
	public R submit(@RequestBody Room room) {
		return R.status(roomService.submit(room));
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(roomService.removeRoom(ids));
	}

	/**
	 * 状态流转
	 */
	@PostMapping("/change-status")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "状态流转", description = "传入id,status")
	public R changeStatus(@RequestParam Long id, @RequestParam String status) {
		return R.status(roomService.changeStatus(id, status));
	}

	/**
	 * 标记小程序同步
	 */
	@PostMapping("/sync-mini")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "标记小程序同步", description = "传入id")
	public R syncMini(@RequestParam Long id) {
		return R.status(roomService.syncMini(id));
	}

}
