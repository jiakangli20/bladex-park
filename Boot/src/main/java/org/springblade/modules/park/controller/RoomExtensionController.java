package org.springblade.modules.park.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.park.constant.ParkConstant;
import org.springblade.modules.park.pojo.entity.RoomUtilityRecord;
import org.springblade.modules.park.pojo.entity.RoomVehicle;
import org.springblade.modules.park.service.IRoomExtensionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NonDS
@RestController
@RequiredArgsConstructor
@PreAuth(menu = "rent_control")
@RequestMapping(ParkConstant.APPLICATION_PARK_NAME + "/room-extension")
@Tag(name = "房源扩展记录", description = "水电抄表和车辆绑定")
public class RoomExtensionController extends BladeController {

	private final IRoomExtensionService roomExtensionService;

	@GetMapping("/utility/page")
	@Operation(summary = "水电记录分页")
	public R<IPage<RoomUtilityRecord>> utilityPage(@RequestParam Long roomId, Query query) {
		return R.data(roomExtensionService.utilityPage(Condition.getPage(query), roomId));
	}

	@PostMapping("/utility/submit")
	@Operation(summary = "新增水电记录")
	public R submitUtility(@RequestBody RoomUtilityRecord record) {
		return R.status(roomExtensionService.submitUtility(record));
	}

	@PostMapping("/utility/remove")
	@Operation(summary = "删除水电记录")
	public R removeUtility(@RequestParam Long recordId) {
		return R.status(roomExtensionService.removeUtility(recordId));
	}

	@GetMapping("/vehicle/page")
	@Operation(summary = "绑定车辆分页")
	public R<IPage<RoomVehicle>> vehiclePage(@RequestParam Long roomId, Query query) {
		return R.data(roomExtensionService.vehiclePage(Condition.getPage(query), roomId));
	}

	@PostMapping("/vehicle/submit")
	@Operation(summary = "新增绑定车辆")
	public R submitVehicle(@RequestBody RoomVehicle vehicle) {
		return R.status(roomExtensionService.submitVehicle(vehicle));
	}

	@PostMapping("/vehicle/remove")
	@Operation(summary = "删除绑定车辆")
	public R removeVehicle(@RequestParam Long vehicleId) {
		return R.status(roomExtensionService.removeVehicle(vehicleId));
	}
}
