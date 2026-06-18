package org.springblade.plugin.workflow.core.user;

import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.service.IUserSearchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统用户
 * 统一用户入口，防止更改包名的情况再次出现
 *
 * @author ssc
 */
@Service
@RequiredArgsConstructor
public class WfUserService {

	private final IUserSearchService userSearchService;

	public List<WfUser> listByUser(List<Long> userId) {
		List<User> userList = userSearchService.listByUser(userId);

		// 根据传入的id重新排序
		Map<Long, User> userMap = userList.stream()
			.collect(Collectors.toMap(User::getId, Function.identity()));

		return userId.stream()
			.map(id -> {
				User user = userMap.get(id);
				return user != null ? BeanUtil.copyProperties(user, WfUser.class) : null;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	public List<WfUser> listByDept(List<Long> deptId) {
		List<User> userList = userSearchService.listByDept(deptId);
		return BeanUtil.copyProperties(userList, WfUser.class);
	}

	public List<WfUser> listByPost(List<Long> postId) {
		List<User> userList = userSearchService.listByPost(postId);
		return BeanUtil.copyProperties(userList, WfUser.class);
	}

	public List<WfUser> listByRole(List<Long> roleId) {
		List<User> userList = userSearchService.listByRole(roleId);
		return BeanUtil.copyProperties(userList, WfUser.class);
	}
}
