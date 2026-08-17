/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.common.mail;

/**
 * 邮件附件.
 *
 * @param fileName    文件名称
 * @param contentType 内容类型
 * @param content     文件内容
 */
public record MailAttachment(String fileName, String contentType, byte[] content) {
}
