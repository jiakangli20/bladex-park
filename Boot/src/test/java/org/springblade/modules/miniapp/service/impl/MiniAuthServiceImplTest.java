package org.springblade.modules.miniapp.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springblade.core.mp.enums.StatusType;
import org.springblade.core.oauth2.provider.OAuth2Token;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.ICustomerService;
import org.springblade.modules.miniapp.config.MiniAppProperties;
import org.springblade.modules.miniapp.constant.MiniAppConstant;
import org.springblade.modules.miniapp.mapper.MiniCustomerMemberMapper;
import org.springblade.modules.miniapp.mapper.MiniMemberMapper;
import org.springblade.modules.miniapp.pojo.dto.MiniBindDTO;
import org.springblade.modules.miniapp.pojo.dto.MiniWechatLoginDTO;
import org.springblade.modules.miniapp.pojo.entity.MiniCustomerMember;
import org.springblade.modules.miniapp.pojo.entity.MiniMember;
import org.springblade.modules.miniapp.pojo.vo.MiniLoginVO;
import org.springblade.modules.miniapp.service.MiniTokenIssuer;
import org.springblade.modules.miniapp.service.MiniWechatClient;
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.service.IParkService;
import org.springblade.modules.system.pojo.entity.Dept;
import org.springblade.modules.system.pojo.entity.Post;
import org.springblade.modules.system.pojo.entity.Role;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.service.IDeptService;
import org.springblade.modules.system.service.IPostService;
import org.springblade.modules.system.service.IRoleService;
import org.springblade.modules.system.service.IUserOauthService;
import org.springblade.modules.system.service.IUserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniAuthServiceImplTest {

	private final MiniAppProperties properties = properties();
	private final MiniWechatClient wechatClient = mock(MiniWechatClient.class);
	private final MiniTokenIssuer tokenIssuer = mock(MiniTokenIssuer.class);
	private final MiniMemberMapper memberMapper = mock(MiniMemberMapper.class);
	private final MiniCustomerMemberMapper customerMemberMapper = mock(MiniCustomerMemberMapper.class);
	private final ICustomerService customerService = mock(ICustomerService.class);
	private final IUserService userService = mock(IUserService.class);
	private final IUserOauthService userOauthService = mock(IUserOauthService.class);
	private final IDeptService deptService = mock(IDeptService.class);
	private final IRoleService roleService = mock(IRoleService.class);
	private final IPostService postService = mock(IPostService.class);
	private final IParkService parkService = mock(IParkService.class);
	private final BladeRedis bladeRedis = mock(BladeRedis.class);
	private final MiniAuthServiceImpl service = new MiniAuthServiceImpl(
		properties, wechatClient, tokenIssuer, memberMapper, customerMemberMapper,
		customerService, userService, userOauthService, deptService, roleService,
		postService, parkService, bladeRedis);

	@Test
	void existingEnterpriseMemberCanLoginBeforeRequestHasBearerToken() {
		MiniMember member = member();
		member.setRoleCode(MiniAppConstant.ROLE_CUSTOMER_ADMIN);
		member.setCustomerId(9L);
		MiniCustomerMember relation = new MiniCustomerMember();
		relation.setMemberId(member.getId());
		relation.setCustomerId(9L);
		relation.setParkId(5L);
		Customer customer = new Customer();
		customer.setCustomerId(9L);
		customer.setEnterpriseName("测试企业");
		User user = user(member.getUserId());

		when(bladeRedis.incr(anyString())).thenReturn(1L);
		when(wechatClient.exchangeCode("wechat-code"))
			.thenReturn(new MiniWechatClient.WechatSession("open-id", null, "session-key"));
		when(memberMapper.selectOne(any())).thenReturn(member);
		when(customerMemberMapper.selectOne(any())).thenReturn(relation);
		when(customerService.getOne(any())).thenReturn(customer);
		when(userService.getById(member.getUserId())).thenReturn(user);
		when(tokenIssuer.issue(member.getTenantId(), member.getUserId())).thenReturn(token());

		MiniWechatLoginDTO request = new MiniWechatLoginDTO();
		request.setCode("wechat-code");
		MiniLoginVO login = service.wechatLogin(request);

		assertFalse(login.getNeedBind());
		assertEquals(List.of(MiniAppConstant.ROLE_CUSTOMER_MEMBER), login.getRoleCodes());
		assertEquals("测试企业", login.getProfile().getEnterpriseName());
		assertFalse(login.getCapabilities().contains("customer.member.manage"));
		verify(customerService, never()).selectCustomerById(any());
	}

	@Test
	void phoneBindingAlwaysCreatesIndependentGuestAccount() {
		String ticket = "bind-ticket";
		MiniAuthServiceImpl.BindTicketPayload payload = new MiniAuthServiceImpl.BindTicketPayload();
		payload.setOpenId("new-open-id");
		String payloadJson = JsonUtil.toJson(payload);
		Role role = new Role();
		role.setId(101L);
		Dept dept = new Dept();
		dept.setId(102L);
		Post post = new Post();
		post.setId(103L);
		Park park = new Park();
		park.setId(5L);
		User savedUser = user(201L);

		when(bladeRedis.incr(anyString())).thenReturn(1L);
		when(bladeRedis.<String>get(MiniAppConstant.BIND_TICKET_PREFIX + ticket)).thenReturn(payloadJson);
		when(bladeRedis.<String>getAndDel(MiniAppConstant.BIND_TICKET_PREFIX + ticket)).thenReturn(payloadJson);
		when(wechatClient.exchangePhone("phone-code")).thenReturn("13862061912");
		when(memberMapper.selectOne(any())).thenReturn(null);
		when(roleService.getOne(any())).thenReturn(role);
		when(deptService.getOne(any())).thenReturn(dept);
		when(postService.getOne(any())).thenReturn(post);
		when(parkService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(park));
		when(userService.count(any())).thenReturn(1L);
		when(userService.submit(any())).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(savedUser.getId());
			return true;
		});
		when(userService.updatePlatform(savedUser.getId(), 3, "miniapp")).thenReturn(true);
		when(userOauthService.count(any())).thenReturn(0L);
		when(userOauthService.save(any())).thenReturn(true);
		when(userService.getById(savedUser.getId())).thenReturn(savedUser);
		when(tokenIssuer.issue("000000", savedUser.getId())).thenReturn(token());

		MiniBindDTO request = new MiniBindDTO();
		request.setBindTicket(ticket);
		request.setPhoneCode("phone-code");
		MiniLoginVO login = service.bind(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userService).submit(userCaptor.capture());
		User created = userCaptor.getValue();
		assertNull(created.getPhone(), "后台已有相同手机号时，独立小程序账号不占用该手机号");
		assertTrue(created.getName().matches("游客[0-9a-f]{6}"));
		assertEquals(created.getName(), created.getRealName());
		assertEquals("101", created.getRoleId());
		assertEquals("102", created.getDeptId());
		assertEquals("103", created.getPostId());
		assertNull(created.getParkIds());

		ArgumentCaptor<MiniMember> memberCaptor = ArgumentCaptor.forClass(MiniMember.class);
		verify(memberMapper).insert(memberCaptor.capture());
		MiniMember createdMember = memberCaptor.getValue();
		assertEquals("13862061912", createdMember.getMobile());
		assertEquals(created.getName(), createdMember.getNickname());
		assertNull(createdMember.getCustomerId());
		assertEquals(MiniAppConstant.ROLE_USER, createdMember.getRoleCode());
		assertTrue(login.getCapabilities().isEmpty());
		verify(customerService, never()).selectCustomerList(any());
	}

	@Test
	void existingGuestPhoneNameIsReplacedOnLogin() {
		MiniMember member = member();
		member.setRoleCode(MiniAppConstant.ROLE_USER);
		member.setNickname(null);
		User user = user(member.getUserId());
		user.setAccount("mini_13862061912_4ce818");
		user.setName(member.getMobile());
		user.setRealName(member.getMobile());

		when(bladeRedis.incr(anyString())).thenReturn(1L);
		when(wechatClient.exchangeCode("wechat-code"))
			.thenReturn(new MiniWechatClient.WechatSession("open-id", null, "session-key"));
		when(memberMapper.selectOne(any())).thenReturn(member);
		when(customerMemberMapper.selectOne(any())).thenReturn(null);
		when(userService.getById(member.getUserId())).thenReturn(user);
		when(tokenIssuer.issue(member.getTenantId(), member.getUserId())).thenReturn(token());

		MiniWechatLoginDTO request = new MiniWechatLoginDTO();
		request.setCode("wechat-code");
		MiniLoginVO login = service.wechatLogin(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userService).updateById(userCaptor.capture());
		assertEquals("游客4ce818", userCaptor.getValue().getName());
		assertEquals("游客4ce818", userCaptor.getValue().getRealName());
		assertEquals("游客4ce818", member.getNickname());
		assertEquals("游客4ce818", login.getProfile().getNickname());
	}

	@Test
	void deletedWebUserReturnsToPhoneBindingInsteadOfIssuingToken() {
		MiniMember member = member();
		Park park = new Park();
		park.setId(5L);

		when(bladeRedis.incr(anyString())).thenReturn(1L);
		when(wechatClient.exchangeCode("wechat-code"))
			.thenReturn(new MiniWechatClient.WechatSession("open-id", null, "session-key"));
		when(memberMapper.selectOne(any())).thenReturn(member);
		when(userService.getById(member.getUserId())).thenReturn(null);
		when(parkService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(park));

		MiniWechatLoginDTO request = new MiniWechatLoginDTO();
		request.setCode("wechat-code");
		MiniLoginVO login = service.wechatLogin(request);

		assertTrue(login.getNeedBind());
		assertTrue(login.getBindTicket() != null && !login.getBindTicket().isBlank());
		verify(customerMemberMapper).delete(any());
		verify(memberMapper).deleteById(member.getId());
		verify(tokenIssuer, never()).issue(anyString(), any());
	}

	private MiniMember member() {
		MiniMember member = new MiniMember();
		member.setId(10L);
		member.setTenantId("000000");
		member.setAppId("wx43451045973e460f");
		member.setOpenId("open-id");
		member.setUserId(20L);
		member.setParkId(5L);
		member.setMobile("13862061912");
		member.setNickname("测试用户");
		member.setStatus(StatusType.ACTIVE.getType());
		member.setIsDeleted(0);
		return member;
	}

	private User user(Long id) {
		User user = new User();
		user.setId(id);
		user.setName("测试用户");
		return user;
	}

	private OAuth2Token token() {
		return OAuth2Token.create().setAccessToken("access-token").setRefreshToken("refresh-token")
			.setAccessTokenExpire(3600);
	}

	private MiniAppProperties properties() {
		MiniAppProperties value = new MiniAppProperties();
		value.setAppId("wx43451045973e460f");
		value.setDefaultTenantId("000000");
		value.setDefaultParkId(5L);
		value.setRateLimitPerMinute(30);
		return value;
	}
}
