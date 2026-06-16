/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.pojo.entity.ServiceWorkorder;
import org.springblade.modules.business.pojo.entity.WorkorderLog;

import java.util.List;
import java.util.Map;

/**
 * 物业服务工单服务.
 *
 * @author BladeX
 */
public interface IPropertyWorkorderService extends IService<ServiceWorkorder> {

	ServiceWorkorder selectWorkorderById(Long orderId);

	List<ServiceWorkorder> selectWorkorderList(ServiceWorkorder workorder);

	IPage<ServiceWorkorder> selectWorkorderPage(IPage<ServiceWorkorder> page, ServiceWorkorder workorder);

	Map<String, Object> selectWorkorderStatistics(ServiceWorkorder workorder);

	boolean insertWorkorder(ServiceWorkorder workorder);

	boolean updateWorkorder(ServiceWorkorder workorder);

	boolean submitWorkorder(ServiceWorkorder workorder);

	boolean deleteWorkorderByIds(String ids);

	boolean assignWorkorder(ServiceWorkorder workorder);

	boolean finishWorkorder(ServiceWorkorder workorder);

	boolean closeWorkorder(ServiceWorkorder workorder);

	boolean rateWorkorder(ServiceWorkorder workorder);

	List<WorkorderLog> selectLogByOrderId(Long orderId);

}
