package org.springblade.plugin.workflow.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.math.NumberUtils;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.process.entity.WfDraft;
import org.springblade.plugin.workflow.process.mapper.WfDraftMapper;
import org.springblade.plugin.workflow.process.service.IWfDraftService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 流程草稿箱 服务实现类
 *
 * @author ssc
 */
@Service
public class WfDraftServiceImpl extends ServiceImpl<WfDraftMapper, WfDraft> implements IWfDraftService {

	@Override
	public void submit(WfDraft wfDraft) {
		wfDraft.setUserId(Long.valueOf(WfTaskUtil.getTaskUser()));
		try {
			this.save(wfDraft);
		} catch (Exception e) {
			String variables = wfDraft.getVariables();
			LambdaQueryWrapper<WfDraft> wrapper = new LambdaQueryWrapper<WfDraft>()
				.eq(WfDraft::getUserId, wfDraft.getUserId())
				.eq(WfDraft::getPlatform, wfDraft.getPlatform());
			if (wfDraft.getTaskId() != null) {
				wrapper.eq(WfDraft::getTaskId, wfDraft.getTaskId());
			} else if (wfDraft.getProcessDefId() != null) {
				wrapper.eq(WfDraft::getProcessDefId, wfDraft.getProcessDefId());
			}
			this.update(new WfDraft().setVariables(variables), wrapper);
		}
	}

	@Async
	@Override
	public void deleteByProcessDefId(String processDefId, Long userId, Object platform) {
		baseMapper.deleteByProcessDefId(processDefId, userId, platform);
	}

	@Override
	public void deleteByTaskId(String taskId, String userId) {
		baseMapper.delete(new LambdaQueryWrapper<WfDraft>()
			.eq(WfDraft::getTaskId, taskId)
			.eq(WfDraft::getUserId, NumberUtils.toLong(userId)));
	}

	@Async
	@Override
	public void deleteByFormKey(String formKey) {
		baseMapper.deleteByFormKey(formKey);
	}

}
