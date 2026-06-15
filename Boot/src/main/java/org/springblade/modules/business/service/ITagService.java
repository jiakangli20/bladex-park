/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.pojo.entity.Tag;

import java.util.List;

/**
 * 客户标签服务.
 *
 * @author BladeX
 */
public interface ITagService extends IService<Tag> {

	Tag selectTagById(Long tagId);

	List<Tag> selectTagList(Tag tag);

	IPage<Tag> selectTagPage(IPage<Tag> page, Tag tag);

	boolean insertTag(Tag tag);

	boolean updateTag(Tag tag);

	boolean submitTag(Tag tag);

	boolean deleteTagByIds(String ids, boolean force);

	List<Tag> selectTagByType(Integer tagType, Long parkId);

	List<Tag> selectTagsByCustomerId(Long customerId);

	List<Tag> selectTagsByOpportunityId(Long opportunityId);

	boolean setCustomerTags(Long customerId, List<Long> tagIds);

	boolean setOpportunityTags(Long opportunityId, List<Long> tagIds);

}
