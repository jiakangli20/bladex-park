/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.pojo.entity.PropertyService;

import java.util.List;

/**
 * 物业服务项服务.
 *
 * @author BladeX
 */
public interface IPropertyServiceService extends IService<PropertyService> {

	PropertyService selectPropertyServiceById(Long serviceId);

	List<PropertyService> selectPropertyServiceList(PropertyService service);

	IPage<PropertyService> selectPropertyServicePage(IPage<PropertyService> page, PropertyService service);

	boolean insertPropertyService(PropertyService service);

	boolean updatePropertyService(PropertyService service);

	boolean submitPropertyService(PropertyService service);

	boolean deletePropertyServiceByIds(String ids);

}
