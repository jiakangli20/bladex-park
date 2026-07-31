/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.park.pojo.entity.SmartDevice;

import java.util.List;
import java.util.Map;

/**
 * 智能设备台账服务.
 *
 * @author BladeX
 */
public interface ISmartDeviceService extends IService<SmartDevice> {

	SmartDevice selectDeviceById(Long deviceId);

	IPage<SmartDevice> selectDevicePage(IPage<SmartDevice> page, SmartDevice device);

	Kv selectDeviceStatistics(SmartDevice device);

	List<Map<String, Object>> selectDeviceTypeStatistics();

	/**
	 * 企业服务“在园企业数据”看板使用的全部园区设备统计。
	 */
	List<Map<String, Object>> selectAllParkDeviceTypeStatistics();

	boolean submitDevice(SmartDevice device);

	boolean removeDevice(String ids);

}
