/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.park.pojo.entity.SmartDevice;

import java.util.List;
import java.util.Map;

/**
 * 智能设备台账 Mapper.
 *
 * @author BladeX
 */
public interface SmartDeviceMapper extends BaseMapper<SmartDevice> {

	SmartDevice selectDeviceById(@Param("deviceId") Long deviceId);

	List<SmartDevice> selectDevicePage(IPage<SmartDevice> page, @Param("device") SmartDevice device);

	Map<String, Object> selectDeviceStatistics(@Param("device") SmartDevice device);

	List<Map<String, Object>> selectDeviceTypeStatistics();

	int countDeviceCode(@Param("deviceCode") String deviceCode, @Param("excludeId") Long excludeId);

	int deleteDeviceByIds(@Param("ids") List<Long> ids, @Param("updateBy") String updateBy);

}
