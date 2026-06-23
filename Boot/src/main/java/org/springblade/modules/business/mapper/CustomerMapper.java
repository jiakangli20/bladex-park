/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.park.pojo.entity.Park;

import java.util.List;
import java.util.Map;

/**
 * 入驻客户 Mapper.
 *
 * @author BladeX
 */
public interface CustomerMapper extends BaseMapper<Customer> {

	Customer selectCustomerById(@Param("customerId") Long customerId);

	List<Customer> selectCustomerList(@Param("customer") Customer customer);

	IPage<Customer> selectCustomerPage(IPage<Customer> page, @Param("customer") Customer customer);

	Map<String, Object> selectCustomerStatistics(@Param("customer") Customer customer);

	List<Park> selectListPark();

	Long selectCustomerIdByCreditCode(@Param("creditCode") String creditCode);

	Long selectCustomerIdByEnterpriseAndPark(@Param("enterpriseName") String enterpriseName,
											 @Param("parkId") Long parkId,
											 @Param("excludeCustomerId") Long excludeCustomerId);

	Integer countTableRows(@Param("tableName") String tableName);

	Integer countOpportunityByCustomerId(@Param("customerId") Long customerId);

	Integer countApprovalProjectByCustomerId(@Param("customerId") Long customerId);

	Integer countContractByCustomerId(@Param("customerId") Long customerId);

	int insertCustomer(Customer customer);

	int updateCustomer(Customer customer);

	int updateCustomerStatus(@Param("customerId") Long customerId,
							 @Param("status") String status,
							 @Param("updateBy") String updateBy);

	int updateCustomerCheckResult(Customer customer);

	int updateOpportunityEnterpriseNameByCustomerId(@Param("customerId") Long customerId,
													@Param("enterpriseName") String enterpriseName,
													@Param("updateBy") String updateBy);

	int updateApprovalProjectEnterpriseNameByCustomerId(@Param("customerId") Long customerId,
														@Param("enterpriseName") String enterpriseName,
														@Param("updateBy") String updateBy);

	int deleteCustomerByIds(@Param("ids") List<Long> ids,
							@Param("updateBy") String updateBy);

	int updateApprovalProjectCustomerId(@Param("projectId") Long projectId,
										@Param("customerId") Long customerId,
										@Param("updateBy") String updateBy);

	int updateTenantEntryFlowState(@Param("customerId") Long customerId,
								   @Param("processInsId") String processInsId,
								   @Param("status") String status,
								   @Param("currentNode") String currentNode,
								   @Param("approvalPdfUrl") String approvalPdfUrl,
								   @Param("approvalTime") java.util.Date approvalTime,
								   @Param("settlementStatus") Integer settlementStatus,
								   @Param("updateBy") String updateBy);

}
