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
package org.springblade.modules.park.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.pojo.vo.ParkVO;

import java.util.Objects;

/**
 * 园区包装类
 *
 * @author Chill
 */
public class ParkWrapper extends BaseEntityWrapper<Park, ParkVO> {

	public static ParkWrapper build() {
		return new ParkWrapper();
	}

	@Override
	public ParkVO entityVO(Park park) {
		ParkVO parkVO = Objects.requireNonNull(BeanUtil.copyProperties(park, ParkVO.class));
		parkVO.setStatusName("1".equals(parkVO.getStatus()) ? "停用" : "启用");
		return parkVO;
	}

}
