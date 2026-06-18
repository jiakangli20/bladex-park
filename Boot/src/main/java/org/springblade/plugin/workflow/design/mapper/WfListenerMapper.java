package org.springblade.plugin.workflow.design.mapper;

import org.springblade.plugin.workflow.design.entity.WfListenerEntity;
import org.springblade.plugin.workflow.design.vo.WfListenerVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

/**
 * 任务/执行监听器 Mapper 接口
 *
 * @author ssc
 */
public interface WfListenerMapper extends BaseMapper<WfListenerEntity> {

	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param wfListener
	 * @return
	 */
	List<WfListenerVO> selectWfListenerPage(IPage page, WfListenerVO wfListener);

}
