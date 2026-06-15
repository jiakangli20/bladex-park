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
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.park.mapper.ParkMapper;
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.pojo.vo.ParkVO;
import org.springblade.modules.park.service.IParkService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * 园区 服务实现类
 *
 * @author Chill
 */
@Service
public class ParkServiceImpl extends ServiceImpl<ParkMapper, Park> implements IParkService {

	@Override
	public IPage<ParkVO> selectParkPage(IPage<ParkVO> page, ParkVO park) {
		return page.setRecords(baseMapper.selectParkPage(page, park));
	}

	@Override
	public Map<String, Object> selectParkStatistics(Long parkId) {
		return baseMapper.selectParkStatistics(parkId);
	}

	@Override
	public boolean submit(Park park) {
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
		return this.removeByIds(Func.toLongList(ids));
	}

}
