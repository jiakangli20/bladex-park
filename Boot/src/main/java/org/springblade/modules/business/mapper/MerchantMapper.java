/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.Merchant;

import java.util.List;
import java.util.Map;

/**
 * 商户服务 Mapper.
 *
 * @author BladeX
 */
public interface MerchantMapper extends BaseMapper<Merchant> {

	Merchant selectMerchantById(@Param("merchantId") Long merchantId);

	List<Merchant> selectMerchantList(@Param("merchant") Merchant merchant);

	IPage<Merchant> selectMerchantPage(IPage<Merchant> page, @Param("merchant") Merchant merchant);

	Map<String, Object> selectMerchantStatistics(@Param("merchant") Merchant merchant);

	int insertMerchant(Merchant merchant);

	int updateMerchant(Merchant merchant);

	int deleteMerchantByIds(@Param("ids") List<Long> ids, @Param("parkId") Long parkId, @Param("updateBy") String updateBy);

	int updateMerchantStatus(@Param("merchantId") Long merchantId, @Param("status") String status, @Param("updateBy") String updateBy);

}
