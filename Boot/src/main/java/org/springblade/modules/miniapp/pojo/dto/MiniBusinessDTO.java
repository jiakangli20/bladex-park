/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement.
 * <p>
 * Redistribution of this software's source code to any third party without
 * a commercial license is strictly prohibited.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 小程序业务写入 DTO 集合。
 *
 * @author Chill
 */
public interface MiniBusinessDTO {

	@Data
	class Appointment {
		@NotNull(message = "房源不能为空") private Long roomId;
		@NotBlank(message = "企业名称不能为空") private String enterpriseName;
		@NotBlank(message = "联系人不能为空") private String contactName;
		@Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String contactPhone;
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date preferredTime;
		private String demandDesc;
	}

	@Data
	class Settlement {
		private Long roomId;
		@NotBlank(message = "企业名称不能为空") private String enterpriseName;
		@NotBlank(message = "统一社会信用代码不能为空") private String creditCode;
		private String industryType;
		private String enterpriseScale;
		private BigDecimal intentArea;
		@JsonFormat(pattern = "yyyy-MM-dd") private Date expectedEntryDate;
		@NotBlank(message = "联系人不能为空") private String contactName;
		@Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String contactPhone;
		private String demandDesc;
	}

	@Data
	class PropertyOrder {
		@NotNull(message = "物业服务不能为空") private Long serviceId;
		@NotBlank(message = "联系人不能为空") private String contactName;
		@Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String contactPhone;
		private String roomIds;
		private String roomInfo;
		@NotBlank(message = "需求描述不能为空") private String demandDesc;
		private String demandImages;
		private String priority;
	}

	@Data
	class ValueOrder {
		@NotNull(message = "服务商不能为空") private Long merchantId;
		@NotBlank(message = "服务类型不能为空") private String serviceType;
		@NotBlank(message = "联系人不能为空") private String contactName;
		@Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String contactPhone;
		private String serviceScope;
		@NotBlank(message = "需求描述不能为空") private String demandDesc;
		private String demandImages;
	}

	@Data
	class Company {
		@NotBlank(message = "企业名称不能为空") private String enterpriseName;
		private String industry;
		private String scale;
		@NotBlank(message = "联系人不能为空") private String contactName;
		@Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String contactPhone;
		private String contactEmail;
		private String address;
		private String businessScope;
	}

	@Data
	class Invite {
		private String mobile;
		private String roleCode;
		@Min(value = 1, message = "有效期至少1小时") @Max(value = 720, message = "有效期最长720小时")
		private Integer validHours = 72;
		@Min(value = 1, message = "使用次数至少1次") @Max(value = 100, message = "使用次数最多100次")
		private Integer maxUses = 1;
	}

	@Data
	class CustomerAction {
		@NotBlank(message = "操作不能为空") private String action;
		private String reason;
		@Min(value = 1, message = "评分范围为1-5") @Max(value = 5, message = "评分范围为1-5") private Integer rating;
		private String content;
	}

	@Data
	class AdminAction {
		@NotBlank(message = "操作不能为空") private String action;
		private String assignee;
		private String content;
		private String reason;
		private BigDecimal dealAmount;
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date nextFollowTime;
	}

	@Data
	class PaymentSubmission {
		@NotNull(message = "付款金额不能为空")
		@DecimalMin(value = "0.01", message = "付款金额必须大于0")
		private BigDecimal submitAmount;
		@NotBlank(message = "凭证名称不能为空") private String voucherName;
		@NotBlank(message = "请上传付款凭证") private String voucherUrl;
	}

	@Data
	class CustomerAd {
		@NotBlank(message = "广告标题不能为空") private String adTitle;
		@NotBlank(message = "请上传广告封面") private String coverUrl;
		private String linkType;
		private String linkUrl;
		private Long merchantId;
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date startTime;
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date endTime;
		private String remark;
	}
}
