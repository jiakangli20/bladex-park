/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * Redistribution of this software's source code to any third party without
 * a commercial license is strictly prohibited.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.service;

import lombok.RequiredArgsConstructor;
import org.springblade.core.oauth2.granter.TokenGranter;
import org.springblade.core.oauth2.granter.TokenGranterFactory;
import org.springblade.core.oauth2.handler.TokenHandler;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.provider.OAuth2Token;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.oauth2.service.OAuth2UserService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.auth.provider.UserType;
import org.springblade.modules.miniapp.config.MiniAppProperties;
import org.springframework.stereotype.Component;

/**
 * 标准 BladeX JWT 签发器。
 *
 * @author Chill
 */
@Component
@RequiredArgsConstructor
public class MiniTokenIssuer {

	private final TokenGranterFactory granterFactory;
	private final OAuth2UserService userService;
	private final TokenHandler tokenHandler;
	private final MiniAppProperties properties;

	public OAuth2Token issue(String tenantId, Long userId) {
		OAuth2Request request = request(tenantId, "password");
		OAuth2User user = userService.loadByUserId(String.valueOf(userId), request);
		if (user == null) {
			throw new org.springblade.core.log.exception.ServiceException("绑定用户不存在或已停用");
		}
		TokenGranter granter = granterFactory.create("password");
		user.setClient(granter.client(request));
		OAuth2Token token = granter.token(user, request);
		return tokenHandler.enhance(user, token, request);
	}

	public OAuth2Token refresh(String refreshToken) {
		io.jsonwebtoken.Claims claims = org.springblade.core.jwt.JwtUtil.parseJWT(refreshToken);
		if (claims == null) {
			throw new org.springblade.core.log.exception.ServiceException("刷新令牌无效或已过期");
		}
		String tenantId = String.valueOf(claims.get("tenant_id"));
		OAuth2Request request = request(tenantId, "refresh_token");
		request.getParameterArgs().set("refresh_token", refreshToken);
		TokenGranter granter = granterFactory.create("refresh_token");
		OAuth2User user = granter.user(request);
		OAuth2Token token = granter.token(user, request);
		return tokenHandler.enhance(user, token, request);
	}

	private OAuth2Request request(String tenantId, String grantType) {
		OAuth2Request request = OAuth2Request.create();
		request.setTenantId(tenantId);
		request.setClientArgs(Kv.create()
			.set("client_id", properties.getOauthClientId())
			.set("client_secret", properties.getOauthClientSecret()));
		request.setHeaderArgs(Kv.create().set("User-Type", UserType.OTHER.getName()));
		request.setParameterArgs(Kv.create()
			.set("grant_type", grantType));
		return request;
	}
}
