package org.springblade.plugin.workflow.design.service;

import org.springblade.plugin.workflow.design.entity.WfFormTheme;
import org.springblade.plugin.workflow.design.vo.WfFormThemeVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;

/**
 * 表单 - 主题 服务类
 *
 * @author ssc
 */
public interface IWfFormThemeService extends BaseService<WfFormTheme> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param wfFormTheme
	 * @return
	 */
	IPage<WfFormThemeVO> selectWfFormThemePage(IPage<WfFormThemeVO> page, WfFormThemeVO wfFormTheme);


}
