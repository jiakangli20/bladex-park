/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.approval.pojo.entity.ApprovalFlow;

import java.util.List;

/**
 * 审批流程配置 Mapper.
 *
 * @author BladeX
 */
public interface ApprovalFlowMapper extends BaseMapper<ApprovalFlow> {

	ApprovalFlow selectApprovalFlowById(@Param("flowId") Long flowId);

	List<ApprovalFlow> selectApprovalFlowList(@Param("flow") ApprovalFlow flow);

	IPage<ApprovalFlow> selectApprovalFlowPage(IPage<ApprovalFlow> page, @Param("flow") ApprovalFlow flow);

	int insertApprovalFlow(ApprovalFlow flow);

	int updateApprovalFlow(ApprovalFlow flow);

	int deleteApprovalFlowByIds(@Param("ids") List<Long> ids, @Param("parkId") Long parkId, @Param("updateBy") String updateBy);

}
