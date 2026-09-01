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
package org.springblade.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springblade.core.jwt.JwtUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.enums.StatusType;
import org.springblade.core.oauth2.provider.OAuth2Token;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.modules.auth.provider.UserType;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.ICustomerService;
import org.springblade.modules.miniapp.config.MiniAppProperties;
import org.springblade.modules.miniapp.constant.MiniAppConstant;
import org.springblade.modules.miniapp.mapper.MiniCustomerMemberMapper;
import org.springblade.modules.miniapp.mapper.MiniMemberMapper;
import org.springblade.modules.miniapp.pojo.dto.MiniBindDTO;
import org.springblade.modules.miniapp.pojo.dto.MiniRefreshDTO;
import org.springblade.modules.miniapp.pojo.dto.MiniWechatLoginDTO;
import org.springblade.modules.miniapp.pojo.dto.MiniMockLoginDTO;
import org.springblade.modules.miniapp.pojo.entity.MiniCustomerMember;
import org.springblade.modules.miniapp.pojo.entity.MiniMember;
import org.springblade.modules.miniapp.pojo.vo.MiniLoginVO;
import org.springblade.modules.miniapp.pojo.vo.MiniProfileVO;
import org.springblade.modules.miniapp.service.IMiniAuthService;
import org.springblade.modules.miniapp.service.MiniTokenIssuer;
import org.springblade.modules.miniapp.service.MiniWechatClient;
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.service.IParkService;
import org.springblade.modules.system.pojo.entity.Dept;
import org.springblade.modules.system.pojo.entity.Post;
import org.springblade.modules.system.pojo.entity.Role;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.pojo.entity.UserOauth;
import org.springblade.modules.system.service.IDeptService;
import org.springblade.modules.system.service.IPostService;
import org.springblade.modules.system.service.IRoleService;
import org.springblade.modules.system.service.IUserOauthService;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 小程序认证服务实现。
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class MiniAuthServiceImpl implements IMiniAuthService {

	private final MiniAppProperties properties;
	private final MiniWechatClient wechatClient;
	private final MiniTokenIssuer tokenIssuer;
	private final MiniMemberMapper memberMapper;
	private final MiniCustomerMemberMapper customerMemberMapper;
	private final ICustomerService customerService;
	private final IUserService userService;
	private final IUserOauthService userOauthService;
	private final IDeptService deptService;
	private final IRoleService roleService;
	private final IPostService postService;
	private final IParkService parkService;
	private final BladeRedis bladeRedis;

	@Override
	public MiniLoginVO wechatLogin(MiniWechatLoginDTO request) {
		checkRateLimit("login");
		MiniWechatClient.WechatSession wechatSession = wechatClient.exchangeCode(request.getCode());
		MiniMember member = memberMapper.selectOne(Wrappers.<MiniMember>lambdaQuery()
			.eq(MiniMember::getTenantId, properties.getDefaultTenantId())
			.eq(MiniMember::getAppId, effectiveAppId())
			.eq(MiniMember::getOpenId, wechatSession.openId())
			.eq(MiniMember::getIsDeleted, 0)
			.last("limit 1"));
		if (member == null) {
			return pendingBindLogin(wechatSession.openId(), wechatSession.unionId(), request.getNickname());
		}
		// Web 端删除用户后，历史小程序绑定可能仍是有效记录。注销脏绑定并回到手机号绑定流程。
		if (member.getUserId() == null || userService.getById(member.getUserId()) == null) {
			memberMapper.purgeDeletedByAppOpen(member.getAppId(), member.getOpenId());
			customerMemberMapper.purgeDeletedByMemberId(member.getId());
			customerMemberMapper.delete(Wrappers.<MiniCustomerMember>lambdaQuery()
				.eq(MiniCustomerMember::getMemberId, member.getId()));
			memberMapper.deleteById(member.getId());
			return pendingBindLogin(wechatSession.openId(), wechatSession.unionId(), request.getNickname());
		}
		assertMemberEnabled(member);
		synchronizeIdentity(member);
		member.setLastLoginTime(new Date());
		memberMapper.updateById(member);
		return buildLogin(member, tokenIssuer.issue(member.getTenantId(), member.getUserId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MiniLoginVO mockLogin(MiniMockLoginDTO request) {
		if (!Boolean.TRUE.equals(properties.getMockLoginEnabled())) {
			throw new ServiceException("mock 登录未启用");
		}
		String mobile = request.getMobile().trim();
		String openId = "mock-guest-" + org.springblade.core.tool.utils.DigestUtil.sha256Hex(mobile).substring(0, 24);
		MiniMember member = memberMapper.selectOne(Wrappers.<MiniMember>lambdaQuery()
			.eq(MiniMember::getTenantId, properties.getDefaultTenantId())
			.eq(MiniMember::getAppId, effectiveAppId())
			.eq(MiniMember::getOpenId, openId)
			.eq(MiniMember::getIsDeleted, 0)
			.last("limit 1"));
		if (member == null) {
			User user = createLightweightUser(properties.getDefaultTenantId(), mobile, request.getNickname());
			member = new MiniMember();
			member.setTenantId(properties.getDefaultTenantId());
			member.setAppId(effectiveAppId());
			member.setOpenId(openId);
			member.setUserId(user.getId());
			member.setParkId(effectiveDefaultParkId());
			member.setMobile(mobile);
			member.setRoleCode(MiniAppConstant.ROLE_USER);
			member.setNickname(StringUtil.isNotBlank(request.getNickname()) ? request.getNickname().trim() : "测试游客");
			member.setStatus(StatusType.ACTIVE.getType());
			member.setIsDeleted(0);
			member.setLastLoginTime(new Date());
			memberMapper.insert(member);
		}
		assertMemberEnabled(member);
		synchronizeIdentity(member);
		member.setLastLoginTime(new Date());
		memberMapper.updateById(member);
		return buildLogin(member, tokenIssuer.issue(member.getTenantId(), member.getUserId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MiniLoginVO bind(MiniBindDTO request) {
		checkRateLimit("bind");
		String ticketKey = MiniAppConstant.BIND_TICKET_PREFIX + request.getBindTicket();
		String payloadJson = bladeRedis.get(ticketKey);
		if (StringUtil.isBlank(payloadJson)) {
			throw new ServiceException("绑定票据无效或已过期，请重新登录");
		}
		BindTicketPayload payload = JsonUtil.parse(payloadJson, BindTicketPayload.class);
		String mobile = wechatClient.exchangePhone(request.getPhoneCode());
		String tenantId = properties.getDefaultTenantId();
		Long parkId = effectiveDefaultParkId();
		MiniMember duplicate = memberMapper.selectOne(Wrappers.<MiniMember>lambdaQuery()
			.eq(MiniMember::getTenantId, tenantId).eq(MiniMember::getAppId, effectiveAppId())
			.eq(MiniMember::getOpenId, payload.getOpenId()).eq(MiniMember::getIsDeleted, 0).last("limit 1"));
		if (duplicate != null) {
			throw new ServiceException("该微信已完成绑定，请直接登录");
		}
		if (StringUtil.isBlank(bladeRedis.getAndDel(ticketKey))) {
			throw new ServiceException("绑定票据已被使用或已过期，请重新登录");
		}
		String nickname = StringUtil.isNotBlank(request.getNickname()) ? request.getNickname() : payload.getNickname();
		User miniappUser = createLightweightUser(tenantId, mobile, nickname);
		String memberNickname = StringUtil.isNotBlank(nickname) ? nickname.trim() : miniappUser.getName();

		MiniMember member = new MiniMember();
		member.setTenantId(tenantId);
		member.setAppId(effectiveAppId());
		member.setOpenId(payload.getOpenId());
		member.setUnionId(payload.getUnionId());
		member.setUserId(miniappUser.getId());
		member.setCustomerId(null);
		member.setParkId(parkId);
		member.setMobile(mobile);
		member.setRoleCode(MiniAppConstant.ROLE_USER);
		member.setNickname(memberNickname);
		member.setStatus(StatusType.ACTIVE.getType());
		member.setIsDeleted(0);
		member.setLastLoginTime(new Date());
		memberMapper.insert(member);
		bindOauth(member, miniappUser);
		return buildLogin(member, tokenIssuer.issue(tenantId, miniappUser.getId()));
	}

	@Override
	public MiniLoginVO refresh(MiniRefreshDTO request) {
		checkRateLimit("refresh");
		Claims claims = JwtUtil.parseJWT(request.getRefreshToken());
		if (claims == null || !"refresh_token".equals(String.valueOf(claims.get("token_type")))) {
			throw new ServiceException("刷新令牌无效或已过期");
		}
		Long userId = Long.valueOf(String.valueOf(claims.get("user_id")));
		String tenantId = String.valueOf(claims.get("tenant_id"));
		MiniMember member = findMemberByUser(tenantId, userId);
		assertMemberEnabled(member);
		synchronizeIdentity(member);
		return buildLogin(member, tokenIssuer.refresh(request.getRefreshToken()));
	}

	@Override
	public MiniLoginVO session() {
		return buildLogin(currentMember(), null);
	}

	@Override
	public void logout() {
		// BladeX 在无状态 JWT 模式下登出由客户端清理令牌；有状态模式由框架令牌缓存控制。
	}

	@Override
	public MiniMember currentMember() {
		Long userId = AuthUtil.getUserId();
		if (userId == null || userId <= 0) {
			throw new ServiceException("登录状态无效");
		}
		MiniMember member = findMemberByUser(AuthUtil.getTenantId(), userId);
		assertMemberEnabled(member);
		return synchronizeIdentity(member);
	}

	@Override
	public MiniMember requireCustomer() {
		MiniMember member = currentMember();
		if (member.getCustomerId() == null || !Set.of(MiniAppConstant.ROLE_CUSTOMER_MEMBER,
			MiniAppConstant.ROLE_CUSTOMER_ADMIN).contains(member.getRoleCode())) {
			throw new ServiceException("当前账号没有企业客户权限");
		}
		return member;
	}

	@Override
	public MiniMember requireCustomerAdmin() {
		MiniMember member = requireCustomer();
		if (!MiniAppConstant.ROLE_CUSTOMER_ADMIN.equals(member.getRoleCode())) {
			throw new ServiceException("当前账号没有企业管理员权限");
		}
		return member;
	}

	@Override
	public MiniMember requireParkAdmin() {
		MiniMember member = currentMember();
		if (!MiniAppConstant.ROLE_PARK_ADMIN.equals(member.getRoleCode())) {
			throw new ServiceException("当前账号没有园区管理员权限");
		}
		return member;
	}

	private MiniMember findMemberByUser(String tenantId, Long userId) {
		return memberMapper.selectOne(Wrappers.<MiniMember>lambdaQuery()
			.eq(MiniMember::getTenantId, tenantId).eq(MiniMember::getAppId, effectiveAppId())
			.eq(MiniMember::getUserId, userId).eq(MiniMember::getIsDeleted, 0).last("limit 1"));
	}

	private void assertMemberEnabled(MiniMember member) {
		if (member == null) {
			throw new ServiceException("微信账号尚未绑定园区身份");
		}
		if (!Integer.valueOf(StatusType.ACTIVE.getType()).equals(member.getStatus())) {
			throw new ServiceException("账号已停用，请联系园区管理员");
		}
	}

	private MiniLoginVO pendingBindLogin(String openId, String unionId, String nickname) {
		String ticket = UUID.randomUUID().toString().replace("-", "");
		BindTicketPayload payload = new BindTicketPayload();
		payload.setOpenId(openId);
		payload.setUnionId(unionId);
		payload.setNickname(nickname);
		bladeRedis.setEx(MiniAppConstant.BIND_TICKET_PREFIX + ticket, JsonUtil.toJson(payload),
			Duration.ofMinutes(properties.getBindTicketMinutes()));
		MiniLoginVO login = new MiniLoginVO();
		login.setNeedBind(true);
		login.setBindTicket(ticket);
		login.setTenantId(properties.getDefaultTenantId());
		login.setParkId(effectiveDefaultParkId());
		return login;
	}

	private User createLightweightUser(String tenantId, String mobile, String nickname) {
		Role guestRole = roleService.getOne(Wrappers.<Role>lambdaQuery()
			.eq(Role::getTenantId, tenantId)
			.eq(Role::getRoleAlias, MiniAppConstant.WEB_ROLE_GUEST)
			.eq(Role::getStatus, StatusType.ACTIVE.getType())
			.eq(Role::getIsDeleted, 0).last("limit 1"));
		Dept guestDept = deptService.getOne(Wrappers.<Dept>lambdaQuery()
			.eq(Dept::getTenantId, tenantId)
			.eq(Dept::getDeptName, MiniAppConstant.WEB_DEPT_GUEST)
			.eq(Dept::getStatus, StatusType.ACTIVE.getType())
			.eq(Dept::getIsDeleted, 0).last("limit 1"));
		Post miniappPost = postService.getOne(Wrappers.<Post>lambdaQuery()
			.eq(Post::getTenantId, tenantId)
			.eq(Post::getPostCode, MiniAppConstant.WEB_POST_MINIAPP)
			.eq(Post::getStatus, StatusType.ACTIVE.getType())
			.eq(Post::getIsDeleted, 0).last("limit 1"));
		if (guestRole == null || guestDept == null || miniappPost == null) {
			throw new ServiceException("小程序游客身份未初始化，请先执行最新数据库迁移");
		}
		String accountSuffix = UUID.randomUUID().toString().substring(0, 6);
		String displayName = StringUtil.isNotBlank(nickname) ? nickname.trim() : guestDisplayName(accountSuffix);
		User user = new User();
		user.setTenantId(tenantId);
		user.setAccount("mini_" + mobile + "_" + accountSuffix);
		user.setPassword(UUID.randomUUID().toString());
		user.setName(truncate(displayName, 20));
		user.setRealName(truncate(displayName, 10));
		user.setPhone(isUserPhoneAvailable(tenantId, mobile) ? mobile : null);
		user.setUserType(UserType.OTHER.getCategory());
		user.setRoleId(String.valueOf(guestRole.getId()));
		user.setDeptId(String.valueOf(guestDept.getId()));
		user.setPostId(String.valueOf(miniappPost.getId()));
		user.setStatus(StatusType.ACTIVE.getType());
		user.setIsDeleted(0);
		if (!userService.submit(user)) {
			throw new ServiceException("创建小程序用户失败");
		}
		userService.updatePlatform(user.getId(), UserType.OTHER.getCategory(), "miniapp");
		return user;
	}

	private boolean isUserPhoneAvailable(String tenantId, String mobile) {
		return userService.count(Wrappers.<User>lambdaQuery()
			.eq(User::getTenantId, tenantId)
			.eq(User::getPhone, mobile)
			.eq(User::getIsDeleted, 0)) == 0;
	}

	private String truncate(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private void bindOauth(MiniMember member, User user) {
		Long count = userOauthService.count(Wrappers.<UserOauth>lambdaQuery()
			.eq(UserOauth::getUuid, member.getOpenId()).eq(UserOauth::getSource, MiniAppConstant.OAUTH_SOURCE)
			.eq(UserOauth::getIsDeleted, 0));
		if (count > 0) {
			throw new ServiceException("该微信身份已绑定其他账号");
		}
		UserOauth oauth = new UserOauth();
		oauth.setTenantId(member.getTenantId());
		oauth.setUuid(member.getOpenId());
		oauth.setUserId(user.getId());
		oauth.setUsername(user.getAccount());
		oauth.setNickname(member.getNickname());
		oauth.setSource(MiniAppConstant.OAUTH_SOURCE);
		oauth.setStatus(StatusType.ACTIVE.getType());
		oauth.setIsDeleted(0);
		userOauthService.save(oauth);
	}

	private MiniLoginVO buildLogin(MiniMember member, OAuth2Token token) {
		MiniLoginVO login = new MiniLoginVO();
		login.setNeedBind(false);
		if (token != null) {
			login.setAccessToken(token.getAccessToken());
			login.setRefreshToken(token.getRefreshToken());
			login.setExpiresIn(token.getAccessTokenExpire());
		}
		login.setTenantId(member.getTenantId());
		login.setParkId(member.getParkId());
		login.setCustomerId(member.getCustomerId());
		login.setRoleCodes(List.of(member.getRoleCode()));
		login.setCapabilities(capabilities(member.getRoleCode()));
		String subscribeTemplateId = MiniAppConstant.ROLE_PARK_ADMIN.equals(member.getRoleCode())
			? properties.getTodoReminderTemplateId()
			: Set.of(MiniAppConstant.ROLE_CUSTOMER_MEMBER, MiniAppConstant.ROLE_CUSTOMER_ADMIN).contains(member.getRoleCode())
				? properties.getServiceNoticeTemplateId() : null;
		login.setSubscribeTemplateIds(StringUtil.isBlank(subscribeTemplateId) ? List.of() : List.of(subscribeTemplateId));
		User user = normalizeGuestProfile(member, userService.getById(member.getUserId()));
		MiniProfileVO profile = new MiniProfileVO();
		profile.setUserId(member.getUserId());
		profile.setNickname(StringUtil.isNotBlank(member.getNickname()) ? member.getNickname() : user.getName());
		profile.setMobile(member.getMobile());
		profile.setAvatar(user.getAvatar());
		if (member.getCustomerId() != null) {
			Customer customer = activeCustomer(member.getCustomerId());
			profile.setEnterpriseName(customer == null ? null : customer.getEnterpriseName());
		}
		login.setProfile(profile);
		List<MiniCustomerMember> relations = customerMemberMapper.selectList(Wrappers.<MiniCustomerMember>lambdaQuery()
			.eq(MiniCustomerMember::getTenantId, member.getTenantId()).eq(MiniCustomerMember::getUserId, member.getUserId())
			.eq(MiniCustomerMember::getStatus, StatusType.ACTIVE.getType()).eq(MiniCustomerMember::getIsDeleted, 0));
		List<Map<String,Object>> enterprises = new ArrayList<>();
		for (MiniCustomerMember relation : relations) {
			Customer c = activeCustomer(relation.getCustomerId());
			Map<String,Object> item = new LinkedHashMap<>();
			item.put("enterpriseSubjectId", relation.getEnterpriseSubjectId() == null ? null : String.valueOf(relation.getEnterpriseSubjectId()));
			item.put("customerId", relation.getCustomerId() == null ? null : String.valueOf(relation.getCustomerId()));
			item.put("parkId", relation.getParkId() == null ? null : String.valueOf(relation.getParkId()));
			item.put("enterpriseName", c == null ? null : c.getEnterpriseName());
			Park park = relation.getParkId() == null ? null : parkService.getById(relation.getParkId());
			item.put("parkName", park == null ? null : park.getName());
			item.put("roleCode", relation.getRoleCode());
			enterprises.add(item);
		}
		login.setEnterprises(enterprises);
		login.setCurrentEnterpriseSubjectId(member.getCustomerId() == null ? null : relations.stream().filter(r -> Objects.equals(r.getCustomerId(), member.getCustomerId()) && Objects.equals(r.getParkId(), member.getParkId())).map(MiniCustomerMember::getEnterpriseSubjectId).findFirst().orElse(null));
		login.setCurrentParkId(member.getParkId());
		return login;
	}

	private User normalizeGuestProfile(MiniMember member, User user) {
		if (!MiniAppConstant.ROLE_USER.equals(member.getRoleCode()) || user == null) {
			return user;
		}
		String mobile = member.getMobile();
		boolean updateMemberNickname = StringUtil.isBlank(member.getNickname())
			|| Objects.equals(member.getNickname(), mobile);
		boolean updateUserName = StringUtil.isBlank(user.getName()) || Objects.equals(user.getName(), mobile);
		boolean updateRealName = StringUtil.isBlank(user.getRealName()) || Objects.equals(user.getRealName(), mobile);
		if (!updateMemberNickname && !updateUserName && !updateRealName) {
			return user;
		}
		String displayName = !updateMemberNickname ? member.getNickname().trim()
			: !updateUserName ? user.getName().trim() : guestDisplayName(accountSuffix(user));
		if (updateMemberNickname) {
			member.setNickname(displayName);
			memberMapper.updateById(member);
		}
		if (updateUserName || updateRealName) {
			User update = new User();
			update.setId(user.getId());
			if (updateUserName) {
				update.setName(truncate(displayName, 20));
				user.setName(update.getName());
			}
			if (updateRealName) {
				update.setRealName(truncate(displayName, 10));
				user.setRealName(update.getRealName());
			}
			userService.updateById(update);
		}
		return user;
	}

	private String accountSuffix(User user) {
		String account = user.getAccount();
		int separator = StringUtil.isBlank(account) ? -1 : account.lastIndexOf('_');
		if (separator >= 0 && separator < account.length() - 1) {
			return account.substring(separator + 1);
		}
		String userId = String.valueOf(user.getId());
		return userId.substring(Math.max(0, userId.length() - 6));
	}

	private String guestDisplayName(String suffix) {
		return "游客" + suffix;
	}

	private List<String> capabilities(String roleCode) {
		return switch (roleCode) {
			case MiniAppConstant.ROLE_CUSTOMER_ADMIN -> MiniAppConstant.CUSTOMER_ADMIN_CAPABILITIES;
			case MiniAppConstant.ROLE_CUSTOMER_MEMBER -> MiniAppConstant.CUSTOMER_CAPABILITIES;
			default -> Collections.emptyList();
		};
	}

	private MiniMember synchronizeIdentity(MiniMember member) {
		var query = Wrappers.<MiniCustomerMember>lambdaQuery()
			.eq(MiniCustomerMember::getTenantId, member.getTenantId())
			.eq(MiniCustomerMember::getMemberId, member.getId())
			.eq(MiniCustomerMember::getStatus, StatusType.ACTIVE.getType())
			.eq(MiniCustomerMember::getIsDeleted, 0);
		if (member.getCustomerId() != null && member.getParkId() != null) {
			query.eq(MiniCustomerMember::getCustomerId, member.getCustomerId()).eq(MiniCustomerMember::getParkId, member.getParkId());
		}
		MiniCustomerMember relation = customerMemberMapper.selectOne(query.last("limit 1"));
		Customer customer = relation == null ? null : activeCustomer(relation.getCustomerId());
		Long customerId = customer == null ? null : relation.getCustomerId();
		Long parkId = customer == null ? member.getParkId() : relation.getParkId();
		String roleCode = customer == null ? MiniAppConstant.ROLE_USER : ("OWNER".equals(relation.getRoleCode()) ? MiniAppConstant.ROLE_CUSTOMER_ADMIN : MiniAppConstant.ROLE_CUSTOMER_MEMBER);
		if (!Objects.equals(member.getCustomerId(), customerId)
			|| !Objects.equals(member.getParkId(), parkId)
			|| !Objects.equals(member.getRoleCode(), roleCode)) {
			member.setCustomerId(customerId);
			member.setParkId(parkId);
			member.setRoleCode(roleCode);
			memberMapper.updateById(member);
		}
		return member;
	}

	private Customer activeCustomer(Long customerId) {
		if (customerId == null) return null;
		return customerService.getOne(Wrappers.<Customer>lambdaQuery()
			.eq(Customer::getCustomerId, customerId)
			.eq(Customer::getStatus, "0")
			.eq(Customer::getDelFlag, "0")
			.last("limit 1"));
	}

	private void checkRateLimit(String action) {
		String key = MiniAppConstant.RATE_LIMIT_PREFIX + action + ":" + WebUtil.getIP();
		Long count = bladeRedis.incr(key);
		if (count != null && count == 1L) {
			bladeRedis.expire(key, Duration.ofMinutes(1));
		}
		if (count != null && count > properties.getRateLimitPerMinute()) {
			throw new ServiceException("操作过于频繁，请稍后再试");
		}
	}

	private String effectiveAppId() {
		return StringUtil.isNotBlank(properties.getAppId()) ? properties.getAppId() : "miniapp-dev";
	}

	private List<Long> activeParkIds() {
		return parkService.list(Wrappers.<Park>query().eq("status", "0").select("id"))
			.stream().map(Park::getId).filter(Objects::nonNull).sorted().toList();
	}

	private Long effectiveDefaultParkId() {
		List<Long> parkIds = activeParkIds();
		if (parkIds.isEmpty()) {
			throw new ServiceException("暂无启用园区，请先在后台启用园区");
		}
		Long configured = properties.getDefaultParkId();
		return configured != null && parkIds.contains(configured) ? configured : parkIds.get(0);
	}

	@Data
	public static class BindTicketPayload {
		private String openId;
		private String unionId;
		private String nickname;
	}
}
