/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.park.mapper.SmartDeviceMapper;
import org.springblade.modules.park.pojo.entity.Building;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.entity.SmartDevice;
import org.springblade.modules.park.service.IBuildingService;
import org.springblade.modules.park.service.IRoomService;
import org.springblade.modules.park.service.ISmartDeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 智能设备台账服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class SmartDeviceServiceImpl extends ServiceImpl<SmartDeviceMapper, SmartDevice> implements ISmartDeviceService {

	private static final List<String> DEVICE_TYPES = List.of("electric", "water");
	private static final List<String> PAYMENT_TYPES = List.of("prepaid", "postpaid");
	private static final List<String> METER_TYPES = List.of("branch", "total", "public");

	private final IBuildingService buildingService;
	private final IRoomService roomService;

	@Override
	public SmartDevice selectDeviceById(Long deviceId) {
		return baseMapper.selectDeviceById(deviceId);
	}

	@Override
	public IPage<SmartDevice> selectDevicePage(IPage<SmartDevice> page, SmartDevice device) {
		return page.setRecords(baseMapper.selectDevicePage(page, device));
	}

	@Override
	public Kv selectDeviceStatistics(SmartDevice device) {
		Map<String, Object> statistics = baseMapper.selectDeviceStatistics(device);
		return Kv.create()
			.set("totalCount", toLong(statistics, "totalCount"))
			.set("onlineCount", toLong(statistics, "onlineCount"))
			.set("offlineCount", toLong(statistics, "offlineCount"))
			.set("disabledCount", toLong(statistics, "disabledCount"));
	}

	@Override
	public List<Map<String, Object>> selectDeviceTypeStatistics() {
		return baseMapper.selectDeviceTypeStatistics();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitDevice(SmartDevice device) {
		normalizeDevice(device);
		validateDevice(device);
		Date now = new Date();
		String userName = currentUserName();
		device.setDelFlag("0");
		device.setUpdateBy(userName);
		device.setUpdateTime(now);
		if (device.getDeviceId() == null) {
			device.setCreateBy(userName);
			device.setCreateTime(now);
		} else {
			SmartDevice existing = selectDeviceById(device.getDeviceId());
			if (existing == null) {
				throw new ServiceException("设备不存在");
			}
			device.setCreateBy(existing.getCreateBy());
			device.setCreateTime(existing.getCreateTime());
		}
		return saveOrUpdate(device);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeDevice(String ids) {
		List<Long> deviceIds = Func.toLongList(ids);
		if (deviceIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的设备");
		}
		return baseMapper.deleteDeviceByIds(deviceIds, currentUserName()) > 0;
	}

	private void validateDevice(SmartDevice device) {
		if (device == null || StringUtil.isBlank(device.getDeviceName())) {
			throw new ServiceException("请输入设备名称");
		}
		if (StringUtil.isBlank(device.getDeviceCode())) {
			throw new ServiceException("请输入设备编码");
		}
		if (!DEVICE_TYPES.contains(Func.toStr(device.getDeviceType()))) {
			throw new ServiceException("设备类型不正确");
		}
		if (StringUtil.isBlank(device.getBrand())) {
			throw new ServiceException("请选择或输入设备厂商");
		}
		if (!PAYMENT_TYPES.contains(Func.toStr(device.getPaymentType()))) {
			throw new ServiceException("付费类型不正确");
		}
		if (!METER_TYPES.contains(Func.toStr(device.getMeterType()))) {
			throw new ServiceException("表类型不正确");
		}
		if (StringUtil.isBlank(device.getPurpose())) {
			throw new ServiceException("请选择用途");
		}
		if (device.getMultiplier() == null || device.getMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("倍率必须大于0");
		}
		if (device.getMaxReading() == null || device.getMaxReading().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("最大读数必须大于0");
		}
		if (device.getCurrentReading().compareTo(device.getMaxReading()) > 0) {
			throw new ServiceException("当前读数不能超过最大读数");
		}
		if (StringUtil.isNotBlank(device.getOnlineStatus()) && !List.of("0", "1").contains(device.getOnlineStatus())) {
			throw new ServiceException("设备在线状态不正确");
		}
		if (StringUtil.isNotBlank(device.getEnabledStatus()) && !List.of("0", "1").contains(device.getEnabledStatus())) {
			throw new ServiceException("设备启用状态不正确");
		}
		if (baseMapper.countDeviceCode(device.getDeviceCode().trim(), device.getDeviceId()) > 0) {
			throw new ServiceException("设备编码已存在");
		}
		resolveLocation(device);
	}

	private void normalizeDevice(SmartDevice device) {
		if (device == null) {
			return;
		}
		if (StringUtil.isNotBlank(device.getDeviceName())) {
			device.setDeviceName(device.getDeviceName().trim());
		}
		if (StringUtil.isNotBlank(device.getDeviceCode())) {
			device.setDeviceCode(device.getDeviceCode().trim());
		}
		if (StringUtil.isNotBlank(device.getBrand())) {
			device.setBrand(device.getBrand().trim());
		}
		device.setPaymentType(StringUtil.isBlank(device.getPaymentType()) ? "postpaid" : device.getPaymentType());
		device.setMeterType(StringUtil.isBlank(device.getMeterType()) ? "branch" : device.getMeterType());
		device.setCurrentReading(device.getCurrentReading() == null ? BigDecimal.ZERO : device.getCurrentReading());
		device.setCurrentBalance(device.getCurrentBalance() == null ? BigDecimal.ZERO : device.getCurrentBalance());
		device.setMultiplier(device.getMultiplier() == null ? BigDecimal.ONE : device.getMultiplier());
		device.setOnlineStatus(StringUtil.isBlank(device.getOnlineStatus()) ? "1" : device.getOnlineStatus());
		device.setEnabledStatus(StringUtil.isBlank(device.getEnabledStatus()) ? "0" : device.getEnabledStatus());
	}

	private void resolveLocation(SmartDevice device) {
		if (device.getRoomId() != null) {
			Room room = roomService.selectRoomById(device.getRoomId());
			if (room == null) {
				throw new ServiceException("关联房间不存在");
			}
			device.setParkId(room.getParkId());
			device.setBuildingId(room.getBuildingId());
			device.setFloorNo(room.getFloor());
			return;
		}
		if (device.getBuildingId() != null) {
			Building building = buildingService.getById(device.getBuildingId());
			if (building == null) {
				throw new ServiceException("关联楼宇不存在");
			}
			device.setParkId(building.getParkId());
		}
		if (device.getParkId() == null) {
			throw new ServiceException("请选择所属园区");
		}
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

	private long toLong(Map<String, Object> map, String key) {
		if (map == null || map.get(key) == null) {
			return 0L;
		}
		Object value = map.get(key);
		return value instanceof Number number ? number.longValue() : Func.toLong(value, 0L);
	}

}
