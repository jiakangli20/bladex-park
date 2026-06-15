package org.springblade.modules.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证锁定类型枚举
 *
 * @author BladeX
 */
@Getter
@AllArgsConstructor
public enum AuthLockType {

	/**
	 * 系统自动锁定（认证失败次数达到阈值）
	 */
	SYSTEM("SYSTEM", "系统自动"),

	/**
	 * 人工手动锁定（管理员操作）
	 */
	MANUAL("MANUAL", "人工手动");

	/**
	 * 类型编码
	 */
	@EnumValue
	@JsonValue
	private final String code;

	/**
	 * 类型描述
	 */
	private final String description;

	/**
	 * 根据编码获取枚举
	 *
	 * @param code 类型编码
	 * @return AuthLockType
	 */
	public static AuthLockType of(String code) {
		for (AuthLockType type : values()) {
			if (type.getCode().equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("未知的锁定类型: " + code);
	}

}
