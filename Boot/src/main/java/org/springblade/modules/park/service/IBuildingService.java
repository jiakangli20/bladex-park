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
import org.springblade.modules.park.excel.BuildingExcel;
import org.springblade.modules.park.pojo.dto.BuildingDTO;
import org.springblade.modules.park.pojo.entity.Building;
import org.springblade.modules.park.pojo.vo.BuildingVO;

import java.util.List;

/**
 * 建筑服务类
 *
 * @author Chill
 */
public interface IBuildingService extends IService<Building> {

	/**
	 * 查询建筑列表
	 *
	 * @param building 查询条件
	 * @return 建筑集合
	 */
	List<Building> selectBuildingList(Building building);

	/**
	 * 查询建筑分页
	 *
	 * @param page 分页
	 * @param building 查询条件
	 * @return 建筑分页
	 */
	IPage<BuildingVO> selectBuildingPage(IPage<BuildingVO> page, Building building);

	/**
	 * 查询建筑详情
	 *
	 * @param id 建筑ID
	 * @return 建筑
	 */
	BuildingVO selectBuildingById(Long id);

	/**
	 * 新增或修改建筑
	 *
	 * @param buildingDTO 建筑
	 * @return 是否成功
	 */
	boolean submit(BuildingDTO buildingDTO);

	/**
	 * 删除建筑
	 *
	 * @param ids 主键集合
	 * @return 是否成功
	 */
	boolean removeBuilding(String ids);

	/**
	 * 导出建筑
	 *
	 * @param building 查询条件
	 * @return 建筑导出集合
	 */
	List<BuildingExcel> exportBuilding(Building building);

	/**
	 * 导入建筑
	 *
	 * @param data 导入数据
	 */
	void importBuilding(List<BuildingExcel> data);

}
