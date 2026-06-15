package org.springblade.modules.system.pojo.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import org.springblade.modules.system.enums.AuthLockStatus;
import org.springblade.modules.system.enums.AuthLockType;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

/**
 * 认证锁定记录实体类
 *
 * @author BladeX
 */
@Data
@TableName("blade_auth_lock")
public class AuthLock implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	/**
	 * 租户ID
	 */
	private String tenantId;

	/**
	 * 锁定类型（SYSTEM-系统自动/MANUAL-人工手动）
	 */
	private AuthLockType lockType;

	/**
	 * 锁定状态（PRE_LOCKED-待锁定/LOCKED-锁定中/UN_LOCKED-已解锁）
	 */
	private AuthLockStatus lockStatus;

	/**
	 * 锁定目标（账号名或IP地址）
	 */
	private String lockTarget;

	/**
	 * 请求IP
	 */
	private String remoteIp;

	/**
	 * 用户代理
	 */
	private String userAgent;

	/**
	 * 关联用户ID
	 */
	private Long userId;

	/**
	 * 锁定开始时间
	 */
	private Date lockBeginTime;

	/**
	 * 锁定结束时间
	 */
	private Date lockEndTime;

	/**
	 * 锁定原因
	 */
	private String lockReason;

	/**
	 * 解锁原因
	 */
	private String unlockReason;

	/**
	 * 认证失败次数
	 */
	private Integer failCount;

	/**
	 * 状态
	 */
	private Integer status;

	/**
	 * 是否已删除
	 */
	@TableLogic
	private Integer isDeleted;

}
