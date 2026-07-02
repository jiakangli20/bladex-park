package org.springblade.plugin.workflow.ops.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springblade.core.tool.api.R;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.core.user.WfUserCache;
import org.springblade.plugin.workflow.ops.entity.WfProxy;
import org.springblade.plugin.workflow.ops.mapper.WfProxyMapper;
import org.springblade.plugin.workflow.ops.service.IWfProxyService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 流程代理 服务实现类
 *
 * @author ssc
 */
@Service
@RequiredArgsConstructor
public class WfProxyServiceImpl extends ServiceImpl<WfProxyMapper, WfProxy> implements IWfProxyService {

	private final String WF_ALL_PROCESS = "WF_ALL_PROCESS";
	private final String PROXY_TYPE_FOREVER = "1";
	private final String PROXY_TYPE_TIMING = "2";

	private final RuntimeService runtimeService;

	@Override
	public Object create(WfProxy proxy) {
		String processDefKey = proxy.getProcessDefKey();
		String[] arr = processDefKey.split(",");
		int count = 0;
		for (String key : arr) {
			try {
				proxy.setId(null);
				proxy.setProcessDefKey(key);
				this.save(proxy);
				count++;
			} catch (DuplicateKeyException ignore) {
			}
		}
		if (count > 0 && arr.length == count) {
			return R.success("操作成功");
		} else if (count == 0) {
			return R.fail("操作失败，当前委托人已存在相同流程的代理");
		} else {
			return R.success("部分操作成功，当前委托人已存在部分相同流程的代理");
		}
	}

	@Override
	public String getProxyUser(String userId, String processInsId) {
		long uid;
		try {
			uid = Long.parseLong(userId);
			if (uid < 0) {
				return null;
			}
		} catch (Exception e) {
			return userId;
		}
		LinkedHashSet<WfUser> users = new LinkedHashSet<>();
		users.add(WfUserCache.getUser(uid));
		users = this.getProxyUsers(users, processInsId);
		if (users.isEmpty()) {
			return null;
		}
		WfUser user = new ArrayList<>(users).get(0);
		if (user != null) {
			return String.valueOf(user.getId());
		}
		return userId;
	}

	@Override
	public LinkedHashSet<WfUser> getProxyUsers(LinkedHashSet<WfUser> users, String processInsId) {
		if (users == null || users.isEmpty()) {
			return new LinkedHashSet<>();
		}
		try {
			ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInsId)
				.singleResult();
			if (processInstance == null) return users;
			LocalDateTime now = LocalDateTime.now();
			LinkedHashSet<WfUser> userList = new LinkedHashSet<>();
			users.stream().filter(Objects::nonNull).filter(user -> user.getId() != null).forEach(user -> {
				LambdaQueryWrapper<WfProxy> queryWrapper = new LambdaQueryWrapper<WfProxy>()
					.eq(WfProxy::getUserId, user.getId())
					.eq(WfProxy::getStatus, "1");
				List<WfProxy> list = this.list(queryWrapper);
				if (list.isEmpty()) {
					userList.add(user);
				} else {
					boolean hasProxy = false;
					for (WfProxy proxy : list) {
						String processDefKey = proxy.getProcessDefKey();
						String type = proxy.getType();
						LocalDateTime startTime = proxy.getStartTime();
						LocalDateTime endTime = proxy.getEndTime();
						// 全部或某个流程、永久
						if (PROXY_TYPE_FOREVER.equals(type) &&
							(WF_ALL_PROCESS.equals(processDefKey) || processInstance.getProcessDefinitionKey().equals(processDefKey))) {
							userList.add(WfUserCache.getUser(proxy.getProxyUserId()));
							hasProxy = true;
							break;
						}
						// 全部或某个流程、定时
						else if (PROXY_TYPE_TIMING.equals(type) &&
							(WF_ALL_PROCESS.equals(processDefKey) || processInstance.getProcessDefinitionKey().equals(processDefKey))) {
							if (now.isAfter(startTime) && now.isBefore(endTime)) { // 在时间段内
								userList.add(WfUserCache.getUser(proxy.getProxyUserId()));
								hasProxy = true;
								break;
							}
						}
					}
					if (!hasProxy) {
						userList.add(user);
					}
				}
			});
			return userList;
		} catch (Exception e) {
			return users;
		}
	}

}
