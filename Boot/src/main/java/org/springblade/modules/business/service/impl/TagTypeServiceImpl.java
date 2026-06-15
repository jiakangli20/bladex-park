/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.TagTypeMapper;
import org.springblade.modules.business.pojo.entity.TagType;
import org.springblade.modules.business.service.ITagTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户标签类型服务实现.
 *
 * @author BladeX
 */
@Service
public class TagTypeServiceImpl extends ServiceImpl<TagTypeMapper, TagType> implements ITagTypeService {

	private static final String STATUS_NORMAL = "0";
	private static final String DEL_FLAG_NORMAL = "0";

	@Override
	public List<TagType> selectTagTypeList(TagType tagType) {
		return baseMapper.selectTagTypeList(tagType);
	}

	@Override
	public TagType selectTagTypeById(Integer typeId) {
		return baseMapper.selectTagTypeById(typeId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertTagType(TagType tagType) {
		validateForSave(tagType, null);
		if (Func.isEmpty(tagType.getSortOrder()) || tagType.getSortOrder() <= 0) {
			tagType.setSortOrder(1);
		}
		if (StringUtil.isBlank(tagType.getStatus())) {
			tagType.setStatus(STATUS_NORMAL);
		}
		if (StringUtil.isBlank(tagType.getDelFlag())) {
			tagType.setDelFlag(DEL_FLAG_NORMAL);
		}
		tagType.setCreateBy(currentUserName());
		tagType.setCreateTime(DateUtil.now());
		return baseMapper.insertTagType(tagType) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateTagType(TagType tagType) {
		if (Func.isEmpty(tagType.getTypeId())) {
			throw new ServiceException("标签类型不存在");
		}
		TagType old = baseMapper.selectTagTypeById(tagType.getTypeId());
		if (Func.isEmpty(old)) {
			throw new ServiceException("标签类型不存在");
		}
		validateForSave(tagType, tagType.getTypeId());
		if (Func.isEmpty(tagType.getSortOrder()) || tagType.getSortOrder() <= 0) {
			tagType.setSortOrder(old.getSortOrder());
		}
		tagType.setUpdateBy(currentUserName());
		tagType.setUpdateTime(DateUtil.now());
		return baseMapper.updateTagType(tagType) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitTagType(TagType tagType) {
		return Func.isEmpty(tagType.getTypeId()) ? insertTagType(tagType) : updateTagType(tagType);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteTagTypeById(Integer typeId) {
		TagType old = baseMapper.selectTagTypeById(typeId);
		if (Func.isEmpty(old)) {
			throw new ServiceException("标签类型不存在");
		}
		Integer tagCount = baseMapper.countTagsByType(typeId);
		if (Func.isNotEmpty(tagCount) && tagCount > 0) {
			throw new ServiceException("该分类下存在标签，无法删除，请先清空分类内标签");
		}
		return baseMapper.deleteTagTypeById(typeId) > 0;
	}

	private void validateForSave(TagType tagType, Integer excludeTypeId) {
		String typeName = tagType.getTypeName() == null ? "" : tagType.getTypeName().trim();
		if (StringUtil.isBlank(typeName)) {
			throw new ServiceException("标签类型名称不能为空");
		}
		TagType sameName = baseMapper.selectTagTypeByName(typeName);
		if (Func.isNotEmpty(sameName) && !Func.equals(sameName.getTypeId(), excludeTypeId)) {
			throw new ServiceException("标签类型名称已存在");
		}
		tagType.setTypeName(typeName);
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

}
