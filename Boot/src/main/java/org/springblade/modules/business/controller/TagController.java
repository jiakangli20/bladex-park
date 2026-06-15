/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.pojo.entity.Tag;
import org.springblade.modules.business.service.ITagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户标签控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "customer_tag")
@RequestMapping("/blade-park/tag")
@io.swagger.v3.oas.annotations.tags.Tag(name = "客户标签", description = "客户标签接口")
public class TagController extends BladeController {

	private final ITagService tagService;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入tagId")
	public R<Tag> detail(@Parameter(description = "标签ID") @RequestParam Long tagId) {
		return R.data(tagService.selectTagById(tagId));
	}

	@GetMapping("/get/{tagId}")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "详情", description = "兼容源接口")
	public R<Tag> get(@Parameter(description = "标签ID") @PathVariable Long tagId) {
		return R.data(tagService.selectTagById(tagId));
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "客户标签分页")
	public R<IPage<Tag>> list(Tag tag, Query query) {
		IPage<Tag> pages = tagService.selectTagPage(Condition.getPage(query), tag);
		return R.data(pages);
	}

	@GetMapping("/all")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "列表", description = "客户标签列表")
	public R<List<Tag>> all(Tag tag) {
		return R.data(tagService.selectTagList(tag));
	}

	@GetMapping("/list-by-type/{tagType}")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "按类型查询", description = "按标签类型查询启用标签")
	public R<List<Tag>> listByType(@PathVariable Integer tagType, @RequestParam(required = false) Long parkId) {
		return R.data(tagService.selectTagByType(tagType, parkId));
	}

	@GetMapping("/listByType/{tagType}")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "按类型查询", description = "兼容源接口")
	public R<List<Tag>> listByTypeCompat(@PathVariable Integer tagType, @RequestParam(required = false) Long parkId) {
		return listByType(tagType, parkId);
	}

	@GetMapping("/list-by-customer/{customerId}")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "客户标签", description = "查询客户已绑定标签")
	public R<List<Tag>> listByCustomer(@PathVariable Long customerId) {
		return R.data(tagService.selectTagsByCustomerId(customerId));
	}

	@GetMapping("/listByCustomer/{customerId}")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "客户标签", description = "兼容源接口")
	public R<List<Tag>> listByCustomerCompat(@PathVariable Long customerId) {
		return listByCustomer(customerId);
	}

	@GetMapping("/list-by-opportunity/{opportunityId}")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "商机标签", description = "查询商机已绑定标签")
	public R<List<Tag>> listByOpportunity(@PathVariable Long opportunityId) {
		return R.data(tagService.selectTagsByOpportunityId(opportunityId));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "新增", description = "新增客户标签")
	public R save(@Valid @RequestBody Tag tag) {
		return R.status(tagService.insertTag(tag));
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "修改", description = "修改客户标签")
	public R update(@Valid @RequestBody Tag tag) {
		return R.status(tagService.updateTag(tag));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "新增或修改", description = "新增或修改客户标签")
	public R submit(@Valid @RequestBody Tag tag) {
		return R.status(tagService.submitTag(tag));
	}

	@PostMapping("/remove")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "删除", description = "逻辑删除客户标签")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids,
					@RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
		return R.status(tagService.deleteTagByIds(ids, force));
	}

	@PostMapping("/set-customer-tags/{customerId}")
	@ApiOperationSupport(order = 14)
	@Operation(summary = "设置客户标签", description = "设置客户标签关系")
	public R setCustomerTags(@PathVariable Long customerId, @RequestBody(required = false) List<Long> tagIds) {
		return R.status(tagService.setCustomerTags(customerId, tagIds));
	}

	@PostMapping("/set-opportunity-tags/{opportunityId}")
	@ApiOperationSupport(order = 15)
	@Operation(summary = "设置商机标签", description = "设置商机标签关系")
	public R setOpportunityTags(@PathVariable Long opportunityId, @RequestBody(required = false) List<Long> tagIds) {
		return R.status(tagService.setOpportunityTags(opportunityId, tagIds));
	}

}
