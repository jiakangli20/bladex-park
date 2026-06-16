/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.ServiceWorkorder;
import org.springblade.modules.business.pojo.entity.WorkorderLog;

import java.util.List;
import java.util.Map;

/**
 * 物业服务工单 Mapper.
 *
 * @author BladeX
 */
public interface PropertyWorkorderMapper extends BaseMapper<ServiceWorkorder> {

	ServiceWorkorder selectWorkorderById(@Param("orderId") Long orderId);

	List<ServiceWorkorder> selectWorkorderList(@Param("workorder") ServiceWorkorder workorder);

	IPage<ServiceWorkorder> selectWorkorderPage(IPage<ServiceWorkorder> page,
												@Param("workorder") ServiceWorkorder workorder);

	Map<String, Object> selectWorkorderStatistics(@Param("workorder") ServiceWorkorder workorder);

	int insertWorkorder(ServiceWorkorder workorder);

	int updateWorkorder(ServiceWorkorder workorder);

	int deleteWorkorderByIds(@Param("ids") List<Long> ids, @Param("parkId") Long parkId);

	int insertLog(WorkorderLog log);

	List<WorkorderLog> selectLogByOrderId(@Param("orderId") Long orderId);

	String selectLastOrderNoByDate(@Param("datePrefix") String datePrefix);

}
