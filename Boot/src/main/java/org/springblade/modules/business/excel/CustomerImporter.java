/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.excel;

import lombok.RequiredArgsConstructor;
import org.springblade.core.excel.support.ExcelImporter;
import org.springblade.modules.business.service.ICustomerService;

import java.util.List;

/**
 * 客户数据导入类.
 *
 * @author BladeX
 */
@RequiredArgsConstructor
public class CustomerImporter implements ExcelImporter<CustomerExcel> {

	private final ICustomerService service;

	@Override
	public void save(List<CustomerExcel> data) {
		service.importCustomer(data);
	}

}
