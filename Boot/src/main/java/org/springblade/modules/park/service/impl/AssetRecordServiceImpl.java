/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.park.mapper.AssetRecordMapper;
import org.springblade.modules.park.pojo.entity.AssetRecord;
import org.springblade.modules.park.pojo.entity.Building;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.service.IAssetRecordService;
import org.springblade.modules.park.service.IBuildingService;
import org.springblade.modules.park.service.IRoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 资产登记台账服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class AssetRecordServiceImpl extends ServiceImpl<AssetRecordMapper, AssetRecord> implements IAssetRecordService {

	private static final List<String> ASSET_CATEGORIES = List.of("facility", "office", "safety", "other");
	private static final List<String> ASSET_STATUSES = List.of("in_use", "idle", "repair", "scrapped");

	private final IBuildingService buildingService;
	private final IRoomService roomService;

	@Override
	public AssetRecord selectAssetById(Long assetId) {
		return baseMapper.selectAssetById(assetId);
	}

	@Override
	public IPage<AssetRecord> selectAssetPage(IPage<AssetRecord> page, AssetRecord asset) {
		return page.setRecords(baseMapper.selectAssetPage(page, asset));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitAsset(AssetRecord asset) {
		normalizeAsset(asset);
		validateAsset(asset);
		Date now = new Date();
		String userName = currentUserName();
		asset.setDelFlag("0");
		asset.setUpdateBy(userName);
		asset.setUpdateTime(now);
		if (asset.getAssetId() == null) {
			asset.setCreateBy(userName);
			asset.setCreateTime(now);
		} else {
			AssetRecord existing = selectAssetById(asset.getAssetId());
			if (existing == null) {
				throw new ServiceException("资产记录不存在");
			}
			asset.setCreateBy(existing.getCreateBy());
			asset.setCreateTime(existing.getCreateTime());
		}
		return saveOrUpdate(asset);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeAsset(String ids) {
		List<Long> assetIds = Func.toLongList(ids);
		if (assetIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的资产");
		}
		return baseMapper.deleteAssetByIds(assetIds, currentUserName()) > 0;
	}

	private void normalizeAsset(AssetRecord asset) {
		if (asset == null) return;
		if (StringUtil.isNotBlank(asset.getAssetCode())) asset.setAssetCode(asset.getAssetCode().trim());
		if (StringUtil.isNotBlank(asset.getAssetName())) asset.setAssetName(asset.getAssetName().trim());
		if (StringUtil.isNotBlank(asset.getBrandModel())) asset.setBrandModel(asset.getBrandModel().trim());
		if (StringUtil.isNotBlank(asset.getResponsiblePerson())) asset.setResponsiblePerson(asset.getResponsiblePerson().trim());
		asset.setQuantity(asset.getQuantity() == null ? 1 : asset.getQuantity());
		asset.setUnit(StringUtil.isBlank(asset.getUnit()) ? "件" : asset.getUnit().trim());
		asset.setOriginalValue(asset.getOriginalValue() == null ? BigDecimal.ZERO : asset.getOriginalValue());
		asset.setAssetStatus(StringUtil.isBlank(asset.getAssetStatus()) ? "in_use" : asset.getAssetStatus());
	}

	private void validateAsset(AssetRecord asset) {
		if (asset == null || StringUtil.isBlank(asset.getAssetCode())) throw new ServiceException("请输入资产编号");
		if (StringUtil.isBlank(asset.getAssetName())) throw new ServiceException("请输入资产名称");
		if (!ASSET_CATEGORIES.contains(Func.toStr(asset.getAssetCategory()))) throw new ServiceException("资产分类不正确");
		if (!ASSET_STATUSES.contains(Func.toStr(asset.getAssetStatus()))) throw new ServiceException("资产状态不正确");
		if (asset.getQuantity() == null || asset.getQuantity() <= 0) throw new ServiceException("资产数量必须大于0");
		if (asset.getOriginalValue() == null || asset.getOriginalValue().compareTo(BigDecimal.ZERO) < 0) {
			throw new ServiceException("资产原值不能小于0");
		}
		if (baseMapper.countAssetCode(asset.getAssetCode(), asset.getAssetId()) > 0) {
			throw new ServiceException("资产编号已存在");
		}
		resolveLocation(asset);
	}

	private void resolveLocation(AssetRecord asset) {
		if (asset.getRoomId() != null) {
			Room room = roomService.selectRoomById(asset.getRoomId());
			if (room == null) throw new ServiceException("关联房间不存在");
			asset.setParkId(room.getParkId());
			asset.setBuildingId(room.getBuildingId());
			asset.setFloorNo(room.getFloor());
			return;
		}
		if (asset.getBuildingId() == null) throw new ServiceException("请选择安装位置");
		Building building = buildingService.getById(asset.getBuildingId());
		if (building == null) throw new ServiceException("关联楼宇不存在");
		asset.setParkId(building.getParkId());
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

}
