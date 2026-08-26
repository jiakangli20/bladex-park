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
package org.springblade.modules.system.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springblade.core.tenant.mp.TenantEntity;
import org.springblade.core.tool.jackson.Sensitive;
import org.springblade.core.tool.sensitive.SensitiveType;

import java.io.Serial;
import java.util.Date;
import java.util.List;

/**
 * 实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_user")
@EqualsAndHashCode(callSuper = true)
public class User extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 用户编号
	 */
	private String code;
	/**
	 * 用户平台
	 */
	private Integer userType;
	/**
	 * 账号
	 */
	private String account;
	/**
	 * 密码
	 */
	private String password;
	/**
	 * 昵称
	 */
	private String name;
	/**
	 * 真名
	 */
	private String realName;
	/**
	 * 头像
	 */
	private String avatar;
	/**
	 * 邮箱
	 */
	@Sensitive(type = SensitiveType.EMAIL)
	private String email;
	/**
	 * 手机
	 */
	@Sensitive(type = SensitiveType.MOBILE)
	private String phone;
	/**
	 * 生日
	 */
	private Date birthday;
	/**
	 * 性别
	 */
	private Integer sex;
	/**
	 * 角色id
	 */
	private String roleId;
	/**
	 * 部门id
	 */
	private String deptId;
	/**
	 * 岗位id
	 */
	private String postId;
	/**
	 * 主管id
	 */
	private String leaderId;
	/**
	 * 是否主管
	 */
	private Integer isLeader;

	@TableField(exist = false)
	@Schema(description = "授权园区ID集合")
	private List<Long> parkIds;

	@TableField(exist = false)
	@Schema(description = "默认园区ID")
	private Long defaultParkId;


}
