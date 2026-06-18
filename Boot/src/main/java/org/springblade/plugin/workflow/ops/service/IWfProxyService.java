package org.springblade.plugin.workflow.ops.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.ops.entity.WfProxy;

import java.util.LinkedHashSet;

/**
 * 流程代理 服务类
 *
 * @author ssc
 */
public interface IWfProxyService extends IService<WfProxy> {

	Object create(WfProxy proxy);

	/**
	 * 获取流程的代理用户
	 *
	 * @param userId       原用户
	 * @param processInsId 流程实例id
	 * @return 代理用户
	 */
	String getProxyUser(String userId, String processInsId);

	/**
	 * 获取流程的代理用户
	 *
	 * @param users        原用户列表
	 * @param processInsId 流程实例id
	 * @return 代理用户
	 */
	LinkedHashSet<WfUser> getProxyUsers(LinkedHashSet<WfUser> users, String processInsId);

}
