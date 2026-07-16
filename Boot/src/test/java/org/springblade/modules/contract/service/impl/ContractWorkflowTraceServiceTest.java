package org.springblade.modules.contract.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractWorkflowTraceServiceTest {

	private final ContractWorkflowTraceService service = new ContractWorkflowTraceService(null, null);

	@Test
	void mapsWorkflowStatusToUnreadableCommentFallback() {
		assertEquals("同意", service.workflowResult("approved"));
		assertEquals("驳回", service.workflowResult("rejected"));
		assertEquals("已撤回", service.workflowResult("withdrawn"));
		assertEquals("审批完成", service.workflowResult("running"));
	}

}
