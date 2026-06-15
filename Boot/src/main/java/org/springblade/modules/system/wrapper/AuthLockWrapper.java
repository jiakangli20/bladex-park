package org.springblade.modules.system.wrapper;

import org.springblade.common.cache.UserCache;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.modules.system.enums.AuthLockStatus;
import org.springblade.modules.system.pojo.entity.AuthLock;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.pojo.vo.AuthLockVO;

import java.util.Objects;

/**
 * 认证锁定记录包装类
 *
 * @author BladeX
 */
public class AuthLockWrapper extends BaseEntityWrapper<AuthLock, AuthLockVO> {

	public static AuthLockWrapper build() {
		return new AuthLockWrapper();
	}

	@Override
	public AuthLockVO entityVO(AuthLock entity) {
		AuthLockVO vo = Objects.requireNonNull(BeanUtil.copyProperties(entity, AuthLockVO.class));
		if (entity.getLockType() != null) {
			vo.setLockTypeName(entity.getLockType().getDescription());
		}
		if (entity.getLockStatus() != null) {
			vo.setLockStatusName(entity.getLockStatus().getDescription());
		}
		if (entity.getUserId() != null) {
			User user = UserCache.getUser(entity.getUserId());
			if (user != null) {
				vo.setUserName(user.getName());
			}
		}
		// 惰性过期判定：LOCKED + lockEndTime 已过期 → 标记为已过期（适用于 SYSTEM 和 MANUAL）
		boolean expired = AuthLockStatus.LOCKED.equals(entity.getLockStatus())
			&& entity.getLockEndTime() != null
			&& entity.getLockEndTime().before(DateUtil.now());
		vo.setExpired(expired);
		if (expired) {
			vo.setLockStatusName("已过期");
		}
		return vo;
	}

}
