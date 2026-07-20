/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.park.pojo.entity.AssetRecord;

import java.util.List;

/**
 * 资产登记台账 Mapper.
 *
 * @author BladeX
 */
public interface AssetRecordMapper extends BaseMapper<AssetRecord> {

	AssetRecord selectAssetById(@Param("assetId") Long assetId);

	List<AssetRecord> selectAssetPage(IPage<AssetRecord> page, @Param("asset") AssetRecord asset);

	int countAssetCode(@Param("assetCode") String assetCode, @Param("excludeId") Long excludeId);

	int deleteAssetByIds(@Param("ids") List<Long> ids, @Param("updateBy") String updateBy);

}
