package org.springblade.plugin.workflow.design.service.impl;

import org.springblade.plugin.workflow.design.entity.WfFormTheme;
import org.springblade.plugin.workflow.design.vo.WfFormThemeVO;
import org.springblade.plugin.workflow.design.mapper.WfFormThemeMapper;
import org.springblade.plugin.workflow.design.service.IWfFormThemeService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;

/**
 * 表单 - 主题 服务实现类
 *
 * @author ssc
 */
@Service
public class WfFormThemeServiceImpl extends BaseServiceImpl<WfFormThemeMapper, WfFormTheme> implements IWfFormThemeService {

	@Override
	public IPage<WfFormThemeVO> selectWfFormThemePage(IPage<WfFormThemeVO> page, WfFormThemeVO wfFormTheme) {
		return page.setRecords(baseMapper.selectWfFormThemePage(page, wfFormTheme));
	}

}
