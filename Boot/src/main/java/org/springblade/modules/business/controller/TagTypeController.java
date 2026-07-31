/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.pojo.entity.TagType;
import org.springblade.modules.business.service.ITagTypeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户标签类型控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "customer_tag")
@RequestMapping("/blade-park/tag-type")
@io.swagger.v3.oas.annotations.tags.Tag(name = "客户标签类型", description = "客户标签类型接口")
public class TagTypeController extends BladeController {

	private final ITagTypeService tagTypeService;

	@GetMapping("/detail")
	@PreAuth(menu = "customer_tag_view")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入typeId")
	public R<TagType> detail(@Parameter(description = "类型ID") @RequestParam Integer typeId) {
		return R.data(tagTypeService.selectTagTypeById(typeId));
	}

	@GetMapping("/list")
	@PreAuth(menu = "customer_tag_view")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "列表", description = "标签类型列表")
	public R<List<TagType>> list(TagType tagType) {
		return R.data(tagTypeService.selectTagTypeList(tagType));
	}

	@PostMapping("/save")
	@PreAuth(menu = "customer_tag_type_add")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增", description = "新增标签类型")
	public R save(@Valid @RequestBody TagType tagType) {
		return R.status(tagTypeService.insertTagType(tagType));
	}

	@PostMapping("/update")
	@PreAuth(menu = "customer_tag_type_edit")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "修改", description = "修改标签类型")
	public R update(@Valid @RequestBody TagType tagType) {
		return R.status(tagTypeService.updateTagType(tagType));
	}

	@PostMapping("/submit")
	@PreAuth("(#tagType.typeId == null && hasMenu('customer_tag_type_add')) || " +
		"(#tagType.typeId != null && hasMenu('customer_tag_type_edit'))")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增或修改", description = "新增或修改标签类型")
	public R submit(@Valid @RequestBody TagType tagType) {
		return R.status(tagTypeService.submitTagType(tagType));
	}

	@PostMapping("/remove/{typeId}")
	@PreAuth(menu = "customer_tag_type_delete")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "删除", description = "删除标签类型")
	public R remove(@PathVariable Integer typeId) {
		return R.status(tagTypeService.deleteTagTypeById(typeId));
	}

}
