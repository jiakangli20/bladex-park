package org.springblade.plugin.workflow.design.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.core.user.WfUserCache;
import org.springblade.plugin.workflow.design.entity.WfFormVariable;
import org.springblade.plugin.workflow.design.vo.WfFormVariableVO;

import java.util.Objects;

/**
 * 流程表单 - 历史变量 包装类,返回视图层所需的字段
 *
 * @author ssc
 */
public class WfFormVariableWrapper extends BaseEntityWrapper<WfFormVariable, WfFormVariableVO> {

	public static WfFormVariableWrapper build() {
		return new WfFormVariableWrapper();
	}

	@Override
	public WfFormVariableVO entityVO(WfFormVariable wfFormVariable) {
		WfFormVariableVO wfFormVariableVO = Objects.requireNonNull(BeanUtil.copyProperties(wfFormVariable, WfFormVariableVO.class));
		WfUser user = WfUserCache.getUser(wfFormVariable.getCreateUser());
		if (user != null) {
			wfFormVariableVO.setCreateUsername(user.getRealName());
		}
		return wfFormVariableVO;
	}
}
