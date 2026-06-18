package org.springblade.plugin.workflow.design.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.plugin.workflow.design.entity.WfFormTheme;
import org.springblade.plugin.workflow.design.vo.WfFormThemeVO;
import java.util.Objects;

/**
 * 表单 - 主题 包装类,返回视图层所需的字段
 *
 * @author ssc
 */
public class WfFormThemeWrapper extends BaseEntityWrapper<WfFormTheme, WfFormThemeVO>  {

	public static WfFormThemeWrapper build() {
		return new WfFormThemeWrapper();
 	}

	@Override
	public WfFormThemeVO entityVO(WfFormTheme wfFormTheme) {
		WfFormThemeVO wfFormThemeVO = Objects.requireNonNull(BeanUtil.copyProperties(wfFormTheme, WfFormThemeVO.class));

		//User createUser = UserCache.getUser(wfFormTheme.getCreateUser());
		//User updateUser = UserCache.getUser(wfFormTheme.getUpdateUser());
		//wfFormThemeVO.setCreateUserName(createUser.getName());
		//wfFormThemeVO.setUpdateUserName(updateUser.getName());

		return wfFormThemeVO;
	}


}
