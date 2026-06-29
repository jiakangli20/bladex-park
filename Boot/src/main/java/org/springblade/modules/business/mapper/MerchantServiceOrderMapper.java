/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.MerchantServiceOrder;
import org.springblade.modules.business.pojo.entity.MerchantServiceOrderLog;

import java.util.List;
import java.util.Map;

/**
 * 商户增值服务处理单 Mapper.
 *
 * @author BladeX
 */
public interface MerchantServiceOrderMapper extends BaseMapper<MerchantServiceOrder> {

	MerchantServiceOrder selectOrderById(@Param("orderId") Long orderId);

	List<MerchantServiceOrder> selectOrderList(@Param("order") MerchantServiceOrder order);

	IPage<MerchantServiceOrder> selectOrderPage(IPage<MerchantServiceOrder> page, @Param("order") MerchantServiceOrder order);

	Map<String, Object> selectOrderStatistics(@Param("order") MerchantServiceOrder order);

	int insertOrder(MerchantServiceOrder order);

	int updateOrder(MerchantServiceOrder order);

	int deleteOrderByIds(@Param("ids") List<Long> ids, @Param("parkId") Long parkId, @Param("updateBy") String updateBy);

	int insertLog(MerchantServiceOrderLog log);

	List<MerchantServiceOrderLog> selectLogByOrderId(@Param("orderId") Long orderId);

}
