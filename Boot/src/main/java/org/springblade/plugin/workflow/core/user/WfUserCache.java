package org.springblade.plugin.workflow.core.user;

import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.*;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import static org.springblade.core.cache.constant.CacheConstant.USER_CACHE;

/**
 * 系统用户
 * 统一用户入口，防止更改包名的情况再次出现
 *
 * @author ssc
 */
public class WfUserCache {

	private static final String USER_CACHE_ID = "user:id:";

	private static final IUserService userService;

	static {
		userService = SpringUtil.getBean(IUserService.class);
	}

	/**
	 * 获取用户
	 *
	 * @param userId 用户id
	 */
	public static WfUser getUser(Long userId) {
		return getUser(userId, AuthUtil.getTenantId());
	}

	/**
	 * 获取用户
	 *
	 * @param userId   用户id
	 * @param tenantId 租户id
	 */
	public static WfUser getUser(Long userId, String tenantId) {
		User user = get(USER_CACHE, USER_CACHE_ID, userId, () -> userService.getById(userId), tenantId);
		return BeanUtil.copyProperties(user, WfUser.class);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> T get(String cacheName, String keyPrefix, Object key, Callable<T> valueLoader, String tenantId) {
		if (Func.hasEmpty(cacheName, keyPrefix, key)) {
			return null;
		}
		try {
			Cache.ValueWrapper valueWrapper = CacheUtil.getCache(cacheName, tenantId).get(keyPrefix.concat(String.valueOf(key)));
			Object value = null;
			if (valueWrapper == null) {
				T call = valueLoader.call();
				if (ObjectUtil.isNotEmpty(call)) {
					Field field = ReflectUtil.getField(call.getClass(), BladeConstant.DB_PRIMARY_KEY);
					if (ObjectUtil.isNotEmpty(field) && ObjectUtil.isEmpty(ClassUtil.getMethod(call.getClass(), BladeConstant.DB_PRIMARY_KEY_METHOD).invoke(call))) {
						return null;
					}
					CacheUtil.getCache(cacheName, tenantId).put(keyPrefix.concat(String.valueOf(key)), call);
					value = call;
				}
			} else {
				value = valueWrapper.get();
			}
			return (T) value;
		} catch (Exception ignore) {
			return null;
		}
	}
}
