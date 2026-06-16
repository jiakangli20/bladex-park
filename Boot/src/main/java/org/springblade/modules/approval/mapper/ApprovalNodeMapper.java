/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.approval.pojo.entity.ApprovalNode;

import java.util.List;

/**
 * 审批流程节点 Mapper.
 *
 * @author BladeX
 */
public interface ApprovalNodeMapper extends BaseMapper<ApprovalNode> {

	List<ApprovalNode> selectNodesByFlowId(@Param("flowId") Long flowId);

	int insertNode(ApprovalNode node);

	int deleteNodesByFlowId(@Param("flowId") Long flowId);

}
