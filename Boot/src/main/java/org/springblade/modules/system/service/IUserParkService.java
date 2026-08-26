package org.springblade.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.system.pojo.entity.UserPark;

import java.util.List;

public interface IUserParkService extends IService<UserPark> {
	List<UserPark> userParks(List<Long> userIds);
	List<Long> parkIds(Long userId);
	List<Long> parkIds(Long userId, String tenantId);
	Long defaultParkId(Long userId);
	Long defaultParkId(Long userId, String tenantId);
	boolean saveUserParks(Long userId, String tenantId, List<Long> parkIds, Long defaultParkId);
}
