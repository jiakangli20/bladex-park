package org.springblade.modules.park.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springblade.modules.approval.pojo.entity.ApprovalFlow;
import org.springblade.modules.business.pojo.entity.*;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.contract.pojo.vo.ContractArchiveVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;
import org.springblade.modules.park.pojo.entity.AssetRecord;
import org.springblade.modules.park.pojo.entity.SmartDevice;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkPermissionMapperTest {

	private static final List<MapperCase> CASES = List.of(
		new MapperCase("org/springblade/modules/business/mapper/PropertyServiceMapper.xml", "org.springblade.modules.business.mapper.PropertyServiceMapper.selectPropertyServiceList", "service", new PropertyService()),
		new MapperCase("org/springblade/modules/business/mapper/PropertyWorkorderMapper.xml", "org.springblade.modules.business.mapper.PropertyWorkorderMapper.selectWorkorderPage", "workorder", new ServiceWorkorder()),
		new MapperCase("org/springblade/modules/business/mapper/MerchantMapper.xml", "org.springblade.modules.business.mapper.MerchantMapper.selectMerchantList", "merchant", new Merchant()),
		new MapperCase("org/springblade/modules/business/mapper/MerchantServiceOrderMapper.xml", "org.springblade.modules.business.mapper.MerchantServiceOrderMapper.selectOrderList", "order", new MerchantServiceOrder()),
		new MapperCase("org/springblade/modules/business/mapper/PolicyServiceMapper.xml", "org.springblade.modules.business.mapper.PolicyServiceMapper.selectPolicyList", "policy", new PolicyService()),
		new MapperCase("org/springblade/modules/business/mapper/MerchantAdMapper.xml", "org.springblade.modules.business.mapper.MerchantAdMapper.selectAdList", "ad", new MerchantAd()),
		new MapperCase("org/springblade/modules/business/mapper/TagMapper.xml", "org.springblade.modules.business.mapper.TagMapper.selectTagList", "tag", new Tag()),
		new MapperCase("org/springblade/modules/park/mapper/SmartDeviceMapper.xml", "org.springblade.modules.park.mapper.SmartDeviceMapper.selectDevicePage", "device", new SmartDevice()),
		new MapperCase("org/springblade/modules/park/mapper/AssetRecordMapper.xml", "org.springblade.modules.park.mapper.AssetRecordMapper.selectAssetPage", "asset", new AssetRecord()),
		new MapperCase("org/springblade/modules/approval/mapper/ApprovalFlowMapper.xml", "org.springblade.modules.approval.mapper.ApprovalFlowMapper.selectApprovalFlowPage", "flow", new ApprovalFlow()),
		new MapperCase("org/springblade/modules/contract/mapper/ContractArchiveMapper.xml", "org.springblade.modules.contract.mapper.ContractArchiveMapper.selectArchivePage", "contract", new ContractArchiveVO()),
		new MapperCase("org/springblade/modules/contract/mapper/ContractWorkflowRecordMapper.xml", "org.springblade.modules.contract.mapper.ContractWorkflowRecordMapper.selectRecordPage", "record", new ContractWorkflowRecord()),
		new MapperCase("org/springblade/modules/ics/mapper/PaymentNoticeMapper.xml", "org.springblade.modules.ics.mapper.PaymentNoticeMapper.selectNoticePage", "query", new PaymentNoticeVO()),
		new MapperCase("org/springblade/modules/home/mapper/HomeMapper.xml", "org.springblade.modules.home.mapper.HomeMapper.countRooms", "parkId", null)
	);

	@Test
	void emptyAuthorizedParksAlwaysProduceDenyAllSql() throws Exception {
		for (MapperCase mapperCase : CASES) {
			String sql = sql(mapperCase, List.of());
			assertTrue(sql.contains("1 = 0"), mapperCase.statement + " must deny an empty park scope");
		}
	}

	@Test
	void assignedParksProduceServerSideInCondition() throws Exception {
		for (MapperCase mapperCase : CASES) {
			String sql = sql(mapperCase, List.of(2001L, 2002L));
			assertFalse(sql.contains("1 = 0"), mapperCase.statement + " rejected a non-empty park scope");
			assertTrue(sql.toLowerCase().contains("park_id in"), mapperCase.statement + " did not generate a park IN condition");
		}
	}

	@Test
	void administratorNullScopeDoesNotAddDenyAllCondition() throws Exception {
		for (MapperCase mapperCase : CASES) {
			assertFalse(sql(mapperCase, null).contains("1 = 0"), mapperCase.statement + " restricted administrator scope");
		}
	}

	@Test
	void overdueAndDashboardQueriesHonorParkScope() throws Exception {
		assertScope("org/springblade/modules/ics/mapper/PaymentMapper.xml",
			"org.springblade.modules.ics.mapper.PaymentMapper.selectPaymentPage",
			Map.of("payment", new ContractPayment(), "overdue", false, "overdueHistory", false));
		assertScope("org/springblade/modules/ics/mapper/PaymentMapper.xml",
			"org.springblade.modules.ics.mapper.PaymentMapper.selectSummary",
			Map.of("payment", new ContractPayment(), "overdueHistory", false));
		assertScope("org/springblade/modules/ics/mapper/OverdueInternalNoticeMapper.xml",
			"org.springblade.modules.ics.mapper.OverdueInternalNoticeMapper.countUnread", Map.of("userId", 10L));
		assertScope("org/springblade/modules/business/mapper/EnterpriseDataMapper.xml",
			"org.springblade.modules.business.mapper.EnterpriseDataMapper.selectFinanceOverview", new HashMap<>());
		assertScope("org/springblade/modules/contract/mapper/ContractExpiryRuleMapper.xml",
			"org.springblade.modules.contract.mapper.ContractExpiryRuleMapper.selectRulePage", new HashMap<>());
	}

	private void assertScope(String resource, String statement, Map<String, Object> baseParams) throws Exception {
		Map<String, Object> emptyParams = new HashMap<>(baseParams);
		emptyParams.put("authorizedParkIds", List.of());
		assertTrue(sql(resource, statement, emptyParams).contains("1 = 0"), statement + " must deny an empty park scope");

		Map<String, Object> assignedParams = new HashMap<>(baseParams);
		assignedParams.put("authorizedParkIds", List.of(2001L, 2002L));
		String assignedSql = sql(resource, statement, assignedParams);
		assertFalse(assignedSql.contains("1 = 0"), statement + " rejected assigned parks");
		assertTrue(assignedSql.toLowerCase().contains("park_id in"), statement + " did not generate a park IN condition");

		Map<String, Object> adminParams = new HashMap<>(baseParams);
		adminParams.put("authorizedParkIds", null);
		assertFalse(sql(resource, statement, adminParams).contains("1 = 0"), statement + " restricted administrator scope");
	}

	private String sql(MapperCase mapperCase, List<Long> authorizedParkIds) throws Exception {
		Map<String, Object> params = new HashMap<>();
		params.put(mapperCase.queryKey, mapperCase.query);
		params.put("authorizedParkIds", authorizedParkIds);
		return sql(mapperCase.resource, mapperCase.statement, params);
	}

	private String sql(String resource, String statement, Map<String, Object> params) throws Exception {
		Configuration configuration = new Configuration();
		try (InputStream input = Resources.getResourceAsStream(resource)) {
			new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
		}
		BoundSql boundSql = configuration.getMappedStatement(statement).getBoundSql(params);
		return boundSql.getSql().replaceAll("\\s+", " ").trim();
	}

	private record MapperCase(String resource, String statement, String queryKey, Object query) {
	}
}
