/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.excel;

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
 * 合同导入导出模型.
 */
@Data
@ColumnWidth(22)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class ContractExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@ExcelProperty("合同编号")
	private String contractNo;

	@ExcelProperty("合同名称")
	private String contractName;

	@ExcelProperty("租客名称")
	private String customerName;

	@ExcelProperty("所属园区")
	private String parkName;

	@ExcelProperty("所属楼宇")
	private String buildingName;

	@ExcelProperty("房源名称")
	private String roomName;

	@ExcelProperty("租赁面积(㎡)")
	private BigDecimal rentArea;

	@ExcelProperty("租金单价")
	private BigDecimal rentPrice;

	@ExcelProperty("月租金")
	private BigDecimal monthlyRent;

	@ExcelProperty("物业费")
	private BigDecimal propertyFee;

	@ExcelProperty("押金")
	private BigDecimal deposit;

	@ExcelProperty("合同开始日期")
	private Date startDate;

	@ExcelProperty("合同结束日期")
	private Date endDate;

	@ExcelProperty("签约日期")
	private Date signDate;

	@ExcelProperty("缴费周期")
	private String paymentCycleName;

	@ExcelProperty("滞纳金比例")
	private BigDecimal lateFeeRatio;

	@ExcelProperty("滞纳金单位")
	private String lateFeeUnitName;

	@ExcelProperty("滞纳金上限")
	private BigDecimal lateFeeCap;

	@ExcelProperty("租金递增节点")
	private String rentIncreaseNode;

	@ExcelProperty("跟进人")
	private String followUser;

	@ExcelProperty("合同状态")
	private String contractStatusName;

	@ExcelProperty("备注")
	private String remark;

}
