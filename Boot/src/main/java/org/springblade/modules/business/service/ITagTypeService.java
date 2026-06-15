/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.pojo.entity.TagType;

import java.util.List;

/**
 * 客户标签类型服务.
 *
 * @author BladeX
 */
public interface ITagTypeService extends IService<TagType> {

	List<TagType> selectTagTypeList(TagType tagType);

	TagType selectTagTypeById(Integer typeId);

	boolean insertTagType(TagType tagType);

	boolean updateTagType(TagType tagType);

	boolean submitTagType(TagType tagType);

	boolean deleteTagTypeById(Integer typeId);

}
