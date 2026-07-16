package org.springblade.modules.contract.service.impl;

import org.junit.jupiter.api.Test;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficePdfConversionServiceTest {

	@Test
	void convertsAndCachesGeneratedOfficeFileAsPdf() throws Exception {
		Path template = Path.of(System.getProperty("user.dir")).getParent()
			.resolve("saber3/public/系统所需材料/君联大厦招商管理办法2023/附件二：合同会签审批表.docx");
		ContractNoticeFileVO document = new ContractNoticeFileVO();
		document.setNoticeName("合同会签审批表");
		document.setFileName("合同会签审批表.docx");
		document.setFileBytes(Files.readAllBytes(template));

		OfficePdfConversionService conversionService = new OfficePdfConversionService();
		ReflectionTestUtils.setField(conversionService, "sofficeCommand", "soffice");
		ReflectionTestUtils.setField(conversionService, "timeoutSeconds", 60L);
		byte[] first = conversionService.convert(document);
		byte[] second = conversionService.convert(document);

		assertTrue(first.length > 1000);
		assertTrue(new String(first, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
		assertArrayEquals(first, second);

		ContractDocumentPreviewService previewService = new ContractDocumentPreviewService(conversionService);
		String html = previewService.render(document, Map.of("合同编号", "TEST-001"), List.of());
		assertTrue(html.contains("data:application/pdf;base64,"));
		assertTrue(html.contains("DOCX 转 PDF 原版预览"));
	}
}
