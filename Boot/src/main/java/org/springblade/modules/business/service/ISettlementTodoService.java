/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.pojo.dto.SettlementTodoActionDTO;
import org.springblade.modules.business.pojo.entity.SettlementTodo;

import java.util.Map;

/**
 * 招商待办服务。
 */
public interface ISettlementTodoService extends IService<SettlementTodo> {
	IPage<SettlementTodo> selectPage(IPage<SettlementTodo> page, SettlementTodo query);
	Map<String, Object> statistics(SettlementTodo query);
	SettlementTodo requireScoped(Long id, Long parkId, Long customerId);
	void process(Long id, SettlementTodoActionDTO action);
}
