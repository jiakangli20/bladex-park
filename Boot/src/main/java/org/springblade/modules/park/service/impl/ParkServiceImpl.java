/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.park.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.park.mapper.ParkMapper;
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.pojo.vo.ParkVO;
import org.springblade.modules.park.service.IParkService;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 园区 服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class ParkServiceImpl extends ServiceImpl<ParkMapper, Park> implements IParkService {
	private final IParkPermissionService parkPermissionService;

	@Override
	public IPage<ParkVO> selectParkPage(IPage<ParkVO> page, ParkVO park) {
		return page.setRecords(baseMapper.selectParkPage(page, park, parkPermissionService.authorizedParkIds()));
	}

	@Override
	public Map<String, Object> selectParkStatistics(Long parkId) {
		if (parkId != null) parkPermissionService.requirePark(parkId);
		return baseMapper.selectParkStatistics(parkId, parkPermissionService.authorizedParkIds());
	}

	@Override
	public boolean submit(Park park) {
		if (park == null) {
			throw new ServiceException("园区数据不能为空");
		}
		if (park.getId() == null && !parkPermissionService.hasAllParkAccess()) {
			throw new ServiceException("只有全园区管理员可以新增园区");
		}
		if (park.getId() != null) parkPermissionService.requirePark(park.getId());
		if (StringUtil.isBlank(park.getName()) || StringUtil.isBlank(park.getCode())) {
			throw new ServiceException("园区名称和园区编码不能为空");
		}
		park.setName(park.getName().trim());
		park.setCode(park.getCode().trim());
		if (park.getArea() != null && park.getArea().signum() < 0) {
			throw new ServiceException("园区面积不能小于0");
		}
		if (park.getRentMin() != null && park.getRentMax() != null
			&& park.getRentMin().compareTo(park.getRentMax()) > 0) {
			throw new ServiceException("最低租金不能高于最高租金");
		}
		if (StringUtil.isNotBlank(park.getStatus()) && !List.of("0", "1").contains(park.getStatus())) {
			throw new ServiceException("园区状态不正确");
		}
		if (park.getId() != null) {
			Park existing = getById(park.getId());
			if (existing == null) {
				throw new ServiceException("园区不存在");
			}
		}
		if (baseMapper.countDuplicate(park.getName(), park.getCode(), park.getId()) > 0) {
			throw new ServiceException("园区名称或园区编码已存在");
		}
		Date now = new Date();
		String userName = AuthUtil.getUserName();
		if (park.getId() == null) {
			park.setCreateBy(userName);
			park.setCreateTime(now);
		} else {
			park.setUpdateBy(userName);
			park.setUpdateTime(now);
		}
		return this.saveOrUpdate(park);
	}

	@Override
	public boolean removePark(String ids) {
		List<Long> idList = Func.toLongList(ids);
		if (idList.isEmpty()) {
			throw new ServiceException("请选择需要删除的园区");
		}
		for (Long id : idList) {
			parkPermissionService.requirePark(id);
			Park park = getById(id);
			if (park == null) {
				throw new ServiceException("园区不存在");
			}
			if (baseMapper.countParkReferences(park.getId()) > 0) {
				throw new ServiceException("园区已存在建筑、楼层、房源或合同，不能删除");
			}
		}
		return this.removeByIds(idList);
	}

}
