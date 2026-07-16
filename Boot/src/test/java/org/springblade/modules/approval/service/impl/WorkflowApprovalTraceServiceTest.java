package org.springblade.modules.approval.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowApprovalTraceServiceTest {

	private final WorkflowApprovalTraceService service = new WorkflowApprovalTraceService(null, null, null, null);

	@Test
	void fallsBackWhenHistoricCommentContainsOnlyUnreadableMarkers() {
		assertEquals("同意", service.approvalComment("??", "同意"));
		assertEquals("驳回", service.approvalComment("？ �", "驳回"));
		assertEquals("审批完成", service.approvalComment("", null));
	}

	@Test
	void preservesReadableApprovalComments() {
		assertEquals("同意，请归档", service.approvalComment(" 同意，请归档 ", "同意"));
		assertEquals("驳回：材料不完整", service.approvalComment("驳回：材料不完整", "同意"));
	}

}
