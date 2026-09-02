package org.springblade.modules.business.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.modules.business.mapper.BusinessOpportunityMapper;
import org.springblade.modules.business.mapper.CustomerMapper;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.ITagService;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceImplTest {

	private final CustomerMapper customerMapper = mock(CustomerMapper.class);
	private final IParkPermissionService parkPermissionService = mock(IParkPermissionService.class);
	private final CustomerServiceImpl service = service();

	@Test
	void certificationSubmissionCreatesUnsettledCustomerArchive() {
		Customer stored = customer(88L, 5L, 0);
		when(customerMapper.selectCustomerIdByEnterpriseAndPark("测试企业", 5L, null)).thenReturn(null);
		when(customerMapper.selectCustomerIdByCreditCode("91320594MA1ABCDEF0", 5L)).thenReturn(null);
		when(customerMapper.insertCustomer(any())).thenAnswer(invocation -> {
			Customer customer = invocation.getArgument(0);
			customer.setCustomerId(88L);
			return 1;
		});
		when(customerMapper.selectCustomerById(88L)).thenReturn(stored);

		Customer result;
		try (MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class)) {
			auth.when(AuthUtil::getUserName).thenReturn("mini-user");
			result = service.prepareCertificationCustomer(customer(null, 5L, null));
		}

		ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
		verify(customerMapper).insertCustomer(captor.capture());
		assertEquals(0, captor.getValue().getSettlementStatus());
		assertEquals("0", captor.getValue().getStatus());
		assertEquals("mini-user", captor.getValue().getCreateBy());
		assertSame(stored, result);
		verify(parkPermissionService, never()).requirePark(any());
	}

	@Test
	void certificationSubmissionReusesCustomerInSamePark() {
		Customer existing = customer(7L, 5L, 1);
		when(customerMapper.selectCustomerIdByEnterpriseAndPark("测试企业", 5L, null)).thenReturn(7L);
		when(customerMapper.selectCustomerIdByEnterpriseAndPark("测试企业", 5L, 7L)).thenReturn(null);
		when(customerMapper.selectCustomerIdByCreditCode("91320594MA1ABCDEF0", 5L)).thenReturn(7L);
		when(customerMapper.selectCustomerById(7L)).thenReturn(existing);

		Customer result = service.prepareCertificationCustomer(customer(null, 5L, null));

		assertSame(existing, result);
		verify(customerMapper, never()).insertCustomer(any());
	}

	@Test
	void personalCertificationReusesCustomerWithoutCreditCode() {
		Customer existing = customer(7L, 5L, 1);
		existing.setCreditCode(null);
		Customer request = customer(null, 5L, null);
		request.setCreditCode(null);
		when(customerMapper.selectCustomerIdByEnterpriseAndPark("测试企业", 5L, null)).thenReturn(7L);
		when(customerMapper.selectCustomerIdByEnterpriseAndPark("测试企业", 5L, 7L)).thenReturn(null);
		when(customerMapper.selectCustomerById(7L)).thenReturn(existing);

		Customer result = service.prepareCertificationCustomer(request);

		assertSame(existing, result);
		verify(customerMapper, never()).insertCustomer(any());
	}

	@Test
	void certificationSubmissionRejectsSameNameWithDifferentCreditCode() {
		Customer existing = customer(7L, 5L, 0);
		existing.setCreditCode("91320594MA1EXISTING0");
		when(customerMapper.selectCustomerIdByEnterpriseAndPark("测试企业", 5L, null)).thenReturn(7L);
		when(customerMapper.selectCustomerIdByEnterpriseAndPark("测试企业", 5L, 7L)).thenReturn(null);
		when(customerMapper.selectCustomerById(7L)).thenReturn(existing);

		assertThrows(ServiceException.class,
			() -> service.prepareCertificationCustomer(customer(null, 5L, null)));

		verify(customerMapper, never()).insertCustomer(any());
	}

	@Test
	void certificationSubmissionRejectsSameCreditCodeWithDifferentName() {
		Customer existing = customer(7L, 5L, 0);
		existing.setEnterpriseName("已有企业");
		when(customerMapper.selectCustomerIdByCreditCode("91320594MA1ABCDEF0", 5L)).thenReturn(7L);

		assertThrows(ServiceException.class,
			() -> service.prepareCertificationCustomer(customer(null, 5L, null)));

		verify(customerMapper, never()).insertCustomer(any());
	}

	@Test
	void certificationSubmissionRejectsInactiveCustomer() {
		Customer existing = customer(7L, 5L, 0);
		existing.setStatus("1");
		when(customerMapper.selectCustomerIdByEnterpriseAndPark("测试企业", 5L, null)).thenReturn(7L);
		when(customerMapper.selectCustomerIdByEnterpriseAndPark("测试企业", 5L, 7L)).thenReturn(null);
		when(customerMapper.selectCustomerIdByCreditCode("91320594MA1ABCDEF0", 5L)).thenReturn(7L);
		when(customerMapper.selectCustomerById(7L)).thenReturn(existing);

		assertThrows(ServiceException.class,
			() -> service.prepareCertificationCustomer(customer(null, 5L, null)));

		verify(customerMapper, never()).insertCustomer(any());
	}

	@Test
	void certificationApprovalMarksCustomerAsSettled() {
		Customer existing = customer(7L, 5L, 0);
		when(customerMapper.selectCustomerById(7L)).thenReturn(existing);
		when(customerMapper.updateCustomer(any())).thenReturn(1);

		Customer result;
		try (MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class)) {
			auth.when(AuthUtil::getUserName).thenReturn("park-admin");
			result = service.approveCertificationCustomer(7L);
		}

		ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
		verify(customerMapper).updateCustomer(captor.capture());
		assertEquals(3, captor.getValue().getSettlementStatus());
		assertEquals("park-admin", captor.getValue().getUpdateBy());
		assertEquals(3, result.getSettlementStatus());
		verify(parkPermissionService).requirePark(5L);
	}

	@Test
	void certificationApprovalRejectsInactiveCustomer() {
		Customer existing = customer(7L, 5L, 0);
		existing.setStatus("2");
		when(customerMapper.selectCustomerById(7L)).thenReturn(existing);

		assertThrows(ServiceException.class, () -> service.approveCertificationCustomer(7L));

		verify(parkPermissionService).requirePark(5L);
		verify(customerMapper, never()).updateCustomer(any());
	}

	private CustomerServiceImpl service() {
		CustomerServiceImpl customerService = new CustomerServiceImpl(
			mock(ITagService.class), mock(BusinessOpportunityMapper.class), parkPermissionService);
		ReflectionTestUtils.setField(customerService, "baseMapper", customerMapper);
		return customerService;
	}

	private Customer customer(Long customerId, Long parkId, Integer settlementStatus) {
		Customer customer = new Customer();
		customer.setCustomerId(customerId);
		customer.setParkId(parkId);
		customer.setEnterpriseName("测试企业");
		customer.setCreditCode("91320594MA1ABCDEF0");
		customer.setContactName("测试联系人");
		customer.setContactPhone("13862061912");
		customer.setContactEmail("test@example.com");
		customer.setSettlementStatus(settlementStatus);
		customer.setStatus("0");
		customer.setDelFlag("0");
		return customer;
	}
}
