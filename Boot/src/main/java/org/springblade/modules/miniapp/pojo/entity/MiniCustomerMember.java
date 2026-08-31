package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 小程序企业成员关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_mini_customer_member")
public class MiniCustomerMember extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	private Long memberId;
	private Long userId;
	private Long customerId;
	/** 企业主体ID，多企业关系主维度。 */
	private Long enterpriseSubjectId;
	private Long parkId;
	private String roleCode;
	private String mobile;
	private String joinSource;
	private Long certificationId;
	private Long inviteId;
	private Date joinTime;
}
