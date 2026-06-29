/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.pojo.entity.MerchantAd;

import java.util.List;

/**
 * 商户小程序广告服务.
 *
 * @author BladeX
 */
public interface IMerchantAdService extends IService<MerchantAd> {

	MerchantAd selectAdById(Long adId);

	List<MerchantAd> selectAdList(MerchantAd ad);

	IPage<MerchantAd> selectAdPage(IPage<MerchantAd> page, MerchantAd ad);

	Kv selectAdStatistics(MerchantAd ad);

	boolean insertAd(MerchantAd ad);

	boolean updateAd(MerchantAd ad);

	boolean submitAd(MerchantAd ad);

	boolean deleteAdByIds(String ids);

	boolean changeStatus(Long adId, String status);

}
