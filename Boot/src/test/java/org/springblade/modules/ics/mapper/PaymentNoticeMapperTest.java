package org.springblade.modules.ics.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentNoticeMapperTest {

	private static final String RESOURCE = "org/springblade/modules/ics/mapper/PaymentNoticeMapper.xml";
	private static final String NAMESPACE = "org.springblade.modules.ics.mapper.PaymentNoticeMapper";

	private Configuration configuration;

	@BeforeEach
	void setUp() throws IOException {
		configuration = new Configuration();
		try (InputStream input = Resources.getResourceAsStream(RESOURCE)) {
			new XMLMapperBuilder(input, configuration, RESOURCE, configuration.getSqlFragments()).parse();
		}
	}

	@Test
	void buildsDetailSqlWithUnifiedQueryParameter() {
		PaymentNoticeVO query = new PaymentNoticeVO();
		query.setPaymentId(100L);
		query.setNoticeType("payment-notice");

		BoundSql sql = configuration.getMappedStatement(NAMESPACE + ".selectNoticeByPaymentId")
			.getBoundSql(Map.of("query", query));

		assertTrue(sql.getSql().contains("n.notice_type = ?"));
		assertTrue(sql.getParameterMappings().stream()
			.anyMatch(mapping -> "query.paymentId".equals(mapping.getProperty())));
		assertTrue(sql.getParameterMappings().stream()
			.filter(mapping -> "query.noticeType".equals(mapping.getProperty()))
			.count() == 1);
	}

	@Test
	void buildsPageSqlWithSameQueryParameter() {
		PaymentNoticeVO query = new PaymentNoticeVO();
		query.setCategoryQuery("payment");

		BoundSql sql = configuration.getMappedStatement(NAMESPACE + ".selectNoticePage")
			.getBoundSql(Map.of("query", query));

		assertTrue(sql.getSql().contains("n.notice_type = 'payment-notice'"));
	}

}
