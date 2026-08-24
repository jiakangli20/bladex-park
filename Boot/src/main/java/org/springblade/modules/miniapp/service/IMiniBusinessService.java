/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.service;

import org.springblade.modules.miniapp.pojo.dto.MiniBusinessDTO;

import java.util.List;
import java.util.Map;

/**
 * 小程序聚合业务服务。
 *
 * @author Chill
 */
public interface IMiniBusinessService {
	Map<String, Object> home();
	List<Map<String, Object>> publicNotices();
	Map<String, Object> publicNotice(Long id);
	List<Map<String, Object>> publicPolicies();
	Map<String, Object> publicPolicy(Long id);
	List<Map<String, Object>> publicAds();
	Map<String, Object> publicAd(Long id);
	List<Map<String, Object>> houses(String keyword);
	Map<String, Object> house(Long id);
	List<Map<String, Object>> propertyServices();
	List<Map<String, Object>> valueServices(String keyword);
	Map<String, Object> valueService(Long id);

	Map<String, Object> createAppointment(String requestId, MiniBusinessDTO.Appointment request);
	List<Map<String, Object>> appointments();
	void cancelAppointment(Long id, String reason);
	Map<String, Object> createSettlement(String requestId, MiniBusinessDTO.Settlement request);
	List<Map<String, Object>> settlements();
	Map<String, Object> createPropertyOrder(String requestId, MiniBusinessDTO.PropertyOrder request);
	Map<String, Object> createValueOrder(String requestId, MiniBusinessDTO.ValueOrder request);
	Map<String, Object> company();
	void saveCompany(MiniBusinessDTO.Company request);
	List<Map<String, Object>> contracts();
	Map<String, Object> contract(Long id);
	List<Map<String, Object>> bills();
	Map<String, Object> bill(Long id);
	List<Map<String, Object>> utilityBills();
	Map<String, Object> utilityBill(Long id);
	List<Map<String, Object>> utilityBillSubmissions(Long id);
	Map<String, Object> submitUtilityPayment(String requestId, Long id, MiniBusinessDTO.PaymentSubmission request);
	List<Map<String, Object>> customerAds();
	Map<String, Object> customerAd(Long id);
	Map<String, Object> createCustomerAd(String requestId, MiniBusinessDTO.CustomerAd request);
	Map<String, Object> updateCustomerAd(Long id, MiniBusinessDTO.CustomerAd request);
	void submitCustomerAd(String requestId, Long id);
	void withdrawCustomerAd(Long id);
	List<Map<String, Object>> customerWorkOrders();
	Map<String, Object> customerWorkOrder(String type, Long id);
	void customerAction(String type, Long id, MiniBusinessDTO.CustomerAction request);
	List<Map<String, Object>> members();
	List<Map<String, Object>> invites();
	Map<String, Object> createInvite(MiniBusinessDTO.Invite request);
	void disableMember(Long id);

	List<Map<String, Object>> notifications();
	void readNotification(Long id);
	List<Map<String, Object>> adminWorkOrders(String type);
	Map<String, Object> adminWorkOrder(String type, Long id);
	void adminAction(String type, Long id, MiniBusinessDTO.AdminAction request);
	Map<String, Object> overview();
	List<Map<String, Object>> tenants(String keyword);
	Map<String, Object> tenant(Long id);
}
