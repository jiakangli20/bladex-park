/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.common.mail.MailCredentialCipher;
import org.springblade.common.mail.MailSenderAccount;
import org.springblade.common.mail.MailService;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.system.mapper.UserMailAccountMapper;
import org.springblade.modules.system.pojo.dto.UserMailAccountDTO;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.pojo.entity.UserMailAccount;
import org.springblade.modules.system.pojo.vo.UserMailAccountVO;
import org.springblade.modules.system.service.IUserMailAccountService;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 用户邮箱绑定服务实现.
 */
@Service
@RequiredArgsConstructor
public class UserMailAccountServiceImpl implements IUserMailAccountService {

	private static final String ENABLED = "1";
	private static final String NOT_DELETED = "0";
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@qq\\.com$", Pattern.CASE_INSENSITIVE);

	private final UserMailAccountMapper userMailAccountMapper;
	private final IUserService userService;
	private final MailCredentialCipher credentialCipher;
	private final MailService mailService;

	@Override
	public UserMailAccountVO getCurrent() {
		return toVO(findCurrent());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public UserMailAccountVO saveCurrent(UserMailAccountDTO request) {
		validateRequest(request);
		UserMailAccount account = findCurrent();
		String emailAddress = request.getEmailAddress().trim().toLowerCase();
		boolean emailChanged = account != null && !emailAddress.equalsIgnoreCase(account.getEmailAddress());
		Date now = DateUtil.now();
		if (account == null) {
			if (StringUtil.isBlank(request.getAuthCode())) {
				throw new ServiceException("请输入QQ邮箱SMTP授权码");
			}
			account = new UserMailAccount();
			account.setTenantId(AuthUtil.getTenantId());
			account.setUserId(AuthUtil.getUserId());
			account.setDelFlag(NOT_DELETED);
			account.setCreateBy(currentUserName());
			account.setCreateTime(now);
		}
		if (emailChanged && StringUtil.isBlank(request.getAuthCode())) {
			throw new ServiceException("修改发件邮箱时请重新填写QQ邮箱SMTP授权码");
		}
		account.setEmailAddress(emailAddress);
		if (!StringUtil.isBlank(request.getAuthCode())) {
			account.setAuthCodeCiphertext(credentialCipher.encrypt(request.getAuthCode().trim()));
		}
		if (emailChanged || !StringUtil.isBlank(request.getAuthCode())) {
			account.setLastTestStatus(null);
			account.setLastTestMessage(null);
			account.setLastTestTime(null);
		}
		account.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? "0" : ENABLED);
		account.setUpdateBy(currentUserName());
		account.setUpdateTime(now);
		if (account.getAccountId() == null) {
			userMailAccountMapper.insert(account);
		} else {
			userMailAccountMapper.updateById(account);
		}
		return toVO(account);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public UserMailAccountVO testCurrent(UserMailAccountDTO request) {
		UserMailAccount saved = findCurrent();
		String savedAuthCode = saved == null ? null : credentialCipher.decrypt(saved.getAuthCodeCiphertext());
		String address = request != null && !StringUtil.isBlank(request.getEmailAddress())
			? request.getEmailAddress().trim().toLowerCase()
			: saved == null ? null : saved.getEmailAddress();
		String authCode = request != null && !StringUtil.isBlank(request.getAuthCode())
			? request.getAuthCode().trim()
			: savedAuthCode;
		validateEmail(address);
		Date now = DateUtil.now();
		String status;
		String message;
		try {
			mailService.testConnection(new MailSenderAccount(address, authCode, currentUserName()));
			status = "success";
			message = "连接测试成功";
		} catch (ServiceException exception) {
			status = "failed";
			message = exception.getMessage();
		}
		boolean testedSavedCredentials = saved != null
			&& address.equalsIgnoreCase(saved.getEmailAddress())
			&& Objects.equals(authCode, savedAuthCode);
		if (testedSavedCredentials) {
			saved.setLastTestStatus(status);
			saved.setLastTestMessage(message);
			saved.setLastTestTime(now);
			saved.setUpdateBy(currentUserName());
			saved.setUpdateTime(now);
			userMailAccountMapper.updateById(saved);
		}
		UserMailAccountVO result = saved == null ? new UserMailAccountVO() : toVO(saved);
		result.setEmailAddress(address);
		result.setAuthCodeConfigured(!StringUtil.isBlank(authCode));
		result.setLastTestStatus(status);
		result.setLastTestMessage(message);
		result.setLastTestTime(now);
		return result;
	}

	@Override
	public MailSenderAccount requireCurrentSender() {
		UserMailAccount account = findCurrent();
		if (account == null || !ENABLED.equals(account.getEnabled())) {
			throw new ServiceException("请先在个人中心绑定并启用QQ邮箱");
		}
		return new MailSenderAccount(account.getEmailAddress(), credentialCipher.decrypt(account.getAuthCodeCiphertext()), currentUserName());
	}

	private UserMailAccount findCurrent() {
		Long userId = AuthUtil.getUserId();
		if (userId == null) {
			throw new ServiceException("登录状态已失效");
		}
		return userMailAccountMapper.selectOne(Wrappers.<UserMailAccount>lambdaQuery()
			.eq(UserMailAccount::getTenantId, AuthUtil.getTenantId())
			.eq(UserMailAccount::getUserId, userId)
			.eq(UserMailAccount::getDelFlag, NOT_DELETED)
			.last("LIMIT 1"));
	}

	private void validateRequest(UserMailAccountDTO request) {
		if (request == null) {
			throw new ServiceException("邮箱配置不能为空");
		}
		validateEmail(request.getEmailAddress());
	}

	private void validateEmail(String email) {
		if (StringUtil.isBlank(email) || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
			throw new ServiceException("发件邮箱必须为有效的QQ邮箱");
		}
	}

	private UserMailAccountVO toVO(UserMailAccount account) {
		UserMailAccountVO vo = new UserMailAccountVO();
		if (account == null) {
			vo.setAuthCodeConfigured(false);
			vo.setEnabled(true);
			return vo;
		}
		vo.setEmailAddress(account.getEmailAddress());
		vo.setAuthCodeConfigured(!StringUtil.isBlank(account.getAuthCodeCiphertext()));
		vo.setEnabled(ENABLED.equals(account.getEnabled()));
		vo.setLastTestStatus(account.getLastTestStatus());
		vo.setLastTestMessage(account.getLastTestMessage());
		vo.setLastTestTime(account.getLastTestTime());
		return vo;
	}

	private String currentUserName() {
		User user = userService.getById(AuthUtil.getUserId());
		if (user != null && !StringUtil.isBlank(user.getRealName())) {
			return user.getRealName();
		}
		return StringUtil.isBlank(AuthUtil.getUserName()) ? "系统用户" : AuthUtil.getUserName();
	}

}
