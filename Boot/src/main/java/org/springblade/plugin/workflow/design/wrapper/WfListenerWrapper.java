package org.springblade.plugin.workflow.design.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.plugin.workflow.design.entity.WfListenerEntity;
import org.springblade.plugin.workflow.design.vo.WfListenerVO;
import java.util.Objects;

/**
 * 任务/执行监听器 包装类,返回视图层所需的字段
 *
 * @author ssc
 */
public class WfListenerWrapper extends BaseEntityWrapper<WfListenerEntity, WfListenerVO>  {

	public static WfListenerWrapper build() {
		return new WfListenerWrapper();
 	}

	@Override
	public WfListenerVO entityVO(WfListenerEntity wfListener) {
		WfListenerVO wfListenerVO = Objects.requireNonNull(BeanUtil.copyProperties(wfListener, WfListenerVO.class));

		//User createUser = UserCache.getUser(wfListener.getCreateUser());
		//User updateUser = UserCache.getUser(wfListener.getUpdateUser());
		//wfListenerVO.setCreateUserName(createUser.getName());
		//wfListenerVO.setUpdateUserName(updateUser.getName());

		return wfListenerVO;
	}


}
