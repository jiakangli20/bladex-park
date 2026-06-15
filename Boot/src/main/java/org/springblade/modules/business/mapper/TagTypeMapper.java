/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.TagType;

import java.util.List;

/**
 * 客户标签类型 Mapper.
 *
 * @author BladeX
 */
public interface TagTypeMapper extends BaseMapper<TagType> {

	List<TagType> selectTagTypeList(@Param("tagType") TagType tagType);

	TagType selectTagTypeById(@Param("typeId") Integer typeId);

	TagType selectTagTypeByName(@Param("typeName") String typeName);

	Integer countTagsByType(@Param("typeId") Integer typeId);

	int insertTagType(TagType tagType);

	int updateTagType(TagType tagType);

	int deleteTagTypeById(@Param("typeId") Integer typeId);

}
