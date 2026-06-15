package org.springblade.modules.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证锁定状态枚举
 *
 * @author BladeX
 */
@Getter
@AllArgsConstructor
public enum AuthLockStatus {

	/**
	 * 待锁定（计数累积中，尚未达到阈值）
	 */
	PRE_LOCKED("PRE_LOCKED", "待锁定"),

	/**
	 * 锁定中（已达到失败次数阈值，锁定生效）
	 */
	LOCKED("LOCKED", "锁定中"),

	/**
	 * 已解锁
	 */
	UN_LOCKED("UN_LOCKED", "已解锁");

	/**
	 * 状态编码
	 */
	@EnumValue
	@JsonValue
	private final String code;

	/**
	 * 状态描述
	 */
	private final String description;

	/**
	 * 根据编码获取枚举
	 *
	 * @param code 状态编码
	 * @return AuthLockStatus
	 */
	public static AuthLockStatus of(String code) {
		for (AuthLockStatus status : values()) {
			if (status.getCode().equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的锁定状态: " + code);
	}

}
