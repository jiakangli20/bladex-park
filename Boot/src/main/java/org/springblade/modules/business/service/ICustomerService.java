/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.approval.pojo.entity.ApprovalProject;
import org.springblade.modules.business.excel.CustomerExcel;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.pojo.entity.Customer;

import java.util.List;

/**
 * 入驻客户服务.
 *
 * @author BladeX
 */
public interface ICustomerService extends IService<Customer> {

	Customer selectCustomerById(Long customerId);

	List<Customer> selectCustomerList(Customer customer);

	IPage<Customer> selectCustomerPage(IPage<Customer> page, Customer customer);

	Kv selectCustomerStatistics(Customer customer);

	List<CustomerExcel> exportCustomer(Customer customer);

	void importCustomer(List<CustomerExcel> data);

	Customer insertCustomer(Customer customer);

	boolean updateCustomer(Customer customer);

	boolean submitCustomer(Customer customer);

	boolean deleteCustomerByIds(String ids);

	boolean changeStatus(Long customerId, String status);

	Customer checkCustomer(Long customerId);

	boolean setCustomerTags(Long customerId, List<Long> tagIds);

	Customer completeTenantEntryApproval(ApprovalProject project);

	Customer completeTenantEntryApproval(BusinessOpportunity opportunity, String processInsId, String approvalPdfUrl);

	boolean resetTenantEntryApproval(ApprovalProject project);

}
