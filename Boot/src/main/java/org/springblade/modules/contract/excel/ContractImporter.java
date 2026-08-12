/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.excel;

import lombok.RequiredArgsConstructor;
import org.springblade.core.excel.support.ExcelImporter;
import org.springblade.modules.contract.service.IContractService;

import java.util.List;

/**
 * 合同数据导入类.
 */
@RequiredArgsConstructor
public class ContractImporter implements ExcelImporter<ContractExcel> {

	private final IContractService service;

	@Override
	public void save(List<ContractExcel> data) {
		service.importContract(data);
	}

}
