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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Size;
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
		@NotBlank(message = "企业名称不能为空") @Size(max = 100, message = "企业名称不能超过100字") private String enterpriseName;
		@NotBlank(message = "统一社会信用代码不能为空") @Pattern(regexp = "^[0-9A-Z]{18}$", message = "统一社会信用代码格式不正确") private String creditCode;
		@NotBlank(message = "行业类型不能为空") @Size(max = 50, message = "行业类型不能超过50字") private String industryType;
		@NotBlank(message = "企业规模不能为空") @Size(max = 30, message = "企业规模不能超过30字") private String enterpriseScale;
		@NotNull(message = "意向面积不能为空") @DecimalMin(value = "1", message = "意向面积不能小于1平方米") @DecimalMax(value = "99999", message = "意向面积不能超过99999平方米") private BigDecimal intentArea;
		@NotNull(message = "预计入驻日期不能为空") @JsonFormat(pattern = "yyyy-MM-dd") private Date expectedEntryDate;
		@NotBlank(message = "联系人不能为空") @Size(max = 30, message = "联系人不能超过30字") private String contactName;
		@Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String contactPhone;
		@Size(max = 500, message = "需求说明不能超过500字") private String demandDesc;
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
	class ActivityApplication {
		@NotBlank(message = "活动标题不能为空") @Size(max = 200, message = "活动标题不能超过200字") private String title;
		@NotBlank(message = "请上传活动封面") private String coverUrl;
		@NotBlank(message = "活动简介不能为空") @Size(max = 1000, message = "活动简介不能超过1000字") private String summary;
		@NotNull(message = "活动开始时间不能为空") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date startTime;
		@NotNull(message = "活动结束时间不能为空") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date endTime;
		@NotBlank(message = "活动地点不能为空") @Size(max = 500, message = "活动地点不能超过500字") private String address;
		@Size(max = 100, message = "费用说明不能超过100字") private String priceText;
		@NotBlank(message = "联系人不能为空") @Size(max = 30, message = "联系人不能超过30字") private String contactName;
		@Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String contactPhone;
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
