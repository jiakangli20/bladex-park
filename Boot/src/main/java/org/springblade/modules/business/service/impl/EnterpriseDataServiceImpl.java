/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.business.mapper.EnterpriseDataMapper;
import org.springblade.modules.business.service.IEnterpriseDataService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 在园企业数据看板服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class EnterpriseDataServiceImpl implements IEnterpriseDataService {

	private final EnterpriseDataMapper enterpriseDataMapper;

	@Override
	public Kv overview(Long parkId) {
		Long scopedParkId = scopedParkId(parkId);
		Map<String, Object> finance = mapOrEmpty(enterpriseDataMapper.selectFinanceOverview(scopedParkId));
		Map<String, Object> contract = mapOrEmpty(enterpriseDataMapper.selectContractExecution(scopedParkId));
		Map<String, Object> room = mapOrEmpty(enterpriseDataMapper.selectRoomSummary(scopedParkId));

		return Kv.create()
			.set("digitalOverview", buildDigitalOverview(finance))
			.set("contractExecution", buildContractExecution(contract))
			.set("deviceSummary", buildDeviceSummary())
			.set("roomSummary", buildRoomSummary(room))
			.set("rentMetrics", buildRentMetrics(room))
			.set("vacancyWarning", enterpriseDataMapper.selectVacancyWarning(scopedParkId))
			.set("rentalTrend", enterpriseDataMapper.selectRentalTrend(scopedParkId))
			.set("contractDealTrend", enterpriseDataMapper.selectContractDealTrend(scopedParkId))
			.set("approvalList", enterpriseDataMapper.selectApprovalList(scopedParkId))
			.set("noticeTenantList", enterpriseDataMapper.selectNoticeTenantList(scopedParkId))
			.set("opportunityReminderList", enterpriseDataMapper.selectOpportunityReminderList(scopedParkId));
	}

	private List<Kv> buildDigitalOverview(Map<String, Object> finance) {
		List<Kv> list = new ArrayList<>();
		list.add(card("dueReceivableAmount", "目前已到期应收（元）", money(finance, "dueReceivableAmount"), "blue"));
		list.add(card("duePayableAmount", "目前已到期应支（元）", money(finance, "duePayableAmount"), "green"));
		list.add(card("next30ReceivableAmount", "未来30天应收（元）", money(finance, "next30ReceivableAmount"), "orange"));
		list.add(card("next30PayableAmount", "未来30天应支（元）", money(finance, "next30PayableAmount"), "red"));
		list.add(card("overdueTenantDebtAmount", "已逾期租客欠款（元）", money(finance, "overdueTenantDebtAmount"), "amber"));
		list.add(card("dueTenantCount", "目前租客已到期（个）", number(finance, "dueTenantCount"), "purple"));
		list.add(card("todayOtherReceivableAmount", "今日其他应收（元）", money(finance, "todayOtherReceivableAmount"), "sky"));
		list.add(card("todayOtherPayableAmount", "今日其他应支（元）", money(finance, "todayOtherPayableAmount"), "cyan"));
		return list;
	}

	private Kv buildContractExecution(Map<String, Object> contract) {
		return Kv.create()
			.set("totalCount", number(contract, "totalCount"))
			.set("activeCount", number(contract, "activeCount"))
			.set("terminatedCount", number(contract, "terminatedCount"))
			.set("expiredCount", number(contract, "expiredCount"));
	}

	private List<Kv> buildDeviceSummary() {
		List<Kv> list = new ArrayList<>();
		list.add(device("electric", "电表"));
		list.add(device("water", "水表"));
		list.add(device("camera", "摄像头"));
		list.add(device("lock", "门锁"));
		list.add(device("access", "门禁"));
		return list;
	}

	private Kv buildRoomSummary(Map<String, Object> room) {
		return Kv.create()
			.set("totalRooms", number(room, "totalRooms"))
			.set("vacantRooms", number(room, "vacantRooms"))
			.set("lockedRooms", number(room, "lockedRooms"))
			.set("rentedRooms", number(room, "rentedRooms"))
			.set("overdueRooms", number(room, "overdueRooms"))
			.set("expiredRooms", number(room, "expiredRooms"));
	}

	private Kv buildRentMetrics(Map<String, Object> room) {
		BigDecimal totalRooms = decimal(room, "totalRooms");
		BigDecimal rentedRooms = decimal(room, "rentedRooms");
		BigDecimal vacantRooms = decimal(room, "vacantRooms");
		BigDecimal totalArea = decimal(room, "totalArea");
		BigDecimal billableArea = decimal(room, "billableArea");

		return Kv.create()
			.set("averageRent", decimal(room, "averageRent").setScale(2, RoundingMode.HALF_UP))
			.set("rentRate", percent(rentedRooms, totalRooms))
			.set("vacancyRate", percent(vacantRooms, totalRooms))
			.set("billingRate", percent(billableArea, totalArea));
	}

	private Kv card(String key, String label, Object value, String tone) {
		return Kv.create()
			.set("key", key)
			.set("label", label)
			.set("value", value)
			.set("tone", tone);
	}

	private Kv device(String key, String label) {
		return Kv.create()
			.set("key", key)
			.set("label", label)
			.set("total", 0)
			.set("online", 0)
			.set("offline", 0);
	}

	private Long scopedParkId(Long parkId) {
		if (AuthUtil.isAdministrator()) {
			return Func.isEmpty(parkId) || parkId <= 0 ? null : parkId;
		}
		Long currentParkId = currentParkId();
		return Func.isEmpty(parkId) || parkId <= 0 ? currentParkId : parkId;
	}

	private Long currentParkId() {
		Long deptId = Func.firstLong(AuthUtil.getDeptId());
		return Func.isEmpty(deptId) ? 1L : deptId;
	}

	private Map<String, Object> mapOrEmpty(Map<String, Object> map) {
		return map == null ? Map.of() : map;
	}

	private BigDecimal money(Map<String, Object> map, String key) {
		return decimal(map, key).setScale(2, RoundingMode.HALF_UP);
	}

	private Long number(Map<String, Object> map, String key) {
		Object value = value(map, key);
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Func.toLong(value, 0L);
	}

	private BigDecimal decimal(Map<String, Object> map, String key) {
		Object value = value(map, key);
		if (value instanceof BigDecimal decimal) {
			return decimal;
		}
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		if (value == null) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(String.valueOf(value));
		} catch (NumberFormatException ignored) {
			return BigDecimal.ZERO;
		}
	}

	private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
		if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}
		return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 1, RoundingMode.HALF_UP);
	}

	private Object value(Map<String, Object> map, String key) {
		if (map == null || map.isEmpty()) {
			return null;
		}
		if (map.containsKey(key)) {
			return map.get(key);
		}
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (key.equalsIgnoreCase(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

}
