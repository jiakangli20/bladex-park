/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.common.mail;

/**
 * 邮件发件账号.
 *
 * @param address  发件邮箱
 * @param authCode SMTP授权码
 * @param name     发件人名称
 */
public record MailSenderAccount(String address, String authCode, String name) {
}
