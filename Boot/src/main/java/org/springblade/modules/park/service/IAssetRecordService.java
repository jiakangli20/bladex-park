/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.park.pojo.entity.AssetRecord;

/**
 * 资产登记台账服务.
 *
 * @author BladeX
 */
public interface IAssetRecordService extends IService<AssetRecord> {

	AssetRecord selectAssetById(Long assetId);

	IPage<AssetRecord> selectAssetPage(IPage<AssetRecord> page, AssetRecord asset);

	boolean submitAsset(AssetRecord asset);

	boolean removeAsset(String ids);

}
