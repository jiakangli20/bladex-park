/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.excel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户导入导出模型.
 *
 * @author BladeX
 */
@Data
@ColumnWidth(22)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class CustomerExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@ExcelProperty("所属园区")
	private String parkName;

	@ExcelProperty("企业名称")
	private String enterpriseName;

	@ExcelProperty("统一信用代码")
	private String creditCode;

	@ExcelProperty("联系人")
	private String contactName;

	@ExcelProperty("联系电话")
	private String contactPhone;

	@ExcelProperty("联系邮箱")
	private String contactEmail;

	@ExcelProperty("联系人职务")
	private String contactPosition;

	@ExcelProperty("成立日期")
	private Date establishDate;

	@ExcelProperty("注册资本(万元)")
	private BigDecimal registeredCapital;

	@ExcelProperty("企业类型")
	private String enterpriseType;

	@ExcelProperty("行业类型")
	private String industry;

	@ExcelProperty("企业规模")
	private String scale;

	@ExcelProperty("主营业务")
	private String mainBusiness;

	@ExcelProperty("上年度营收")
	private BigDecimal lastYearRevenue;

	@ExcelProperty("经营范围")
	private String businessScope;

	@ExcelProperty("注册地址")
	private String registeredAddress;

	@ExcelProperty("主要合作客户")
	private String majorClients;

	@ExcelProperty("意向载体类型")
	private String carrierTypes;

	@ExcelProperty("意向面积(㎡)")
	private BigDecimal intentArea;

	@ExcelProperty("使用用途")
	private String usagePurpose;

	@ExcelProperty("租赁期限(年)")
	private BigDecimal leaseTermYears;

	@ExcelProperty("装修要求")
	private String decorationRequirement;

	@ExcelProperty("配套需求")
	private String supportingNeeds;

	@ExcelProperty("招商渠道")
	private String channel;

	@ExcelProperty("第三方渠道")
	private String thirdPartyChannelName;

	@ExcelProperty("入驻状态")
	private String settlementStatusName;

	@ExcelProperty("客户状态")
	private String statusName;

	@ExcelProperty("合作等级")
	private String cooperationLevelName;

	@ExcelProperty("备注")
	private String remark;

}
