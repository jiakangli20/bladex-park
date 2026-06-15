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
import org.springblade.modules.business.mapper.TagMapper;
import org.springblade.modules.business.pojo.entity.Tag;
import org.springblade.modules.business.pojo.entity.TagType;
import org.springblade.modules.business.service.ITagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户标签服务实现.
 *
 * @author BladeX
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements ITagService {

	private static final String STATUS_NORMAL = "0";
	private static final String DEL_FLAG_NORMAL = "0";
	private static final String DEFAULT_COLOR = "#1059C6";
	private static final String CUSTOMER_TAG_TABLE = "biz_customer_tag";
	private static final String OPPORTUNITY_TAG_TABLE = "biz_business_opportunity_tag";

	@Override
	public Tag selectTagById(Long tagId) {
		return baseMapper.selectTagById(tagId);
	}

	@Override
	public List<Tag> selectTagList(Tag tag) {
		return baseMapper.selectTagList(tag);
	}

	@Override
	public IPage<Tag> selectTagPage(IPage<Tag> page, Tag tag) {
		return baseMapper.selectTagPage(page, tag);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertTag(Tag tag) {
		validateTag(tag, null);
		tag.setCreateBy(currentUserName());
		tag.setCreateTime(DateUtil.now());
		if (StringUtil.isBlank(tag.getStatus())) {
			tag.setStatus(STATUS_NORMAL);
		}
		if (StringUtil.isBlank(tag.getDelFlag())) {
			tag.setDelFlag(DEL_FLAG_NORMAL);
		}
		if (StringUtil.isBlank(tag.getTagColor())) {
			tag.setTagColor(DEFAULT_COLOR);
		}
		return baseMapper.insertTag(tag) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateTag(Tag tag) {
		if (Func.isEmpty(tag.getTagId())) {
			throw new ServiceException("标签不存在");
		}
		validateTag(tag, tag.getTagId());
		tag.setUpdateTime(DateUtil.now());
		return baseMapper.updateTag(tag) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitTag(Tag tag) {
		return Func.isEmpty(tag.getTagId()) ? insertTag(tag) : updateTag(tag);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteTagByIds(String ids, boolean force) {
		List<Long> tagIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (tagIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的标签");
		}
		for (Long tagId : tagIds) {
			Integer relationCount = countRelationByTagId(tagId);
			if (!force && Func.isNotEmpty(relationCount) && relationCount > 0) {
				throw new ServiceException("该标签已有客户关联，确定删除该标签？");
			}
		}
		return baseMapper.deleteTagByIds(tagIds) > 0;
	}

	@Override
	public List<Tag> selectTagByType(Integer tagType, Long parkId) {
		return baseMapper.selectTagByType(tagType, parkId);
	}

	@Override
	public List<Tag> selectTagsByCustomerId(Long customerId) {
		if (Func.isEmpty(customerId)) {
			return Collections.emptyList();
		}
		return baseMapper.selectTagsByCustomerId(customerId);
	}

	@Override
	public List<Tag> selectTagsByOpportunityId(Long opportunityId) {
		if (Func.isEmpty(opportunityId)) {
			return Collections.emptyList();
		}
		return baseMapper.selectTagsByOpportunityId(opportunityId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setCustomerTags(Long customerId, List<Long> tagIds) {
		if (Func.isEmpty(customerId)) {
			throw new ServiceException("客户不存在");
		}
		baseMapper.deleteCustomerTags(customerId);
		List<Long> validTagIds = normalizeTagIds(tagIds);
		if (validTagIds.isEmpty()) {
			return true;
		}
		return baseMapper.batchInsertCustomerTag(customerId, validTagIds) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setOpportunityTags(Long opportunityId, List<Long> tagIds) {
		if (Func.isEmpty(opportunityId)) {
			throw new ServiceException("商机不存在");
		}
		baseMapper.deleteOpportunityTags(opportunityId);
		List<Long> validTagIds = normalizeTagIds(tagIds);
		if (validTagIds.isEmpty()) {
			return true;
		}
		return baseMapper.batchInsertOpportunityTag(opportunityId, validTagIds) > 0;
	}

	private void validateTag(Tag tag, Long excludeTagId) {
		String tagName = tag.getTagName() == null ? "" : tag.getTagName().trim();
		if (StringUtil.isBlank(tagName)) {
			throw new ServiceException("标签名称不能为空");
		}
		if (Func.isEmpty(tag.getTagType())) {
			throw new ServiceException("请选择归属标签类型");
		}
		TagType tagType = baseMapper.selectTagTypeById(tag.getTagType());
		if (Func.isEmpty(tagType)) {
			throw new ServiceException("标签类型不存在或已停用");
		}
		if (Func.isEmpty(tag.getSortOrder()) || tag.getSortOrder() <= 0) {
			throw new ServiceException("排序必须为正整数");
		}
		Tag sameName = baseMapper.selectSameNameTag(tagName, tag.getTagType(), excludeTagId);
		if (Func.isNotEmpty(sameName)) {
			throw new ServiceException("同一标签类型下标签名称不可重复");
		}
		tag.setTagName(tagName);
	}

	private List<Long> normalizeTagIds(List<Long> tagIds) {
		if (Func.isEmpty(tagIds)) {
			return Collections.emptyList();
		}
		return tagIds.stream().filter(Func::isNotEmpty).distinct().collect(Collectors.toList());
	}

	private int countRelationByTagId(Long tagId) {
		int count = 0;
		if (tableExists(CUSTOMER_TAG_TABLE)) {
			Integer customerCount = baseMapper.countCustomerByTagId(tagId);
			count += Func.isEmpty(customerCount) ? 0 : customerCount;
		}
		if (tableExists(OPPORTUNITY_TAG_TABLE)) {
			Integer opportunityCount = baseMapper.countOpportunityByTagId(tagId);
			count += Func.isEmpty(opportunityCount) ? 0 : opportunityCount;
		}
		return count;
	}

	private boolean tableExists(String tableName) {
		Integer count = baseMapper.countTableRows(tableName);
		return Func.isNotEmpty(count) && count > 0;
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

}
