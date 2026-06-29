/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.MerchantAd;

import java.util.List;
import java.util.Map;

/**
 * 商户小程序广告 Mapper.
 *
 * @author BladeX
 */
public interface MerchantAdMapper extends BaseMapper<MerchantAd> {

	MerchantAd selectAdById(@Param("adId") Long adId);

	List<MerchantAd> selectAdList(@Param("ad") MerchantAd ad);

	IPage<MerchantAd> selectAdPage(IPage<MerchantAd> page, @Param("ad") MerchantAd ad);

	Map<String, Object> selectAdStatistics(@Param("ad") MerchantAd ad);

	int insertAd(MerchantAd ad);

	int updateAd(MerchantAd ad);

	int deleteAdByIds(@Param("ids") List<Long> ids, @Param("parkId") Long parkId, @Param("updateBy") String updateBy);

	int updateAdStatus(@Param("adId") Long adId, @Param("status") String status, @Param("updateBy") String updateBy);

}
