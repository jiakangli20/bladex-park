/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.park.excel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 建筑导入导出模型
 *
 * @author Chill
 */
@Data
@ColumnWidth(22)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class BuildingExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@ExcelProperty("所属园区")
	private String parkName;

	@ExcelProperty("建筑名称")
	private String name;

	@ExcelProperty("建筑编码")
	private String code;

	@ExcelProperty("建筑地址")
	private String address;

	@ExcelProperty("所属地区")
	private String region;

	@ExcelProperty("不动产编号")
	private String realEstateNo;

	@ExcelProperty("产权编号")
	private String propertyNo;

	@ExcelProperty("土地编号")
	private String landNo;

	@ExcelProperty("排序值")
	private Integer sortNum;

	@ExcelProperty("楼层数")
	private Integer floors;

	@ExcelProperty("建筑面积(㎡)")
	private BigDecimal area;

	@ExcelProperty("产权面积(㎡)")
	private BigDecimal propertyArea;

	@ExcelProperty("可租面积(㎡)")
	private BigDecimal rentableArea;

	@ExcelProperty("自用面积(㎡)")
	private BigDecimal selfUseArea;

	@ExcelProperty("配套面积(㎡)")
	private BigDecimal supportingArea;

	@ExcelProperty("车位面积(㎡)")
	private BigDecimal parkingArea;

	@ExcelProperty("标准层高(m)")
	private BigDecimal standardFloorHeight;

	@ExcelProperty("产权性质")
	private String buildingType;

	@ExcelProperty("状态")
	private String status;

	@ExcelProperty("备注")
	private String memo;

}
