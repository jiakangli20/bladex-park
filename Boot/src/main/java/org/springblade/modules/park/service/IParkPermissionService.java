package org.springblade.modules.park.service;

import java.util.List;

public interface IParkPermissionService {
	boolean hasAllParkAccess();
	List<Long> authorizedParkIds();
	void requirePark(Long parkId);
	void requireAnyPark();
}
