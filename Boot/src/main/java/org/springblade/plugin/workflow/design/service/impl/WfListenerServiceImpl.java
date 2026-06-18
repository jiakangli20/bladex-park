package org.springblade.plugin.workflow.design.service.impl;

import org.springblade.plugin.workflow.design.entity.WfListenerEntity;
import org.springblade.plugin.workflow.design.vo.WfListenerVO;
import org.springblade.plugin.workflow.design.mapper.WfListenerMapper;
import org.springblade.plugin.workflow.design.service.IWfListenerService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;

/**
 * 任务/执行监听器 服务实现类
 *
 * @author ssc
 */
@Service
public class WfListenerServiceImpl extends BaseServiceImpl<WfListenerMapper, WfListenerEntity> implements IWfListenerService {

	@Override
	public IPage<WfListenerVO> selectWfListenerPage(IPage<WfListenerVO> page, WfListenerVO wfListener) {
		return page.setRecords(baseMapper.selectWfListenerPage(page, wfListener));
	}

}
