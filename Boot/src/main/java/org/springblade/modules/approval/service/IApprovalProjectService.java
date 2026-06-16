/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.approval.pojo.entity.ApprovalLog;
import org.springblade.modules.approval.pojo.entity.ApprovalMaterial;
import org.springblade.modules.approval.pojo.entity.ApprovalProject;

import java.util.List;

/**
 * 审批项目服务.
 *
 * @author BladeX
 */
public interface IApprovalProjectService extends IService<ApprovalProject> {

	ApprovalProject selectApprovalProjectById(Long projectId);

	List<ApprovalProject> selectApprovalProjectList(ApprovalProject project);

	IPage<ApprovalProject> selectApprovalProjectPage(IPage<ApprovalProject> page, ApprovalProject project);

	Kv selectApprovalProjectStatistics(ApprovalProject project);

	ApprovalProject insertApprovalProject(ApprovalProject project);

	boolean updateApprovalProject(ApprovalProject project);

	boolean deleteApprovalProjectByIds(String ids);

	boolean submitProject(Long projectId);

	boolean withdrawProject(Long projectId);

	boolean approveProject(Long projectId, ApprovalLog log);

	boolean rejectProject(Long projectId, ApprovalLog log);

	boolean transferProject(Long projectId, ApprovalLog log);

	boolean resubmitProject(Long projectId, ApprovalLog log);

	boolean archiveProject(Long projectId);

	Kv generateApprovalForm(Long projectId);

	List<ApprovalMaterial> selectApprovalMaterialList(ApprovalMaterial material);

	List<ApprovalLog> selectApprovalLogList(ApprovalLog log);

}
