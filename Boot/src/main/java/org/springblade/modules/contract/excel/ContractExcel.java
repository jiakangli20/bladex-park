package org.springblade.modules.contract.excel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 租赁信息表导入模型，一行对应一个房间。 */
@Data
@ColumnWidth(22)
@HeadRowHeight(42)
@ContentRowHeight(18)
public class ContractExcel implements Serializable {
	@Serial private static final long serialVersionUID = 1L;
	@ExcelProperty("楼层") private String floor;
	@ExcelProperty("所属园区") private String parkName;
	@ExcelProperty("所属楼宇") private String buildingName;
	@ExcelProperty("客户") private String customerName;
	@ExcelProperty("房间号") private String roomName;
	@ExcelProperty("合同号") private String contractNo;
	@ExcelProperty("租赁面积") private BigDecimal rentArea;
	@ExcelProperty("租赁期") private String leasePeriod;
	@ExcelProperty("免租期") private String rentFreePeriod;
	@ExcelProperty("租金单价（元/㎡）") private BigDecimal rentPrice;
	@ExcelProperty("租金（元/月）") private BigDecimal monthlyRent;
	@ExcelProperty("物业费（元/月）") private BigDecimal propertyFee;
	@ExcelProperty("2026年租金") private BigDecimal annualRent2026;
	@ExcelProperty("2026年物业费") private BigDecimal annualPropertyFee2026;
	@ExcelProperty("招商员") private String followUser;
}
