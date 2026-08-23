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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.miniapp.config.MiniAppProperties;
import org.springblade.modules.miniapp.constant.MiniAppConstant;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信小程序服务端接口客户端。
 *
 * @author Chill
 */
@Component
@RequiredArgsConstructor
public class MiniWechatClient {

	private static final String WECHAT_API_BASE_URL = "https://api.weixin.qq.com";
	private static final String CODE_SESSION_PATH = "/sns/jscode2session";
	private static final String ACCESS_TOKEN_PATH = "/cgi-bin/token";
	private static final String PHONE_PATH = "/wxa/business/getuserphonenumber";
	private static final String SUBSCRIBE_SEND_PATH = "/cgi-bin/message/subscribe/send";

	private final MiniAppProperties properties;
	private final BladeRedis bladeRedis;
	private final RestClient restClient = RestClient.builder().baseUrl(WECHAT_API_BASE_URL).build();

	public WechatSession exchangeCode(String code) {
		if (Boolean.TRUE.equals(properties.getMockEnabled())) {
			return new WechatSession("mock-" + org.springblade.core.tool.utils.DigestUtil.sha256Hex(code).substring(0, 24), null, null);
		}
		assertWechatConfigured();
		String response;
		try {
			response = restClient.get().uri(uriBuilder -> uriBuilder.path(CODE_SESSION_PATH)
				.queryParam("appid", properties.getAppId())
				.queryParam("secret", properties.getAppSecret())
				.queryParam("js_code", code)
				.queryParam("grant_type", "authorization_code")
				.build()).retrieve().body(String.class);
		} catch (RestClientException exception) {
			throw new ServiceException("微信登录服务暂时不可用，请稍后重试");
		}
		JsonNode root = requireSuccess(response, "微信登录失败");
		String openId = root.path("openid").asText();
		if (StringUtil.isBlank(openId)) {
			throw new ServiceException("微信登录未返回用户标识");
		}
		return new WechatSession(openId, root.path("unionid").asText(null), root.path("session_key").asText(null));
	}

	public String exchangePhone(String phoneCode) {
		if (Boolean.TRUE.equals(properties.getMockEnabled())) {
			if (!phoneCode.matches("^1\\d{10}$")) {
				throw new ServiceException("开发模式请使用真实格式手机号作为 phoneCode");
			}
			return phoneCode;
		}
		assertWechatConfigured();
		String accessToken = accessToken();
		String response;
		try {
			response = restClient.post().uri(uriBuilder -> uriBuilder.path(PHONE_PATH)
				.queryParam("access_token", accessToken).build())
				.body(java.util.Map.of("code", phoneCode)).retrieve().body(String.class);
		} catch (RestClientException exception) {
			throw new ServiceException("微信手机号服务暂时不可用，请稍后重试");
		}
		JsonNode root = requireSuccess(response, "微信手机号授权失败");
		String phone = root.path("phone_info").path("purePhoneNumber").asText();
		if (StringUtil.isBlank(phone)) {
			throw new ServiceException("微信未返回手机号");
		}
		return phone;
	}

	public void sendTodoReminder(String openId, String title, String content, String page) {
		sendSubscribeMessage(properties.getTodoReminderTemplateId(), openId, title, content, page, "微信待办提醒服务暂时不可用，请稍后重试");
	}

	public void sendServiceNotice(String openId, String title, String content, String page) {
		sendSubscribeMessage(properties.getServiceNoticeTemplateId(), openId, title, content, page, "微信服务通知暂时不可用，请稍后重试");
	}

	private void sendSubscribeMessage(String templateId, String openId, String title, String content, String page, String errorMessage) {
		if (StringUtil.isBlank(templateId) || StringUtil.isBlank(openId) || Boolean.TRUE.equals(properties.getMockEnabled())) return;
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("thing1", Map.of("value", truncate(title, 20)));
		data.put("thing2", Map.of("value", truncate(content, 20)));
		data.put("time3", Map.of("value", org.springblade.core.tool.utils.DateUtil.format(new Date(), "yyyy-MM-dd HH:mm")));
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("touser", openId);
		body.put("template_id", templateId);
		body.put("page", StringUtil.isBlank(page) ? "pages/index/index" : page);
		body.put("miniprogram_state", "formal");
		body.put("lang", "zh_CN");
		body.put("data", data);
		String response;
		try {
			response = restClient.post().uri(uriBuilder -> uriBuilder.path(SUBSCRIBE_SEND_PATH)
				.queryParam("access_token", accessToken()).build()).body(body).retrieve().body(String.class);
		} catch (RestClientException exception) {
			throw new ServiceException(errorMessage);
		}
		requireSuccess(response, "微信订阅消息发送失败");
	}

	private String accessToken() {
		String cacheKey = MiniAppConstant.WECHAT_TOKEN_KEY_PREFIX + properties.getAppId();
		String cachedToken = bladeRedis.get(cacheKey);
		if (StringUtil.isNotBlank(cachedToken)) {
			return cachedToken;
		}
		String response;
		try {
			response = restClient.get().uri(uriBuilder -> uriBuilder.path(ACCESS_TOKEN_PATH)
				.queryParam("grant_type", "client_credential")
				.queryParam("appid", properties.getAppId())
				.queryParam("secret", properties.getAppSecret()).build()).retrieve().body(String.class);
		} catch (RestClientException exception) {
			throw new ServiceException("微信服务凭证暂时不可用，请稍后重试");
		}
		JsonNode root = requireSuccess(response, "微信服务凭证获取失败");
		String token = root.path("access_token").asText();
		if (StringUtil.isBlank(token)) {
			throw new ServiceException("微信服务凭证为空");
		}
		long expires = Math.max(300L, root.path("expires_in").asLong(7200L) - 300L);
		bladeRedis.setEx(cacheKey, token, Duration.ofSeconds(expires));
		return token;
	}

	private JsonNode requireSuccess(String response, String message) {
		JsonNode root = JsonUtil.readTree(response);
		if (root == null || (root.has("errcode") && root.path("errcode").asInt() != 0)) {
			String detail = root == null ? "" : root.path("errmsg").asText();
			throw new ServiceException(message + (StringUtil.isBlank(detail) ? "" : "：" + detail));
		}
		return root;
	}

	private void assertWechatConfigured() {
		if (StringUtil.isBlank(properties.getAppId()) || StringUtil.isBlank(properties.getAppSecret())) {
			throw new ServiceException("微信小程序 AppID/AppSecret 尚未配置");
		}
	}

	private String truncate(String value, int maxLength) {
		String normalized = StringUtil.isBlank(value) ? "待处理事项" : value.trim();
		return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
	}

	public record WechatSession(String openId, String unionId, String sessionKey) {
	}
}
