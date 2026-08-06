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
package org.springblade.modules.miniapp.constant;

import java.util.List;

/**
 * 小程序常量。
 *
 * @author Chill
 */
public interface MiniAppConstant {

	String ROLE_CUSTOMER_MEMBER = "mini_customer_member";
	String ROLE_CUSTOMER_ADMIN = "mini_customer_admin";
	String ROLE_PARK_ADMIN = "mini_park_admin";
	String OAUTH_SOURCE = "WECHAT_MINI";
	String BIND_TICKET_PREFIX = "miniapp:bind:";
	String RATE_LIMIT_PREFIX = "miniapp:rate:";
	String WECHAT_TOKEN_KEY_PREFIX = "miniapp:wechat-token:";

	List<String> CUSTOMER_CAPABILITIES = List.of(
		"customer.profile.view", "customer.contract.view", "customer.bill.view",
		"customer.work-order.view", "customer.appointment.create", "customer.service.apply",
		"customer.utility.view", "customer.ad.view"
	);
	List<String> CUSTOMER_ADMIN_CAPABILITIES = List.of(
		"customer.profile.view", "customer.profile.edit", "customer.contract.view", "customer.bill.view",
		"customer.work-order.view", "customer.appointment.create", "customer.service.apply",
		"customer.member.manage", "customer.invite.manage", "customer.utility.view",
		"customer.utility.submit", "customer.ad.view", "customer.ad.submit"
	);
	List<String> PARK_ADMIN_CAPABILITIES = List.of(
		"admin.notification.view", "admin.work-order.view", "admin.work-order.handle",
		"admin.overview.view", "admin.tenant.view"
	);
}
