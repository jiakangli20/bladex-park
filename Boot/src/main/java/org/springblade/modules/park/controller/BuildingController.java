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
package org.springblade.modules.park.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.modules.park.constant.ParkConstant;
import org.springblade.modules.park.excel.BuildingExcel;
import org.springblade.modules.park.excel.BuildingImporter;
import org.springblade.modules.park.pojo.dto.BuildingDTO;
import org.springblade.modules.park.pojo.entity.Building;
import org.springblade.modules.park.pojo.vo.BuildingVO;
import org.springblade.modules.park.service.IBuildingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 建筑管理控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "building")
@RequestMapping(ParkConstant.APPLICATION_PARK_NAME + "/building")
@Tag(name = "建筑管理", description = "建筑管理接口")
public class BuildingController extends BladeController {

	private final IBuildingService buildingService;

	/**
	 * 分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "分页", description = "传入building")
	public R<IPage<BuildingVO>> page(Building building, Query query) {
		return R.data(buildingService.selectBuildingPage(Condition.getPage(query), building));
	}

	/**
	 * 列表
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "列表", description = "传入building")
	public R<List<Building>> list(Building building) {
		return R.data(buildingService.selectBuildingList(building));
	}

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "详情", description = "传入id")
	public R<BuildingVO> detail(@RequestParam Long id) {
		return R.data(buildingService.selectBuildingById(id));
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增或修改", description = "传入building")
	public R submit(@RequestBody BuildingDTO building) {
		return R.status(buildingService.submit(building));
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(buildingService.removeBuilding(ids));
	}

	/**
	 * 导出
	 */
	@GetMapping("/export")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "导出", description = "传入building")
	public void export(Building building, HttpServletResponse response) {
		List<BuildingExcel> list = buildingService.exportBuilding(building);
		ExcelUtil.export(response, "建筑数据" + DateUtil.time(), "建筑数据表", list, BuildingExcel.class);
	}

	/**
	 * 导出模板
	 */
	@GetMapping("/export-template")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "导出模板")
	public void exportTemplate(HttpServletResponse response) {
		List<BuildingExcel> list = List.of();
		ExcelUtil.export(response, "建筑数据模板", "建筑数据表", list, BuildingExcel.class);
	}

	/**
	 * 导入
	 */
	@PostMapping("/import")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导入", description = "传入excel")
	public R importBuilding(MultipartFile file) {
		BuildingImporter importer = new BuildingImporter(buildingService);
		ExcelUtil.save(file, importer, BuildingExcel.class);
		return R.success("操作成功");
	}

	/**
	 * 导入模板
	 */
	@GetMapping("/template")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "模板下载")
	public void template(HttpServletResponse response) {
		List<BuildingExcel> list = List.of();
		ExcelUtil.export(response, "建筑导入模板", "建筑数据表", list, BuildingExcel.class);
	}

}
