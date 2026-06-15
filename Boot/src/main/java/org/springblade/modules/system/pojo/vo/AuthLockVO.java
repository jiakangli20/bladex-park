package org.springblade.modules.system.pojo.vo;

import java.io.Serial;

import org.springblade.modules.system.pojo.entity.AuthLock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 认证锁定记录视图实体类
 *
 * @author BladeX
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthLockVO extends AuthLock {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 锁定类型描述
	 */
	@Schema(description = "锁定类型描述")
	private String lockTypeName;

	/**
	 * 锁定状态描述
	 */
	@Schema(description = "锁定状态描述")
	private String lockStatusName;

	/**
	 * 关联用户姓名
	 */
	@Schema(description = "关联用户姓名")
	private String userName;

	/**
	 * 锁定是否已过期（SYSTEM类型惰性过期判定）
	 */
	@Schema(description = "锁定是否已过期")
	private Boolean expired;

}
