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
package org.springblade.modules.park.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.pojo.vo.ParkVO;

import java.util.Map;

/**
 * 园区 服务类
 *
 * @author Chill
 */
public interface IParkService extends IService<Park> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页
	 * @param park 园区
	 * @return IPage<ParkVO>
	 */
	IPage<ParkVO> selectParkPage(IPage<ParkVO> page, ParkVO park);

	/**
	 * 园区统计
	 *
	 * @param parkId 园区ID
	 * @return Map
	 */
	Map<String, Object> selectParkStatistics(Long parkId);

	/**
	 * 新增或修改园区
	 *
	 * @param park 园区
	 * @return boolean
	 */
	boolean submit(Park park);

	/**
	 * 删除园区
	 *
	 * @param ids 主键集合
	 * @return boolean
	 */
	boolean removePark(String ids);

}
