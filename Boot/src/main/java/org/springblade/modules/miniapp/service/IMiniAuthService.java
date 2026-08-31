/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.service;

import org.springblade.modules.miniapp.pojo.dto.MiniBindDTO;
import org.springblade.modules.miniapp.pojo.dto.MiniRefreshDTO;
import org.springblade.modules.miniapp.pojo.dto.MiniWechatLoginDTO;
import org.springblade.modules.miniapp.pojo.dto.MiniMockLoginDTO;
import org.springblade.modules.miniapp.pojo.entity.MiniMember;
import org.springblade.modules.miniapp.pojo.vo.MiniLoginVO;

/**
 * 小程序认证服务。
 *
 * @author Chill
 */
public interface IMiniAuthService {
	MiniLoginVO wechatLogin(MiniWechatLoginDTO request);
	MiniLoginVO mockLogin(MiniMockLoginDTO request);
	MiniLoginVO bind(MiniBindDTO request);
	MiniLoginVO refresh(MiniRefreshDTO request);
	MiniLoginVO session();
	void logout();
	MiniMember currentMember();
	MiniMember requireCustomer();
	MiniMember requireCustomerAdmin();
	MiniMember requireParkAdmin();
}
