/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.pojo.entity.MerchantServiceOrder;
import org.springblade.modules.business.pojo.entity.MerchantServiceOrderLog;

import java.util.List;

/**
 * 商户增值服务处理单服务.
 *
 * @author BladeX
 */
public interface IMerchantServiceOrderService extends IService<MerchantServiceOrder> {

	MerchantServiceOrder selectOrderById(Long orderId);

	List<MerchantServiceOrder> selectOrderList(MerchantServiceOrder order);

	IPage<MerchantServiceOrder> selectOrderPage(IPage<MerchantServiceOrder> page, MerchantServiceOrder order);

	Kv selectOrderStatistics(MerchantServiceOrder order);

	boolean insertOrder(MerchantServiceOrder order);

	boolean updateOrder(MerchantServiceOrder order);

	boolean submitOrder(MerchantServiceOrder order);

	boolean deleteOrderByIds(String ids);

	boolean assignOrder(MerchantServiceOrder order);

	boolean followOrder(MerchantServiceOrder order);

	boolean dealOrder(MerchantServiceOrder order);

	boolean closeOrder(MerchantServiceOrder order);

	List<MerchantServiceOrderLog> selectLogByOrderId(Long orderId);

}
