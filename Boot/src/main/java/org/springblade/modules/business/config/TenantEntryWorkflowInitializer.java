/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.design.entity.WfModel;
import org.springblade.plugin.workflow.design.entity.WfModelScope;
import org.springblade.plugin.workflow.design.entity.WfCategory;
import org.springblade.plugin.workflow.design.mapper.WfCategoryMapper;
import org.springblade.plugin.workflow.design.mapper.WfModelMapper;
import org.springblade.plugin.workflow.design.mapper.WfModelScopeMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * 入驻审批流程初始化.
 *
 * <p>Flowable 自动部署已关闭，代码内置的 tenant_entry 流程需要显式部署并登记模型权限。</p>
 *
 * @author BladeX
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "blade.park.workflow.tenant-entry", name = "init-legacy", havingValue = "true")
public class TenantEntryWorkflowInitializer implements ApplicationRunner {

	private static final String PROCESS_KEY = "tenant_entry";
	private static final String PROCESS_NAME = "企业入驻审批";
	private static final String PROCESS_RESOURCE = "processes/tenant_entry.bpmn20.xml";
	private static final String TENANT_ID = "000000";
	private static final String FORM_KEY = WfProcessConstant.EX_FORM_PREFIX + "TenantEntry";
	private static final String CATEGORY_NAME = "入驻流程";
	private static final String START_SCOPE_TYPE = "WF_ALL";
	private static final String START_SCOPE_VAL = "WF_ALL";
	private static final String DEFAULT_ROLE_SCOPE =
		"1123598816738675201,1123598816738675202,1123598816738675203,1123598816738675204,1123598816738675205";
	private static final String DEFAULT_ROLE_SCOPE_TEXT = "超级管理员,用户,人事,经理,老板";
	private static final String SYSTEM_USER_ID = "1123598821738675201";

	private final RepositoryService repositoryService;
	private final WfCategoryMapper wfCategoryMapper;
	private final WfModelMapper wfModelMapper;
	private final WfModelScopeMapper wfModelScopeMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void run(ApplicationArguments args) throws Exception {
		String xml = loadProcessXml();
		ProcessDefinition processDefinition = findLatestProcessDefinition();
		if (processDefinition == null) {
			Deployment deployment = repositoryService.createDeployment()
				.name(PROCESS_NAME)
				.key(PROCESS_KEY)
				.addString(PROCESS_KEY + WfProcessConstant.SUFFIX, xml)
				.tenantId(TENANT_ID)
				.deploy();
			processDefinition = repositoryService.createProcessDefinitionQuery()
				.deploymentId(deployment.getId())
				.singleResult();
			log.info("tenant_entry workflow deployed, deploymentId={}", deployment.getId());
		}

		WfCategory category = ensureCategory();
		WfModel model = ensureModel(xml, category.getId());
		ensureModelScope(model);
		if (processDefinition != null) {
			ensureStarterScope(processDefinition.getId());
		}
	}

	private String loadProcessXml() throws Exception {
		ClassPathResource resource = new ClassPathResource(PROCESS_RESOURCE);
		try (InputStream inputStream = resource.getInputStream()) {
			return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
		}
	}

	private ProcessDefinition findLatestProcessDefinition() {
		return repositoryService.createProcessDefinitionQuery()
			.processDefinitionKey(PROCESS_KEY)
			.processDefinitionTenantId(TENANT_ID)
			.latestVersion()
			.singleResult();
	}

	private WfCategory ensureCategory() {
		WfCategory category = wfCategoryMapper.selectOne(new LambdaQueryWrapper<WfCategory>()
			.eq(WfCategory::getName, CATEGORY_NAME)
			.eq(WfCategory::getTenantId, TENANT_ID)
			.last("limit 1"));
		if (category != null) {
			return category;
		}

		Date now = new Date();
		category = new WfCategory();
		category.setId(IdWorker.getId());
		category.setName(CATEGORY_NAME);
		category.setPid(0L);
		category.setSort(4);
		category.setTenantId(TENANT_ID);
		category.setCreateUser(Long.valueOf(SYSTEM_USER_ID));
		category.setCreateTime(now);
		category.setUpdateUser(Long.valueOf(SYSTEM_USER_ID));
		category.setUpdateTime(now);
		category.setStatus(1);
		category.setIsDeleted(0);
		wfCategoryMapper.insert(category);
		return category;
	}

	private WfModel ensureModel(String xml, Long categoryId) {
		WfModel model = wfModelMapper.selectOne(new LambdaQueryWrapper<WfModel>()
			.eq(WfModel::getModelKey, PROCESS_KEY)
			.eq(WfModel::getTenantId, TENANT_ID));
		Date now = new Date();
		if (model == null) {
			model = new WfModel();
			model.setId("tenantentry" + IdWorker.getIdStr());
			model.setName(PROCESS_NAME);
			model.setModelKey(PROCESS_KEY);
			model.setFormKey(FORM_KEY);
			model.setCategoryId(categoryId);
			model.setDescription("入驻管理项目审核流程：发起人提交，经理审批，老板审批，审批节点自动抄送人事。");
			model.setCreated(now);
			model.setCreatedBy(SYSTEM_USER_ID);
			model.setLastUpdated(now);
			model.setLastUpdatedBy(SYSTEM_USER_ID);
			model.setVersion(1);
			model.setModelType(WfModel.MODEL_TYPE_BPMN);
			model.setTenantId(TENANT_ID);
			model.setModelXml(xml);
			wfModelMapper.insert(model);
			return model;
		}
		if (model.getModelXml() == null || model.getModelXml().isBlank() || model.getCategoryId() == null) {
			model.setModelXml(xml);
			model.setFormKey(FORM_KEY);
			model.setCategoryId(categoryId);
			model.setLastUpdated(now);
			model.setLastUpdatedBy(SYSTEM_USER_ID);
			wfModelMapper.updateById(model);
		}
		return model;
	}

	private void ensureModelScope(WfModel model) {
		List<WfModelScope> scopeList = wfModelScopeMapper.selectList(new LambdaQueryWrapper<WfModelScope>()
			.eq(WfModelScope::getModelId, model.getId()));
		boolean hasAll = scopeList.stream().anyMatch(scope -> START_SCOPE_TYPE.equals(scope.getType()));
		if (!hasAll) {
			WfModelScope allScope = new WfModelScope();
			allScope.setId(IdWorker.getId());
			allScope.setModelId(model.getId());
			allScope.setModelKey(PROCESS_KEY);
			allScope.setType(START_SCOPE_TYPE);
			allScope.setVal(START_SCOPE_VAL);
			allScope.setText("全部");
			wfModelScopeMapper.insert(allScope);
		}

		boolean hasRole = scopeList.stream().anyMatch(scope -> "role".equals(scope.getType()));
		if (!hasRole) {
			WfModelScope roleScope = new WfModelScope();
			roleScope.setId(IdWorker.getId());
			roleScope.setModelId(model.getId());
			roleScope.setModelKey(PROCESS_KEY);
			roleScope.setType("role");
			roleScope.setVal(DEFAULT_ROLE_SCOPE);
			roleScope.setText(DEFAULT_ROLE_SCOPE_TEXT);
			wfModelScopeMapper.insert(roleScope);
		}
	}

	private void ensureStarterScope(String processDefinitionId) {
		List<WfModelScope> scopeList = wfModelScopeMapper.selectList(new LambdaQueryWrapper<WfModelScope>()
			.eq(WfModelScope::getModelKey, PROCESS_KEY));
		for (WfModelScope scope : scopeList) {
			if (START_SCOPE_TYPE.equals(scope.getType())) {
				addCandidateStarterGroup(processDefinitionId, START_SCOPE_VAL);
			} else if ("role".equals(scope.getType()) && scope.getVal() != null) {
				for (String group : scope.getVal().split(",")) {
					addCandidateStarterGroup(processDefinitionId, group);
				}
			}
		}
	}

	private void addCandidateStarterGroup(String processDefinitionId, String group) {
		if (group == null || group.isBlank()) {
			return;
		}
		String groupId = group.trim();
		boolean exists = repositoryService.getIdentityLinksForProcessDefinition(processDefinitionId).stream()
			.anyMatch(link -> groupId.equals(link.getGroupId()));
		if (!exists) {
			repositoryService.addCandidateStarterGroup(processDefinitionId, groupId);
		}
	}

}
