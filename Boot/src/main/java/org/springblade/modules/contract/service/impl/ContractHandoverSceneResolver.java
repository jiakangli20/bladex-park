/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service.impl;

import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 统一识别签约交接和退租交接流程，兼容未保存场景字段的历史记录.
 */
final class ContractHandoverSceneResolver {

	static final String SIGN = "sign";
	static final String TERMINATION = "termination";

	private static final Set<String> TERMINATION_CONTRACT_STATUSES = Set.of("4", "7", "8");

	private ContractHandoverSceneResolver() {
	}

	static String resolve(ContractWorkflowRecord record) {
		return resolve(record, null);
	}

	static String resolve(ContractWorkflowRecord record, String contractStatus) {
		Map<String, Object> formData = parseFormData(record == null ? null : record.getFormDataJson());
		String explicitScene = firstNotBlank(
			Func.toStr(formData.get("handoverScene"), ""),
			Func.toStr(formData.get("roomReviewScene"), ""),
			Func.toStr(formData.get("reviewScene"), ""),
			Func.toStr(formData.get("scene"), "")
		);
		String normalizedScene = normalize(explicitScene);
		if (StringUtil.isNotBlank(normalizedScene)) {
			return normalizedScene;
		}

		String processKey = Func.toStr(record == null ? null : record.getProcessDefKey(), "")
			.trim()
			.toLowerCase();
		if ("roomview-1".equals(processKey) || "roomreview-1".equals(processKey)
			|| processKey.contains("sign")) {
			return SIGN;
		}
		if ("roomreview".equals(processKey) || processKey.contains("termination")
			|| processKey.contains("return")) {
			return TERMINATION;
		}
		if (formData.containsKey("sourceTerminationRecordId") || formData.containsKey("terminationRecordId")) {
			return TERMINATION;
		}
		if (StringUtil.isNotBlank(contractStatus) && TERMINATION_CONTRACT_STATUSES.contains(contractStatus)) {
			return TERMINATION;
		}
		return SIGN;
	}

	private static String normalize(String value) {
		String scene = Func.toStr(value, "").trim().toLowerCase();
		if (StringUtil.isBlank(scene)) {
			return null;
		}
		if (scene.contains("sign") || scene.contains("contract") || scene.contains("签约")
			|| scene.contains("合同")) {
			return SIGN;
		}
		if (scene.contains("termination") || scene.contains("return") || scene.contains("退租")) {
			return TERMINATION;
		}
		return null;
	}

	private static Map<String, Object> parseFormData(String json) {
		if (StringUtil.isBlank(json)) {
			return Collections.emptyMap();
		}
		try {
			Map<String, Object> data = JsonUtil.readMap(json);
			return data == null ? Collections.emptyMap() : data;
		} catch (Exception ignored) {
			return Collections.emptyMap();
		}
	}

	private static String firstNotBlank(String... values) {
		for (String value : values) {
			if (StringUtil.isNotBlank(value)) {
				return value;
			}
		}
		return "";
	}

}
