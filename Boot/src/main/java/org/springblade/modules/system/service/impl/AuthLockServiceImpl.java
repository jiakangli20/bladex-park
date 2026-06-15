package org.springblade.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.cache.UserCache;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.enums.AuthLockStatus;
import org.springblade.modules.system.enums.AuthLockType;
import org.springblade.modules.system.mapper.AuthLockMapper;
import org.springblade.modules.system.pojo.entity.AuthLock;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.service.IAuthLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.springblade.common.constant.ParamConstant.*;

/**
 * 认证锁定记录 服务实现类
 * <p>
 * 计数与锁定统一由数据库管理，lock_target 作为多态目标标识（可存储账号名或IP地址）：
 * <ul>
 *   <li>账号维度：lock_target = 用户账号，remote_ip = 最近一次失败IP（元数据）</li>
 *   <li>IP维度：lock_target = IP地址，remote_ip = IP地址，user_id = null</li>
 * </ul>
 * <p>
 * 状态说明：
 * <ul>
 *   <li>SYSTEM + PRE_LOCKED → 计数中（尚未达到阈值，不阻止登录）</li>
 *   <li>SYSTEM + LOCKED → 锁定生效中（达到阈值，阻止登录）</li>
 *   <li>MANUAL + LOCKED + lock_end_time 为空 → 永久锁定</li>
 *   <li>MANUAL + LOCKED + lock_end_time 非空且大于当前时间 → 限时锁定</li>
 * </ul>
 *
 * @author BladeX
 */
@Service
@AllArgsConstructor
public class AuthLockServiceImpl extends ServiceImpl<AuthLockMapper, AuthLock> implements IAuthLockService {

	// ==================== 失败计数 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addAccountFailCount(String tenantId, String account, Long userId, String ip, String userAgent) {
		if (Func.hasEmpty(tenantId, account)) {
			return;
		}
		if (isAccountLocked(tenantId, account)) {
			return;
		}
		incrementFailCount(tenantId, account, userId, ip, userAgent);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addIpFailCount(String tenantId, String ip, String userAgent) {
		if (Func.hasEmpty(tenantId, ip)) {
			return;
		}
		if (isIpLocked(tenantId, ip)) {
			return;
		}
		incrementFailCount(tenantId, ip, null, ip, userAgent);
	}

	// ==================== 锁定判定 ====================

	@Override
	public boolean isAccountLocked(String tenantId, String account) {
		return hasActiveLock(tenantId, AuthLock::getLockTarget, account);
	}

	@Override
	public boolean isIpLocked(String tenantId, String ip) {
		return hasActiveLock(tenantId, AuthLock::getLockTarget, ip);
	}

	// ==================== 释放锁定 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void releaseSystemLock(String tenantId, String account) {
		releaseByField(tenantId, AuthLock::getLockTarget, account);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void releaseSystemIpLock(String tenantId, String ip) {
		releaseByField(tenantId, AuthLock::getLockTarget, ip);
	}

	// ==================== 手动操作 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean manualLock(Long userId, String reason, Date beginTime, Date endTime) {
		User user = UserCache.getUser(userId);
		if (Func.isEmpty(user)) {
			throw new ServiceException("用户不存在!");
		}
		// 仅检查 MANUAL 类型的有效锁定（含预约），不受 SYSTEM 锁定影响
		boolean hasManualLock = this.count(
			Wrappers.<AuthLock>lambdaQuery()
				.eq(AuthLock::getTenantId, user.getTenantId())
				.eq(AuthLock::getLockType, AuthLockType.MANUAL)
				.eq(AuthLock::getLockStatus, AuthLockStatus.LOCKED)
				.eq(AuthLock::getLockTarget, user.getAccount())
				.eq(AuthLock::getIsDeleted, 0)
				.and(m -> m
					.isNull(AuthLock::getLockEndTime)
					.or()
					.gt(AuthLock::getLockEndTime, DateUtil.now())
				)
		) > 0;
		if (hasManualLock) {
			throw new ServiceException("该用户已存在手动锁定记录，无需重复锁定!");
		}
		// 校验时间范围
		Date effectiveBeginTime = beginTime != null ? beginTime : DateUtil.now();
		if (endTime != null && endTime.before(effectiveBeginTime)) {
			throw new ServiceException("锁定结束时间不能早于" + (beginTime != null ? "开始时间" : "当前时间") + "!");
		}
		AuthLock lock = new AuthLock();
		lock.setTenantId(user.getTenantId());
		lock.setLockType(AuthLockType.MANUAL);
		lock.setLockStatus(AuthLockStatus.LOCKED);
		lock.setLockTarget(user.getAccount());
		lock.setUserId(user.getId());
		lock.setFailCount(0);
		lock.setLockBeginTime(effectiveBeginTime);
		lock.setLockEndTime(endTime);
		lock.setLockReason(reason);
		return this.save(lock);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean manualUnlock(Long lockId, String unlockReason) {
		AuthLock lock = this.getById(lockId);
		if (Func.isEmpty(lock)) {
			throw new ServiceException("锁定记录不存在!");
		}
		if (AuthLockStatus.UN_LOCKED.equals(lock.getLockStatus())) {
			throw new ServiceException("该记录已处于解锁状态!");
		}
		lock.setLockStatus(AuthLockStatus.UN_LOCKED);
		lock.setUnlockReason(unlockReason);
		return this.updateById(lock);
	}

	// ==================== 内部方法 ====================

	/**
	 * 递增失败计数，达到阈值时激活锁定
	 * <p>
	 * 通过 target 参数实现账号与IP两种维度的统一计数：
	 * 账号维度传入用户账号，IP维度传入IP地址，均存储在 lock_target 字段中
	 *
	 * @param tenantId  租户ID
	 * @param target    锁定目标标识（账号名 或 IP地址）
	 * @param userId    关联用户ID（IP维度传null）
	 * @param ip        请求IP
	 * @param userAgent 用户代理
	 */
	private void incrementFailCount(String tenantId, String target,
									Long userId, String ip, String userAgent) {
		Date now = DateUtil.now();
		int lockDuration = getLockDurationMinutes();
		Date windowStart = new Date(now.getTime() - lockDuration * 60 * 1000L);
		int threshold = getFailCountThreshold();

		// 构建滑动窗口内的计数记录查询条件
		LambdaQueryWrapper<AuthLock> windowQuery = Wrappers.<AuthLock>lambdaQuery()
			.eq(AuthLock::getTenantId, tenantId)
			.eq(AuthLock::getLockType, AuthLockType.SYSTEM)
			.eq(AuthLock::getLockStatus, AuthLockStatus.PRE_LOCKED)
			.eq(AuthLock::getLockTarget, target)
			.eq(AuthLock::getIsDeleted, 0)
			.gt(AuthLock::getLockBeginTime, windowStart);

		// 在滑动窗口内查找计数记录
		AuthLock counting = this.getOne(windowQuery.orderByDesc(AuthLock::getLockBeginTime), false);

		if (counting == null) {
			// 窗口内无记录，创建初始计数记录
			AuthLock lock = new AuthLock();
			lock.setTenantId(tenantId);
			lock.setLockType(AuthLockType.SYSTEM);
			lock.setLockTarget(target);
			lock.setRemoteIp(ip);
			lock.setUserAgent(userAgent);
			lock.setUserId(userId);
			lock.setFailCount(1);
			lock.setLockBeginTime(now);
			if (1 >= threshold) {
				lock.setLockStatus(AuthLockStatus.LOCKED);
				lock.setLockEndTime(lockExpireTime(now, lockDuration));
				lock.setLockReason(thresholdLockReason(threshold));
			} else {
				lock.setLockStatus(AuthLockStatus.PRE_LOCKED);
			}
			this.save(lock);
			// 并发防护：保存后检查是否因竞态产生了重复的 PRE_LOCKED 记录
			if (AuthLockStatus.PRE_LOCKED.equals(lock.getLockStatus())) {
				counting = deduplicateCountingRecords(tenantId, target, windowStart, lock);
			}
			// 无需继续递增（首次失败 fail_count 已为 1，或已合并到保留记录）
			if (counting == null) {
				return;
			}
		}

		// 递增计数器 + 更新元数据
		LambdaUpdateWrapper<AuthLock> increment = Wrappers.<AuthLock>lambdaUpdate()
			.set(AuthLock::getFailCount, counting.getFailCount() + 1)
			.eq(AuthLock::getId, counting.getId())
			.eq(AuthLock::getLockStatus, AuthLockStatus.PRE_LOCKED);
		if (Func.isNotEmpty(ip)) {
			increment.set(AuthLock::getRemoteIp, ip);
		}
		if (Func.isNotEmpty(userAgent)) {
			increment.set(AuthLock::getUserAgent, userAgent);
		}
		this.update(increment);
		// 达到阈值则条件升级（基于DB实际值判定，PRE_LOCKED卫兵防止并发双重升级）
		this.update(
			Wrappers.<AuthLock>lambdaUpdate()
				.set(AuthLock::getLockStatus, AuthLockStatus.LOCKED)
				.set(AuthLock::getLockEndTime, lockExpireTime(now, lockDuration))
				.set(AuthLock::getLockReason, thresholdLockReason(threshold))
				.eq(AuthLock::getId, counting.getId())
				.eq(AuthLock::getLockStatus, AuthLockStatus.PRE_LOCKED)
				.ge(AuthLock::getFailCount, threshold)
		);
	}

	/**
	 * 并发去重：检查窗口内是否存在多条 PRE_LOCKED 记录
	 * <p>
	 * 当多线程同时首次失败时可能各自创建记录，此方法保留 ID 最小的记录，
	 * 将其余记录的失败次数合并后逻辑删除，确保计数不被分散。
	 *
	 * @param tenantId    租户ID
	 * @param target      锁定目标
	 * @param windowStart 滑动窗口起始时间
	 * @param self        当前线程创建的记录
	 * @return 若发生合并则返回保留的记录（调用方需走递增路径），否则返回 null
	 */
	private AuthLock deduplicateCountingRecords(String tenantId, String target, Date windowStart, AuthLock self) {
		List<AuthLock> records = this.list(
			Wrappers.<AuthLock>lambdaQuery()
				.eq(AuthLock::getTenantId, tenantId)
				.eq(AuthLock::getLockType, AuthLockType.SYSTEM)
				.eq(AuthLock::getLockStatus, AuthLockStatus.PRE_LOCKED)
				.eq(AuthLock::getLockTarget, target)
				.eq(AuthLock::getIsDeleted, 0)
				.gt(AuthLock::getLockBeginTime, windowStart)
				.orderByAsc(AuthLock::getId)
		);
		if (records.size() <= 1) {
			return null;
		}
		// 保留 ID 最小的记录，合并其余记录的失败次数后删除
		AuthLock keeper = records.get(0);
		int totalCount = records.stream().mapToInt(AuthLock::getFailCount).sum();
		for (int i = 1; i < records.size(); i++) {
			this.removeById(records.get(i).getId());
		}
		this.update(
			Wrappers.<AuthLock>lambdaUpdate()
				.set(AuthLock::getFailCount, totalCount)
				.eq(AuthLock::getId, keeper.getId())
				.eq(AuthLock::getLockStatus, AuthLockStatus.PRE_LOCKED)
		);
		// 若自己是 keeper，无需额外递增；否则需要由调用方递增 keeper
		return self.getId().equals(keeper.getId()) ? null : keeper;
	}

	/**
	 * 检查指定字段是否存在有效锁定记录
	 * <p>
	 * 判定规则：
	 * - SYSTEM类型：lockEndTime 非空且大于当前时间
	 * - MANUAL类型：lockEndTime 为空（永久锁定）或大于当前时间
	 * - 两种类型均要求 lockBeginTime <= 当前时间（预约锁定在开始时间前不生效）
	 *
	 * @param tenantId 租户ID
	 * @param field    目标字段（lockTarget 或 remoteIp）
	 * @param value    目标值
	 * @return true-存在有效锁定
	 */
	private boolean hasActiveLock(String tenantId, SFunction<AuthLock, ?> field, String value) {
		if (Func.hasEmpty(tenantId, value)) {
			return false;
		}
		Date now = DateUtil.now();
		return this.count(
			Wrappers.<AuthLock>lambdaQuery()
				.eq(AuthLock::getTenantId, tenantId)
				.eq(AuthLock::getLockStatus, AuthLockStatus.LOCKED)
				.eq(field, value)
				.eq(AuthLock::getIsDeleted, 0)
				.le(AuthLock::getLockBeginTime, now)
				.and(outer -> outer
					.and(sys -> sys
						.eq(AuthLock::getLockType, AuthLockType.SYSTEM)
						.isNotNull(AuthLock::getLockEndTime)
						.gt(AuthLock::getLockEndTime, now)
					)
					.or(manual -> manual
						.eq(AuthLock::getLockType, AuthLockType.MANUAL)
						.and(m -> m
							.isNull(AuthLock::getLockEndTime)
							.or()
							.gt(AuthLock::getLockEndTime, now)
						)
					)
				)
		) > 0;
	}

	/**
	 * 释放指定字段对应的所有系统自动锁定记录（含计数中和已激活的）
	 * <p>
	 * 直接通过 SQL UPDATE 批量操作，避免全量加载到内存
	 *
	 * @param tenantId 租户ID
	 * @param field    目标字段（lockTarget 或 remoteIp）
	 * @param value    目标值
	 */
	private void releaseByField(String tenantId, SFunction<AuthLock, ?> field, String value) {
		if (Func.hasEmpty(tenantId, value)) {
			return;
		}
		this.update(
			Wrappers.<AuthLock>lambdaUpdate()
				.set(AuthLock::getLockStatus, AuthLockStatus.UN_LOCKED)
				.set(AuthLock::getUnlockReason, "用户认证成功，系统自动解锁")
				.eq(AuthLock::getTenantId, tenantId)
				.eq(AuthLock::getLockType, AuthLockType.SYSTEM)
				.in(AuthLock::getLockStatus, AuthLockStatus.PRE_LOCKED, AuthLockStatus.LOCKED)
				.eq(field, value)
				.eq(AuthLock::getIsDeleted, 0)
		);
	}

	/**
	 * 计算锁定到期时间
	 */
	private Date lockExpireTime(Date from, int durationMinutes) {
		return new Date(from.getTime() + durationMinutes * 60 * 1000L);
	}

	/**
	 * 生成阈值触发的锁定原因描述
	 */
	private String thresholdLockReason(int threshold) {
		return "连续认证失败次数达到阈值（" + threshold + "次），系统自动锁定";
	}

	/**
	 * 获取认证失败次数阈值
	 */
	private int getFailCountThreshold() {
		return Func.toInt(ParamCache.getValue(FAIL_COUNT_VALUE), FAIL_COUNT);
	}

	/**
	 * 获取锁定时长（分钟）
	 */
	private int getLockDurationMinutes() {
		return Func.toInt(ParamCache.getValue(LOCK_DURATION_VALUE), LOCK_DURATION);
	}

}
