package org.springblade.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.park.mapper.ParkMapper;
import org.springblade.modules.system.mapper.UserParkMapper;
import org.springblade.modules.system.pojo.entity.UserPark;
import org.springblade.modules.system.service.IUserParkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class UserParkServiceImpl extends ServiceImpl<UserParkMapper, UserPark> implements IUserParkService {
	private final ParkMapper parkMapper;

	public UserParkServiceImpl(ParkMapper parkMapper) {
		this.parkMapper = parkMapper;
	}

	@Override
	public List<UserPark> userParks(List<Long> userIds) {
		if (Func.isEmpty(userIds)) return List.of();
		return list(Wrappers.<UserPark>lambdaQuery().in(UserPark::getUserId, userIds));
	}

	@Override
	public List<Long> parkIds(Long userId) {
		return parkIds(userId, AuthUtil.getTenantId());
	}

	@Override
	public List<Long> parkIds(Long userId, String tenantId) {
		if (userId == null) return List.of();
		return list(Wrappers.<UserPark>lambdaQuery()
			.eq(UserPark::getTenantId, tenantId)
			.eq(UserPark::getUserId, userId))
			.stream().map(UserPark::getParkId).toList();
	}

	@Override
	public Long defaultParkId(Long userId) {
		return defaultParkId(userId, AuthUtil.getTenantId());
	}

	@Override
	public Long defaultParkId(Long userId, String tenantId) {
		return list(Wrappers.<UserPark>lambdaQuery()
			.eq(UserPark::getTenantId, tenantId)
			.eq(UserPark::getUserId, userId)
			.eq(UserPark::getIsDefault, 1).last("LIMIT 1"))
			.stream().map(UserPark::getParkId).findFirst().orElse(null);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveUserParks(Long userId, String tenantId, List<Long> parkIds, Long defaultParkId) {
		if (userId == null) throw new ServiceException("用户ID不能为空");
		String actualTenantId = Func.isEmpty(tenantId) ? AuthUtil.getTenantId() : tenantId;
		if (Func.isEmpty(actualTenantId)) throw new ServiceException("租户ID不能为空");
		List<Long> normalized = new ArrayList<>(new LinkedHashSet<>(Func.isEmpty(parkIds) ? List.of() : parkIds));
		if (defaultParkId != null && !normalized.contains(defaultParkId)) {
			throw new ServiceException("默认园区必须包含在授权园区中");
		}
		if (!AuthUtil.isAdministrator() && !AuthUtil.isAdmin()) {
			if (!actualTenantId.equals(AuthUtil.getTenantId())) {
				throw new ServiceException("无权跨租户配置用户园区");
			}
			List<Long> operatorParkIds = parkIds(AuthUtil.getUserId(), actualTenantId);
			if (!operatorParkIds.containsAll(normalized)) {
				throw new ServiceException("只能授权当前账号有权管理的园区");
			}
		}
		if (!normalized.isEmpty() && parkMapper.selectBatchIds(normalized).size() != normalized.size()) {
			throw new ServiceException("授权园区不存在或已失效");
		}
		remove(Wrappers.<UserPark>lambdaQuery()
			.eq(UserPark::getTenantId, actualTenantId)
			.eq(UserPark::getUserId, userId));
		if (normalized.isEmpty()) return true;
		Long actualDefault = defaultParkId == null ? normalized.get(0) : defaultParkId;
		List<UserPark> rows = normalized.stream().map(parkId -> {
			UserPark row = new UserPark();
			row.setTenantId(actualTenantId);
			row.setUserId(userId);
			row.setParkId(parkId);
			row.setIsDefault(parkId.equals(actualDefault) ? 1 : 0);
			return row;
		}).toList();
		return saveBatch(rows);
	}
}
