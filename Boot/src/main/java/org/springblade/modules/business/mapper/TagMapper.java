/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.Tag;
import org.springblade.modules.business.pojo.entity.TagType;

import java.util.List;

/**
 * 客户标签 Mapper.
 *
 * @author BladeX
 */
public interface TagMapper extends BaseMapper<Tag> {

	Tag selectTagById(@Param("tagId") Long tagId);

	List<Tag> selectTagList(@Param("tag") Tag tag);

	IPage<Tag> selectTagPage(IPage<Tag> page, @Param("tag") Tag tag);

	Tag selectSameNameTag(@Param("tagName") String tagName, @Param("tagType") Integer tagType, @Param("excludeTagId") Long excludeTagId);

	Integer countCustomerByTagId(@Param("tagId") Long tagId);

	Integer countOpportunityByTagId(@Param("tagId") Long tagId);

	Integer countTableRows(@Param("tableName") String tableName);

	TagType selectTagTypeById(@Param("tagType") Integer tagType);

	int insertTag(Tag tag);

	int updateTag(Tag tag);

	int deleteTagById(@Param("tagId") Long tagId);

	int deleteTagByIds(@Param("tagIds") List<Long> tagIds);

	List<Tag> selectTagByType(@Param("tagType") Integer tagType, @Param("parkId") Long parkId);

	List<Tag> selectTagsByCustomerId(@Param("customerId") Long customerId);

	List<Tag> selectTagsByOpportunityId(@Param("opportunityId") Long opportunityId);

	int batchInsertCustomerTag(@Param("customerId") Long customerId, @Param("tagIds") List<Long> tagIds);

	int batchInsertOpportunityTag(@Param("opportunityId") Long opportunityId, @Param("tagIds") List<Long> tagIds);

	int deleteCustomerTags(@Param("customerId") Long customerId);

	int deleteOpportunityTags(@Param("opportunityId") Long opportunityId);

}
