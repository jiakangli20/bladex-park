package org.springblade.plugin.workflow.design.mapper;

import org.springblade.plugin.workflow.design.entity.WfFormVariable;
import org.springblade.plugin.workflow.design.vo.WfFormVariableVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 流程表单 - 历史变量 Mapper 接口
 *
 * @author ssc
 */
public interface WfFormVariableMapper extends BaseMapper<WfFormVariable> {

	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param wfFormVariable
	 * @return
	 */
	List<WfFormVariableVO> selectWfFormVariablePage(IPage page, WfFormVariableVO wfFormVariable);

}
