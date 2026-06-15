/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.auth.handler;

import org.springblade.common.cache.UserCache;
import org.springblade.core.oauth2.exception.ExceptionCode;
import org.springblade.core.oauth2.provider.OAuth2Validation;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.service.IAuthLockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * 失败锁定处理器
 * 统一管理账号锁定和IP锁定逻辑
 *
 * @author BladeX
 */
@Slf4j
@RequiredArgsConstructor
public class BladeLockHandler {

	private final IAuthLockService authLockService;

	/**
	 * 校验账号是否锁定
	 *
	 * @param tenantId 租户ID
	 * @param account  账号
	 * @return OAuth2Validation
	 */
	public OAuth2Validation validateAccountLock(String tenantId, String account) {
		try {
			if (authLockService.isAccountLocked(tenantId, account)) {
				log.error("用户：{}，已锁定，请求IP：{}", account, WebUtil.getIP());
				return buildValidationFailure();
			}
		} catch (Exception e) {
			log.error("账号锁定校验异常，降级放行：{}", e.getMessage(), e);
		}
		return new OAuth2Validation();
	}

	/**
	 * 校验IP是否锁定
	 *
	 * @param tenantId 租户ID
	 * @return OAuth2Validation
	 */
	public OAuth2Validation validateIpLock(String tenantId) {
		try {
			String clientIp = WebUtil.getIP();
			if (authLockService.isIpLocked(tenantId, clientIp)) {
				log.error("IP：{}，已锁定", clientIp);
				return buildValidationFailure();
			}
		} catch (Exception e) {
			log.error("IP锁定校验异常，降级放行：{}", e.getMessage(), e);
		}
		return new OAuth2Validation();
	}

	/**
	 * 处理认证失败
	 * 自动通过缓存查询用户ID后委托给含userId的重载方法
	 *
	 * @param tenantId 租户ID
	 * @param account  账号
	 */
	public void handleAuthFailure(String tenantId, String account) {
		User user = UserCache.getUser(tenantId, account);
		handleAuthFailure(tenantId, account, Func.isEmpty(user) ? null : user.getId());
	}

	/**
	 * 处理认证失败（含用户ID）
	 *
	 * @param tenantId 租户ID
	 * @param account  账号
	 * @param userId   用户ID（可为空）
	 */
	public void handleAuthFailure(String tenantId, String account, Long userId) {
		String ip = WebUtil.getIP();
		String userAgent = WebUtil.getUserAgent();
		CompletableFuture.runAsync(() -> {
			try {
				// 账号维度计数
				authLockService.addAccountFailCount(tenantId, account, userId, ip, userAgent);
				// IP 维度独立计数（防止攻击者轮换账号绕过锁定）
				authLockService.addIpFailCount(tenantId, ip, userAgent);
			} catch (Exception e) {
				log.error("记录认证失败信息异常：{}", e.getMessage(), e);
			}
		});
	}

	/**
	 * 处理认证成功
	 * 释放账号和IP的系统自动锁定记录（含计数记录）
	 *
	 * @param tenantId 租户ID
	 * @param account  账号
	 */
	public void handleAuthSuccess(String tenantId, String account) {
		String ip = WebUtil.getIP();
		CompletableFuture.runAsync(() -> {
			try {
				authLockService.releaseSystemLock(tenantId, account);
				authLockService.releaseSystemIpLock(tenantId, ip);
			} catch (Exception e) {
				log.error("释放系统锁定记录异常：{}", e.getMessage(), e);
			}
		});
	}

	/**
	 * 构建校验失败结果
	 *
	 * @return OAuth2Validation
	 */
	private OAuth2Validation buildValidationFailure() {
		OAuth2Validation validation = new OAuth2Validation();
		validation.setSuccess(false);
		validation.setCode(ExceptionCode.USER_TOO_MANY_FAILS.getCode());
		validation.setMessage(ExceptionCode.USER_TOO_MANY_FAILS.getMessage());
		return validation;
	}

}
