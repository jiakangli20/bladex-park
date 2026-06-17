/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.pojo.entity.Merchant;

import java.util.List;

/**
 * 商户服务接口.
 *
 * @author BladeX
 */
public interface IMerchantService extends IService<Merchant> {

	Merchant selectMerchantById(Long merchantId);

	List<Merchant> selectMerchantList(Merchant merchant);

	IPage<Merchant> selectMerchantPage(IPage<Merchant> page, Merchant merchant);

	Kv selectMerchantStatistics(Merchant merchant);

	boolean insertMerchant(Merchant merchant);

	boolean updateMerchant(Merchant merchant);

	boolean submitMerchant(Merchant merchant);

	boolean deleteMerchantByIds(String ids);

	boolean changeStatus(Long merchantId, String status);

}
