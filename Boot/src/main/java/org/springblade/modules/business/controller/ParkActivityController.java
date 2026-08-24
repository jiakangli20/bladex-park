/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.enums.StatusType;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.ICustomerService;
import org.springblade.modules.miniapp.mapper.ParkActivityMapper;
import org.springblade.modules.miniapp.mapper.ParkActivityAuditLogMapper;
import org.springblade.modules.miniapp.pojo.entity.ParkActivity;
import org.springblade.modules.miniapp.pojo.entity.ParkActivityAuditLog;
import org.springblade.modules.miniapp.service.AdminParkScopeService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/** 园区活动发布与企业活动申请审核。 */
@RestController
@RequiredArgsConstructor
@PreAuth(menu = "park_activity")
@RequestMapping("/blade-ics/park-activity")
public class ParkActivityController {

	private final ParkActivityMapper activityMapper;
	private final ParkActivityAuditLogMapper auditLogMapper;
	private final ICustomerService customerService;
	private final AdminParkScopeService parkScopeService;

	@GetMapping("/page")
	@PreAuth(menu = "park_activity_view")
	public R<IPage<ParkActivity>> page(ParkActivity query, Query pageQuery) {
		if (query.getParkId() != null) parkScopeService.assertAccess(query.getParkId());
		var scopedParkIds = parkScopeService.currentParkIds();
		IPage<ParkActivity> page = activityMapper.selectPage(Condition.getPage(pageQuery), Wrappers.<ParkActivity>lambdaQuery()
			.in(!AuthUtil.isAdministrator(), ParkActivity::getParkId, scopedParkIds.isEmpty() ? java.util.List.of(-1L) : scopedParkIds)
			.eq(query.getParkId() != null, ParkActivity::getParkId, query.getParkId())
			.eq(query.getPublishStatus() != null, ParkActivity::getPublishStatus, query.getPublishStatus())
			.eq(query.getAuditStatus() != null && !query.getAuditStatus().isBlank(), ParkActivity::getAuditStatus, query.getAuditStatus())
			.like(query.getTitle() != null && !query.getTitle().isBlank(), ParkActivity::getTitle, query.getTitle())
			.eq(ParkActivity::getIsDeleted, 0).orderByDesc(ParkActivity::getCreateTime));
		page.getRecords().forEach(this::fillCustomerName);
		return R.data(page);
	}

	@PostMapping("/submit")
	@PreAuth(menu = "park_activity_edit")
	@Transactional(rollbackFor = Exception.class)
	public R<Void> submit(@RequestBody ParkActivity activity) {
		validate(activity);
		parkScopeService.assertAccess(activity.getParkId());
		Date now = new Date();
		if (activity.getId() == null) {
			activity.setTenantId(AuthUtil.getTenantId());
			activity.setCustomerId(null);
			activity.setMemberId(null);
			activity.setAuditStatus("APPROVED");
			activity.setAuditOpinion("园区后台创建");
			activity.setPublishStatus(activity.getPublishStatus() == null ? 0 : activity.getPublishStatus());
			activity.setSortOrder(activity.getSortOrder() == null ? 0 : activity.getSortOrder());
			activity.setStatus(StatusType.ACTIVE.getType());
			activity.setIsDeleted(0);
			activity.setCreateUser(AuthUtil.getUserId());
			activity.setCreateTime(now);
			activityMapper.insert(activity);
		} else {
			ParkActivity old = requireActivity(activity.getId());
			parkScopeService.assertAccess(old.getParkId());
			copyEditableFields(activity, old);
			if (old.getCustomerId() != null) {
				String beforeAuditStatus = old.getAuditStatus();
				Integer beforePublishStatus = old.getPublishStatus();
				old.setPublishStatus(0);
				old.setAuditStatus("PENDING");
				old.setAuditUserId(null);
				old.setAuditUserName(null);
				old.setAuditTime(null);
				old.setAuditOpinion(null);
				old.setUpdateUser(AuthUtil.getUserId());
				old.setUpdateTime(now);
				activityMapper.updateById(old);
				activityMapper.update(null, Wrappers.<ParkActivity>lambdaUpdate().eq(ParkActivity::getId, old.getId())
					.set(ParkActivity::getAuditUserId, null).set(ParkActivity::getAuditUserName, null)
					.set(ParkActivity::getAuditTime, null).set(ParkActivity::getAuditOpinion, null));
				addLog(old, "ADMIN_UPDATE", beforeAuditStatus, "PENDING", beforePublishStatus, 0, "后台修改申请内容，重新进入审核");
			} else {
				old.setUpdateUser(AuthUtil.getUserId());
				old.setUpdateTime(now);
				activityMapper.updateById(old);
			}
		}
		return R.success("保存成功");
	}

	@PostMapping("/{id}/audit")
	@PreAuth(menu = "park_activity_audit")
	@Transactional(rollbackFor = Exception.class)
	public R<Void> audit(@PathVariable Long id, @RequestParam String status, @RequestParam(required = false) String opinion) {
		ParkActivity activity = requireActivity(id);
		parkScopeService.assertAccess(activity.getParkId());
		if (!"PENDING".equals(activity.getAuditStatus())) throw new ServiceException("仅待审核活动可以审核");
		if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) throw new ServiceException("审核状态不正确");
		Integer beforePublishStatus = activity.getPublishStatus();
		activity.setAuditStatus(status);
		activity.setAuditUserId(AuthUtil.getUserId());
		activity.setAuditUserName(AuthUtil.getUserName());
		activity.setAuditTime(new Date());
		activity.setAuditOpinion(opinion);
		activity.setPublishStatus(0);
		activity.setUpdateUser(AuthUtil.getUserId());
		activity.setUpdateTime(new Date());
		activityMapper.updateById(activity);
		addLog(activity, "APPROVED".equals(status) ? "APPROVE" : "REJECT", "PENDING", status, beforePublishStatus, 0, opinion);
		return R.success("审核完成");
	}

	@PostMapping("/{id}/publish")
	@PreAuth(menu = "park_activity_publish")
	@Transactional(rollbackFor = Exception.class)
	public R<Void> publish(@PathVariable Long id, @RequestParam Integer status) {
		ParkActivity activity = requireActivity(id);
		parkScopeService.assertAccess(activity.getParkId());
		if (status != 0 && status != 1) throw new ServiceException("发布状态不正确");
		if (status == 1 && !"APPROVED".equals(activity.getAuditStatus())) throw new ServiceException("审核通过后才能发布");
		Integer beforePublishStatus = activity.getPublishStatus();
		activity.setPublishStatus(status);
		activity.setUpdateUser(AuthUtil.getUserId());
		activity.setUpdateTime(new Date());
		activityMapper.updateById(activity);
		addLog(activity, status == 1 ? "PUBLISH" : "UNPUBLISH", activity.getAuditStatus(), activity.getAuditStatus(), beforePublishStatus, status, null);
		return R.success(status == 1 ? "已发布" : "已下架");
	}

	@PostMapping("/remove")
	@PreAuth(menu = "park_activity_delete")
	public R<Void> remove(@RequestParam Long id) {
		ParkActivity activity = requireActivity(id);
		parkScopeService.assertAccess(activity.getParkId());
		activity.setIsDeleted(1);
		activity.setUpdateUser(AuthUtil.getUserId());
		activity.setUpdateTime(new Date());
		activityMapper.updateById(activity);
		return R.success("删除成功");
	}

	private ParkActivity requireActivity(Long id) {
		ParkActivity activity = activityMapper.selectOne(Wrappers.<ParkActivity>lambdaQuery()
			.eq(ParkActivity::getId, id).eq(ParkActivity::getIsDeleted, 0));
		if (activity == null) throw new ServiceException("园区活动不存在");
		return activity;
	}

	private void validate(ParkActivity activity) {
		if (activity.getParkId() == null) throw new ServiceException("请选择园区");
		if (activity.getTitle() == null || activity.getTitle().isBlank()) throw new ServiceException("请输入活动标题");
		if (activity.getStartTime() == null || activity.getEndTime() == null || !activity.getEndTime().after(activity.getStartTime())) {
			throw new ServiceException("请选择正确的活动时间");
		}
	}

	private void copyEditableFields(ParkActivity source, ParkActivity target) {
		target.setTitle(source.getTitle());
		target.setCoverUrl(source.getCoverUrl());
		target.setSummary(source.getSummary());
		target.setStartTime(source.getStartTime());
		target.setEndTime(source.getEndTime());
		target.setAddress(source.getAddress());
		target.setPriceText(source.getPriceText());
		target.setContactName(source.getContactName());
		target.setContactPhone(source.getContactPhone());
		target.setPublishStatus(source.getPublishStatus() == null ? target.getPublishStatus() : source.getPublishStatus());
		target.setSortOrder(source.getSortOrder() == null ? target.getSortOrder() : source.getSortOrder());
	}

	private void fillCustomerName(ParkActivity activity) {
		if (activity.getCustomerId() == null) return;
		Customer customer = customerService.selectCustomerById(activity.getCustomerId());
		activity.setCustomerName(customer == null ? null : customer.getEnterpriseName());
	}

	private void addLog(ParkActivity activity, String action, String beforeAudit, String afterAudit,
		Integer beforePublish, Integer afterPublish, String opinion) {
		ParkActivityAuditLog log = new ParkActivityAuditLog();
		log.setTenantId(activity.getTenantId()); log.setActivityId(activity.getId()); log.setParkId(activity.getParkId()); log.setCustomerId(activity.getCustomerId());
		log.setActionType(action); log.setBeforeAuditStatus(beforeAudit); log.setAfterAuditStatus(afterAudit);
		log.setBeforePublishStatus(beforePublish); log.setAfterPublishStatus(afterPublish); log.setOperatorUserId(AuthUtil.getUserId()); log.setOperatorName(AuthUtil.getUserName());
		log.setOpinion(opinion); log.setOperateTime(new Date()); log.setStatus(StatusType.ACTIVE.getType()); log.setIsDeleted(0); auditLogMapper.insert(log);
	}
}
