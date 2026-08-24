package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_park_activity_audit_log")
public class ParkActivityAuditLog extends TenantEntity {
	@Serial
	private static final long serialVersionUID = 1L;
	private Long activityId;
	private Long parkId;
	private Long customerId;
	private String actionType;
	private String beforeAuditStatus;
	private String afterAuditStatus;
	private Integer beforePublishStatus;
	private Integer afterPublishStatus;
	private Long operatorUserId;
	private String operatorName;
	private String opinion;
	private Date operateTime;
}
