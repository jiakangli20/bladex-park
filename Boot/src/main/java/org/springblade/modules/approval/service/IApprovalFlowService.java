/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.approval.pojo.entity.ApprovalFlow;
import org.springblade.modules.approval.pojo.entity.ApprovalNode;

import java.util.List;

/**
 * 审批流程配置服务.
 *
 * @author BladeX
 */
public interface IApprovalFlowService extends IService<ApprovalFlow> {

	ApprovalFlow selectApprovalFlowById(Long flowId);

	List<ApprovalFlow> selectApprovalFlowList(ApprovalFlow flow);

	IPage<ApprovalFlow> selectApprovalFlowPage(IPage<ApprovalFlow> page, ApprovalFlow flow);

	ApprovalFlow insertApprovalFlow(ApprovalFlow flow);

	boolean updateApprovalFlow(ApprovalFlow flow);

	boolean submitApprovalFlow(ApprovalFlow flow);

	boolean deleteApprovalFlowByIds(String ids);

	List<ApprovalNode> getStructuredNodes(Long flowId);

	boolean saveFlowNodes(Long flowId, List<ApprovalNode> nodes);

	boolean publishFlow(Long flowId);

	ApprovalFlow copyFlow(Long flowId);

}
