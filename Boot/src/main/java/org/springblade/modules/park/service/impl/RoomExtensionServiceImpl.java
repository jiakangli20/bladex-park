package org.springblade.modules.park.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.park.mapper.RoomUtilityRecordMapper;
import org.springblade.modules.park.mapper.RoomVehicleMapper;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.entity.RoomUtilityRecord;
import org.springblade.modules.park.pojo.entity.RoomVehicle;
import org.springblade.modules.park.pojo.entity.SmartDevice;
import org.springblade.modules.park.service.IRoomExtensionService;
import org.springblade.modules.park.service.IRoomService;
import org.springblade.modules.park.service.ISmartDeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RoomExtensionServiceImpl implements IRoomExtensionService {

	private final RoomUtilityRecordMapper utilityRecordMapper;
	private final RoomVehicleMapper vehicleMapper;
	private final IRoomService roomService;
	private final ISmartDeviceService smartDeviceService;

	@Override
	public IPage<RoomUtilityRecord> utilityPage(IPage<RoomUtilityRecord> page, Long roomId) {
		requireRoom(roomId);
		return page.setRecords(utilityRecordMapper.selectRecordPage(page, roomId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitUtility(RoomUtilityRecord record) {
		if (record == null) throw new ServiceException("抄表记录不能为空");
		requireRoom(record.getRoomId());
		SmartDevice device = smartDeviceService.selectDeviceById(record.getDeviceId());
		if (device == null || !Objects.equals(device.getRoomId(), record.getRoomId())) {
			throw new ServiceException("请选择当前房源下的水表或电表");
		}
		if (!List.of("water", "electric").contains(device.getDeviceType())) {
			throw new ServiceException("所选设备不是水表或电表");
		}
		if (record.getCurrentReading() == null || record.getCurrentReading().compareTo(BigDecimal.ZERO) < 0) {
			throw new ServiceException("请输入有效的本次读数");
		}
		BigDecimal previous = record.getPreviousReading() == null ? BigDecimal.ZERO : record.getPreviousReading();
		if (record.getCurrentReading().compareTo(previous) < 0) {
			throw new ServiceException("本次读数不能小于上次读数");
		}
		Date now = new Date();
		record.setRecordId(null);
		record.setRecordType(device.getDeviceType());
		record.setReadingTime(record.getReadingTime() == null ? now : record.getReadingTime());
		record.setPreviousReading(previous);
		record.setUsageAmount(record.getCurrentReading().subtract(previous));
		record.setAmount(record.getAmount() == null ? BigDecimal.ZERO : record.getAmount());
		record.setOperatorName(StringUtil.isBlank(record.getOperatorName()) ? currentUserName() : record.getOperatorName());
		record.setDelFlag("0");
		record.setCreateBy(currentUserName());
		record.setCreateTime(now);
		return utilityRecordMapper.insert(record) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeUtility(Long recordId) {
		if (recordId == null) throw new ServiceException("请选择抄表记录");
		return utilityRecordMapper.deleteRecord(recordId, currentUserName()) > 0;
	}

	@Override
	public IPage<RoomVehicle> vehiclePage(IPage<RoomVehicle> page, Long roomId) {
		requireRoom(roomId);
		return page.setRecords(vehicleMapper.selectVehiclePage(page, roomId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitVehicle(RoomVehicle vehicle) {
		if (vehicle == null) throw new ServiceException("车辆信息不能为空");
		requireRoom(vehicle.getRoomId());
		if (StringUtil.isBlank(vehicle.getPlateNo())) throw new ServiceException("请输入车牌号");
		vehicle.setPlateNo(vehicle.getPlateNo().trim().toUpperCase(Locale.ROOT));
		if (vehicle.getValidStart() != null && vehicle.getValidEnd() != null && vehicle.getValidEnd().before(vehicle.getValidStart())) {
			throw new ServiceException("有效期结束日期不能早于开始日期");
		}
		if (vehicleMapper.countActivePlate(vehicle.getRoomId(), vehicle.getPlateNo()) > 0) {
			throw new ServiceException("该房源已绑定此车牌");
		}
		Date now = new Date();
		vehicle.setVehicleId(null);
		vehicle.setVehicleType(StringUtil.isBlank(vehicle.getVehicleType()) ? "car" : vehicle.getVehicleType());
		vehicle.setStatus(StringUtil.isBlank(vehicle.getStatus()) ? "active" : vehicle.getStatus());
		vehicle.setDelFlag("0");
		vehicle.setCreateBy(currentUserName());
		vehicle.setCreateTime(now);
		return vehicleMapper.insert(vehicle) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeVehicle(Long vehicleId) {
		if (vehicleId == null) throw new ServiceException("请选择车辆");
		return vehicleMapper.deleteVehicle(vehicleId, currentUserName()) > 0;
	}

	private Room requireRoom(Long roomId) {
		Room room = roomId == null ? null : roomService.getById(roomId);
		if (room == null) throw new ServiceException("房源不存在");
		return room;
	}

	private String currentUserName() {
		return StringUtil.isBlank(AuthUtil.getUserName()) ? "system" : AuthUtil.getUserName();
	}
}
