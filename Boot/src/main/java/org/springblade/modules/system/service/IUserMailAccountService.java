/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.system.service;

import org.springblade.common.mail.MailSenderAccount;
import org.springblade.modules.system.pojo.dto.UserMailAccountDTO;
import org.springblade.modules.system.pojo.vo.UserMailAccountVO;

/**
 * 用户邮箱绑定服务.
 */
public interface IUserMailAccountService {

	UserMailAccountVO getCurrent();

	UserMailAccountVO saveCurrent(UserMailAccountDTO request);

	UserMailAccountVO testCurrent(UserMailAccountDTO request);

	MailSenderAccount requireCurrentSender();

}
