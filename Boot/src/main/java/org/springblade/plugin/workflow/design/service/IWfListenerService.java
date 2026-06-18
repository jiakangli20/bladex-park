package org.springblade.plugin.workflow.design.service;

import org.springblade.plugin.workflow.design.entity.WfListenerEntity;
import org.springblade.plugin.workflow.design.vo.WfListenerVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;

/**
 * 任务/执行监听器 服务类
 *
 * @author ssc
 */
public interface IWfListenerService extends BaseService<WfListenerEntity> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param wfListener
	 * @return
	 */
	IPage<WfListenerVO> selectWfListenerPage(IPage<WfListenerVO> page, WfListenerVO wfListener);

}
