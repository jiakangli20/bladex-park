package org.springblade.plugin.workflow.ops.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.plugin.workflow.ops.entity.WfProxy;
import org.springblade.plugin.workflow.ops.vo.WfProxyVO;
import java.util.Objects;

/**
 * 流程代理包装类,返回视图层所需的字段
 *
 * @author ssc
 */
public class WfProxyWrapper extends BaseEntityWrapper<WfProxy, WfProxyVO>  {

	public static WfProxyWrapper build() {
		return new WfProxyWrapper();
 	}

	@Override
	public WfProxyVO entityVO(WfProxy wfProxy) {
		WfProxyVO wfProxyVO = Objects.requireNonNull(BeanUtil.copyProperties(wfProxy, WfProxyVO.class));

		//User createUser = UserCache.getUser(wfProxy.getCreateUser());
		//User updateUser = UserCache.getUser(wfProxy.getUpdateUser());
		//wfProxyVO.setCreateUserName(createUser.getName());
		//wfProxyVO.setUpdateUserName(updateUser.getName());

		return wfProxyVO;
	}

}
