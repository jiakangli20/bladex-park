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
package org.springblade.modules.contract.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 合同归档详情聚合视图对象
 *
 * @author Chill
 */
@Data
@Schema(description = "合同归档详情聚合视图对象")
public class ContractArchiveDetailVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "合同主档")
	private ContractArchiveVO contract;

	@Schema(description = "缴费记录")
	private List<ContractPayment> payments = new ArrayList<>();

	@Schema(description = "变更审批")
	private List<ContractChangeArchiveVO> changes = new ArrayList<>();

	@Schema(description = "退租记录")
	private List<TerminationArchiveVO> terminations = new ArrayList<>();

	@Schema(description = "操作日志")
	private List<ContractLog> logs = new ArrayList<>();

	@Schema(description = "归档进度")
	private Integer archiveStep = 0;

}
