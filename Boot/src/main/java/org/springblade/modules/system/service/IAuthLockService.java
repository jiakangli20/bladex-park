package org.springblade.modules.system.service;

import java.util.Date;

import org.springblade.modules.system.pojo.entity.AuthLock;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 认证锁定记录 服务类
 *
 * @author BladeX
 */
public interface IAuthLockService extends IService<AuthLock> {

	/**
	 * 增加账号认证失败次数，同时记录IP和用户代理
	 * 若达到阈值则自动激活锁定
	 *
	 * @param tenantId  租户ID
	 * @param account   账号
	 * @param userId    关联用户ID（可为空）
	 * @param ip        请求IP
	 * @param userAgent 用户代理
	 */
	void addAccountFailCount(String tenantId, String account, Long userId, String ip, String userAgent);

	/**
	 * 增加IP认证失败次数
	 * 独立于账号维度计数，可防止攻击者轮换账号绕过锁定
	 * 若达到阈值则自动激活IP锁定
	 *
	 * @param tenantId  租户ID
	 * @param ip        IP地址
	 * @param userAgent 用户代理
	 */
	void addIpFailCount(String tenantId, String ip, String userAgent);

	/**
	 * 检查账号是否处于锁定状态
	 *
	 * @param tenantId 租户ID
	 * @param account  账号
	 * @return true-已锁定 false-未锁定
	 */
	boolean isAccountLocked(String tenantId, String account);

	/**
	 * 检查IP是否处于锁定状态
	 *
	 * @param tenantId 租户ID
	 * @param ip       IP地址
	 * @return true-已锁定 false-未锁定
	 */
	boolean isIpLocked(String tenantId, String ip);

	/**
	 * 认证成功时释放系统自动锁定（含计数记录）
	 *
	 * @param tenantId 租户ID
	 * @param account  账号
	 */
	void releaseSystemLock(String tenantId, String account);

	/**
	 * 认证成功时释放IP的系统自动锁定（含计数记录）
	 *
	 * @param tenantId 租户ID
	 * @param ip       IP地址
	 */
	void releaseSystemIpLock(String tenantId, String ip);

	/**
	 * 管理员手动锁定用户
	 *
	 * @param userId    用户ID
	 * @param reason    锁定原因
	 * @param beginTime 锁定开始时间
	 * @param endTime   锁定结束时间（为空表示永久锁定）
	 * @return 是否成功
	 */
	boolean manualLock(Long userId, String reason, Date beginTime, Date endTime);

	/**
	 * 管理员手动解锁
	 *
	 * @param lockId       锁定记录ID
	 * @param unlockReason 解锁原因
	 * @return 是否成功
	 */
	boolean manualUnlock(Long lockId, String unlockReason);

}
