/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.pojo.entity.MerchantAd;
import org.springblade.modules.miniapp.pojo.entity.MerchantAdAuditLog;

import java.util.List;

/**
 * 商户小程序广告服务.
 *
 * @author BladeX
 */
public interface IMerchantAdService extends IService<MerchantAd> {

	MerchantAd selectAdById(Long adId);

	List<MerchantAd> selectAdList(MerchantAd ad);

	List<MerchantAd> selectPublicAdList(Long parkId, String adPosition);

	IPage<MerchantAd> selectAdPage(IPage<MerchantAd> page, MerchantAd ad);

	Kv selectAdStatistics(MerchantAd ad);

	boolean insertAd(MerchantAd ad);

	boolean updateAd(MerchantAd ad);

	boolean submitAd(MerchantAd ad);

	boolean deleteAdByIds(String ids);

	boolean changeStatus(Long adId, String status);

	boolean audit(Long adId, String auditStatus, String opinion);

	List<MerchantAdAuditLog> auditLogs(Long adId);

	MerchantAd createCustomerAd(MerchantAd ad, Long parkId, Long customerId, Long memberId);

	MerchantAd updateCustomerAd(MerchantAd ad, Long parkId, Long customerId);

	boolean submitCustomerAd(Long adId, Long parkId, Long customerId);

	boolean withdrawCustomerAd(Long adId, Long parkId, Long customerId);

	List<MerchantAd> selectCustomerAdList(Long parkId, Long customerId);

	MerchantAd selectCustomerAdById(Long adId, Long parkId, Long customerId);

}
