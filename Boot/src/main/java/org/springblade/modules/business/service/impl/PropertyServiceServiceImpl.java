/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.PropertyServiceMapper;
import org.springblade.modules.business.pojo.entity.PropertyService;
import org.springblade.modules.business.service.IPropertyServiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 物业服务项服务实现.
 *
 * @author BladeX
 */
@Service
public class PropertyServiceServiceImpl extends ServiceImpl<PropertyServiceMapper, PropertyService> implements IPropertyServiceService {

	private static final String STATUS_NORMAL = "0";
	private static final String DEL_FLAG_NORMAL = "0";

	@Override
	public PropertyService selectPropertyServiceById(Long serviceId) {
		PropertyService service = baseMapper.selectPropertyServiceById(serviceId);
		if (Func.isNotEmpty(service) && !hasAccessToPark(service.getParkId())) {
			throw new ServiceException("服务项不存在");
		}
		return service;
	}

	@Override
	public List<PropertyService> selectPropertyServiceList(PropertyService service) {
		return baseMapper.selectPropertyServiceList(normalizeQuery(service));
	}

	@Override
	public IPage<PropertyService> selectPropertyServicePage(IPage<PropertyService> page, PropertyService service) {
		return baseMapper.selectPropertyServicePage(page, normalizeQuery(service));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertPropertyService(PropertyService service) {
		validateService(service);
		service.setParkId(resolveWriteParkId(service.getParkId()));
		service.setStatus(StringUtil.isBlank(service.getStatus()) ? STATUS_NORMAL : service.getStatus());
		service.setDelFlag(DEL_FLAG_NORMAL);
		service.setSortOrder(Func.isEmpty(service.getSortOrder()) ? 0 : service.getSortOrder());
		service.setCreateBy(currentUserName());
		service.setCreateTime(DateUtil.now());
		return baseMapper.insertPropertyService(service) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updatePropertyService(PropertyService service) {
		if (Func.isEmpty(service.getServiceId())) {
			throw new ServiceException("服务项不存在");
		}
		PropertyService old = requireWritableService(service.getServiceId());
		validateService(service);
		if (AuthUtil.isAdministrator() && Func.isNotEmpty(service.getParkId()) && service.getParkId() > 0) {
			service.setParkId(service.getParkId());
		} else {
			service.setParkId(old.getParkId());
		}
		service.setUpdateBy(currentUserName());
		service.setUpdateTime(DateUtil.now());
		return baseMapper.updatePropertyService(service) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitPropertyService(PropertyService service) {
		return Func.isEmpty(service.getServiceId()) ? insertPropertyService(service) : updatePropertyService(service);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deletePropertyServiceByIds(String ids) {
		List<Long> serviceIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (serviceIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的服务项");
		}
		Long parkId = AuthUtil.isAdministrator() ? null : currentParkId();
		return baseMapper.deletePropertyServiceByIds(serviceIds, parkId) > 0;
	}

	private PropertyService requireWritableService(Long serviceId) {
		PropertyService service = baseMapper.selectPropertyServiceById(serviceId);
		if (Func.isEmpty(service) || !hasAccessToPark(service.getParkId())) {
			throw new ServiceException("服务项不存在");
		}
		return service;
	}

	private PropertyService normalizeQuery(PropertyService service) {
		PropertyService query = Func.isEmpty(service) ? new PropertyService() : service;
		if (!AuthUtil.isAdministrator()) {
			query.setParkId(currentParkId());
		}
		return query;
	}

	private Long resolveWriteParkId(Long parkId) {
		if (AuthUtil.isAdministrator() && Func.isNotEmpty(parkId) && parkId > 0) {
			return parkId;
		}
		return currentParkId();
	}

	private boolean hasAccessToPark(Long parkId) {
		return AuthUtil.isAdministrator() || currentParkId().equals(parkId);
	}

	private Long currentParkId() {
		Long deptId = Func.firstLong(AuthUtil.getDeptId());
		return Func.isEmpty(deptId) ? 1L : deptId;
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

	private void validateService(PropertyService service) {
		if (Func.isEmpty(service) || StringUtil.isBlank(service.getServiceName())) {
			throw new ServiceException("服务名称不能为空");
		}
		if (StringUtil.isBlank(service.getServiceType())) {
			throw new ServiceException("服务类型不能为空");
		}
		service.setServiceName(service.getServiceName().trim());
		service.setServiceType(service.getServiceType().trim());
	}

}
