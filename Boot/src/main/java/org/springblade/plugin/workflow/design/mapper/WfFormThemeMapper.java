package org.springblade.plugin.workflow.design.mapper;

import org.springblade.plugin.workflow.design.entity.WfFormTheme;
import org.springblade.plugin.workflow.design.vo.WfFormThemeVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

/**
 * 表单 - 主题 Mapper 接口
 *
 * @author ssc
 */
public interface WfFormThemeMapper extends BaseMapper<WfFormTheme> {

	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param wfFormTheme
	 * @return
	 */
	List<WfFormThemeVO> selectWfFormThemePage(IPage page, WfFormThemeVO wfFormTheme);

}
