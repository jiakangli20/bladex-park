package org.springblade.modules.park.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.park.pojo.entity.RoomUtilityRecord;
import org.springblade.modules.park.pojo.entity.RoomVehicle;

public interface IRoomExtensionService {

	IPage<RoomUtilityRecord> utilityPage(IPage<RoomUtilityRecord> page, Long roomId);

	boolean submitUtility(RoomUtilityRecord record);

	boolean removeUtility(Long recordId);

	IPage<RoomVehicle> vehiclePage(IPage<RoomVehicle> page, Long roomId);

	boolean submitVehicle(RoomVehicle vehicle);

	boolean removeVehicle(Long vehicleId);
}
