package org.springblade.modules.contract.service.impl;

import org.junit.jupiter.api.Test;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractHandoverSceneResolverTest {

	@Test
	void resolvesExplicitScenes() {
		assertEquals(ContractHandoverSceneResolver.SIGN,
			ContractHandoverSceneResolver.resolve(record(null, "{\"handoverScene\":\"sign\"}")));
		assertEquals(ContractHandoverSceneResolver.TERMINATION,
			ContractHandoverSceneResolver.resolve(record(null, "{\"roomReviewScene\":\"termination\"}")));
	}

	@Test
	void resolvesLegacyProcessKeys() {
		assertEquals(ContractHandoverSceneResolver.SIGN,
			ContractHandoverSceneResolver.resolve(record("roomview-1", null)));
		assertEquals(ContractHandoverSceneResolver.TERMINATION,
			ContractHandoverSceneResolver.resolve(record("roomreview", null)));
	}

	@Test
	void resolvesTerminationSourceMarkerAndContractStatus() {
		assertEquals(ContractHandoverSceneResolver.TERMINATION,
			ContractHandoverSceneResolver.resolve(
				record(null, "{\"sourceTerminationRecordId\":\"1\"}")));
		assertEquals(ContractHandoverSceneResolver.TERMINATION,
			ContractHandoverSceneResolver.resolve(record(null, null), "7"));
	}

	@Test
	void defaultsUnknownHistoricalRecordsToSignScene() {
		assertEquals(ContractHandoverSceneResolver.SIGN,
			ContractHandoverSceneResolver.resolve(record(null, null)));
		assertEquals(ContractHandoverSceneResolver.SIGN,
			ContractHandoverSceneResolver.resolve(record("custom-handover", "{}")));
	}

	private ContractWorkflowRecord record(String processKey, String formDataJson) {
		ContractWorkflowRecord record = new ContractWorkflowRecord();
		record.setProcessDefKey(processKey);
		record.setFormDataJson(formDataJson);
		return record;
	}

}
