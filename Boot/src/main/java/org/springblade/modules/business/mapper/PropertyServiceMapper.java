/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.PropertyService;

import java.util.List;

/**
 * 物业服务项 Mapper.
 *
 * @author BladeX
 */
public interface PropertyServiceMapper extends BaseMapper<PropertyService> {

	PropertyService selectPropertyServiceById(@Param("serviceId") Long serviceId);

	List<PropertyService> selectPropertyServiceList(@Param("service") PropertyService service);

	IPage<PropertyService> selectPropertyServicePage(IPage<PropertyService> page,
													 @Param("service") PropertyService service);

	int insertPropertyService(PropertyService service);

	int updatePropertyService(PropertyService service);

	int deletePropertyServiceByIds(@Param("ids") List<Long> ids, @Param("parkId") Long parkId);

}
