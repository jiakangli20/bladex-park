/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.approval.pojo.entity.ApprovalProject;

import java.util.List;
import java.util.Map;

/**
 * 审批项目 Mapper.
 *
 * @author BladeX
 */
public interface ApprovalProjectMapper extends BaseMapper<ApprovalProject> {

	ApprovalProject selectApprovalProjectById(@Param("projectId") Long projectId);

	List<ApprovalProject> selectApprovalProjectList(@Param("project") ApprovalProject project);

	IPage<ApprovalProject> selectApprovalProjectPage(IPage<ApprovalProject> page, @Param("project") ApprovalProject project);

	List<Map<String, Object>> selectApprovalProjectStatistics(@Param("project") ApprovalProject project);

	int insertApprovalProject(ApprovalProject project);

	int updateApprovalProject(ApprovalProject project);

	int updateProcessStatus(@Param("projectId") Long projectId,
							@Param("parkId") Long parkId,
							@Param("processStatus") String processStatus,
							@Param("currentNode") String currentNode,
							@Param("currentNodeName") String currentNodeName,
							@Param("approvalResult") String approvalResult,
							@Param("updateBy") String updateBy);

	int archiveProject(@Param("projectId") Long projectId, @Param("parkId") Long parkId, @Param("updateBy") String updateBy);

	int deleteApprovalProjectByIds(@Param("ids") List<Long> ids, @Param("parkId") Long parkId, @Param("updateBy") String updateBy);

}
