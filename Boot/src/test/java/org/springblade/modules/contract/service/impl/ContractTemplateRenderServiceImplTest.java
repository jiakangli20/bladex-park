package org.springblade.modules.contract.service.impl;

import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.junit.jupiter.api.Test;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractTemplateRenderServiceImplTest {

	private static final String CONTRACT_FIXED = "君联合同/科技服务中心租赁合同（固定租金）202508版 - 解锁.docx";
	private static final String TENANT_ENTRY_APPROVAL = "君联大厦招商管理办法2023/附件一：企业入驻审批表.docx";
	private static final String CONTRACT_APPROVAL = "君联大厦招商管理办法2023/附件二：合同会签审批表.docx";
	private static final String PAYMENT_NOTICE = "君联大厦招商管理办法2023/附件四：君联大厦付款通知单.docx";
	private static final String INVOICE_APPLY = "君联大厦招商管理办法2023/开票申请.docx";
	private static final String REMINDER = "君联大厦招商管理办法2023/附件五：催款通知书.docx";
	private static final String PROJECT_APPROVAL = "君联大厦招商管理办法2023/附件七：项目审批表.docx";
	private static final String TERMINATION_APPROVAL = "君联大厦招商管理办法2023/附件八：退租审批表.docx";
	private static final String ROOM_REVIEW = "君联大厦招商管理办法2023/附件15：房屋退租交接验收单（思锐泰）.xlsx";
	private static final String EXACT_PREFIX = "__exact__:";

	// 本测试仅覆盖仓库内置模板，不访问 OSS；生产环境由 Spring 注入 OssBuilder。
	private final ContractTemplateRenderServiceImpl service = new ContractTemplateRenderServiceImpl(null);

	@Test
	void fillsUpdatedTenantEntryApprovalTemplate() throws Exception {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("申请人", "招商员张三");
		fields.put("部门", "招商服务部");
		fields.put("申请日期", "2026-08-03");
		fields.put("企业名称", "苏州验收科技有限公司");
		fields.put("股东信息", "自然人股东");
		fields.put("经营范围", "软件开发与技术服务");
		fields.put("税收", "预计年税收100万元");
		fields.put("法人、联系方式", "张三，13800000000");
		fields.put("财务、联系方式", "李四，13900000000");
		fields.put("情况说明", "申请入驻君联大厦");
		fields.put("意向楼层", "22层2201，559.8㎡");
		fields.put("租金", "50元/㎡/月");
		fields.put("免租期", "2个月");
		fields.put("部门审批", "王经理（同意，2026-08-03）");
		fields.put("分管领导审批", "李总（同意，2026-08-03）");
		fields.put("总经理审批", "赵总（同意，2026-08-03）");

		ContractNoticeFileVO file = render(TENANT_ENTRY_APPROVAL, fields, Map.of());
		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getFileBytes()))) {
			assertEquals(1, document.getTables().size());
			XWPFTable table = document.getTables().get(0);
			assertEquals(12, table.getNumberOfRows());
			assertEquals("企业入驻审核表", table.getRow(0).getCell(0).getText());
			assertFalse(documentText(document).contains("合同会签审批表"));
			assertEquals("招商员张三", table.getRow(1).getCell(1).getText());
			assertEquals("苏州验收科技有限公司", table.getRow(2).getCell(1).getText());
			assertEquals("张三，13800000000", table.getRow(6).getCell(1).getText());
			assertEquals("李四，13900000000", table.getRow(6).getCell(3).getText());
			assertEquals("50元/㎡/月", table.getRow(8).getCell(3).getText());
			assertEquals("王经理（同意，2026-08-03）", table.getRow(9).getCell(1).getText());
			assertFalse(table.getRow(9).getCell(1).getText().contains("日期："));
			assertEquals(1, occurrences(table.getRow(9).getCell(1).getText(), "2026-08-03"));
		}
	}

	@Test
	void preservesContractSlotUnderlineWhenFillingInlineValues() throws Exception {
		String source = sourceParagraph(CONTRACT_FIXED, "本租赁合同由下列双方于");
		String target = "本租赁合同由下列双方于2026 年 7 月 2 日签订:";
		ContractNoticeFileVO file = render(CONTRACT_FIXED, Map.of(), Map.of(EXACT_PREFIX + source, target));

		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getFileBytes()))) {
			XWPFParagraph paragraph = findParagraph(document, "本租赁合同由下列双方于");
			assertNotNull(paragraph);
			assertEquals(target, paragraph.getText());
			assertTrue(underlinedDomText(paragraph.getCTP().getDomNode()).contains("2026"), dumpRuns(paragraph));
		}
	}

	@Test
	void preservesReminderFeeTableAndInlineStyles() throws Exception {
		String source = sourceParagraph(REMINDER, "贵司承租的位于");
		String target = "贵司承租的位于1701 / 龙西大厦办公物业租金已于2026 年 5 月 1 日到期，我司特向贵司发出本催款通知书，请贵司在收到本通知书的10个工作日内足额及时缴纳租金及其他相关费用。";
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("费用期间", "2026-09-01 至 2026-10-01");
		fields.put("项目名称", "租金");
		fields.put("应收金额", "11100.00");
		fields.put("应付日期", "2026-05-01");
		fields.put("违约金", "416.25");
		fields.put("总计", "11516.25");
		fields.put("合计人民币", "11516.25");
		int sourceRunCount;
		try (XWPFDocument sourceDocument = sourceDocument(REMINDER)) {
			sourceRunCount = findParagraph(sourceDocument, "贵司承租的位于").getRuns().size();
		}
		ContractNoticeFileVO file = render(REMINDER, fields, Map.of(EXACT_PREFIX + source, target));

		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getFileBytes()))) {
			XWPFTable table = document.getTables().get(0);
			assertEquals(7, table.getNumberOfRows());
			assertEquals("租金", table.getRow(2).getCell(1).getText());
			XWPFParagraph paragraph = findParagraph(document, "贵司承租的位于");
			assertNotNull(paragraph);
			assertEquals(sourceRunCount, paragraph.getRuns().size());
			assertTrue(underlinedDomText(paragraph.getCTP().getDomNode()).contains("2026"), dumpRuns(paragraph));
		}
	}

	@Test
	void keepsTerminationApprovalOnSinglePage() throws Exception {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("退租申请单位（个人）", "苏州验收科技有限公司");
		fields.put("申请时间", "2026-07-15");
		fields.put("申请人联系方式", "-");
		fields.put("租赁楼层、面积", "1701 / 龙西大厦 300 m2");
		fields.put("合同有效期", "2026-08-01 至 2026-10-01");
		fields.put("月租金（元）", "11100.00");
		fields.put("保证金（元）", "11100.00");
		fields.put("经办人", "招商员张三");
		fields.put("部门", "招商服务部");
		fields.put("申请内容", "验收流程提前退租");
		fields.put("分管领导", "分管领导（同意，2026-07-02）");
		fields.put("总经理", "总经理（同意，2026-07-02）");

		int sourceParagraphs;
		try (XWPFDocument sourceDocument = sourceDocument(TERMINATION_APPROVAL)) {
			sourceParagraphs = tableParagraphCount(sourceDocument.getTables().get(0));
		}
		ContractNoticeFileVO file = render(TERMINATION_APPROVAL, fields, Map.of());
		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getFileBytes()))) {
			assertTrue(document.getParagraphs().get(0).getText().contains("退租审批表"));
			assertEquals(ParagraphAlignment.CENTER, document.getParagraphs().get(0).getAlignment());
			assertFalse(document.getParagraphs().stream()
				.anyMatch(paragraph -> hasPageBreak(paragraph.getCTP().getDomNode())));
			XWPFTable table = document.getTables().get(0);
			assertEquals(14, table.getNumberOfRows());
			assertEquals(sourceParagraphs - 5, tableParagraphCount(table));
			assertEquals(1200, table.getRow(7).getHeight());
			assertEquals(1200, table.getRow(8).getHeight() + table.getRow(9).getHeight());
			assertEquals(1200, table.getRow(10).getHeight());
			assertEquals(1200, table.getRow(11).getHeight() + table.getRow(12).getHeight());
			assertEquals(1200, table.getRow(13).getHeight());
			XWPFParagraph trailing = document.getParagraphs().get(document.getParagraphs().size() - 1);
			assertTrue(trailing.getText().isBlank());
			assertEquals(0, trailing.getSpacingBefore());
			assertEquals(0, trailing.getSpacingAfter());
			assertEquals(1, trailing.getRuns().get(0).getFontSize());
		}
	}

	@Test
	void rendersProjectApprovalTitleBeforeInlineTable() throws Exception {
		ContractNoticeFileVO file = render(PROJECT_APPROVAL, Map.of(), Map.of());

		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getFileBytes()))) {
			XWPFParagraph title = document.getParagraphs().get(0);
			assertEquals("项目审批表", title.getText());
			assertEquals(ParagraphAlignment.CENTER, title.getAlignment());
			assertFalse(hasLocalName(title.getCTP().getPPr().getDomNode(), "ind"));
			assertFalse(hasLocalName(document.getTables().get(0).getCTTbl().getTblPr().getDomNode(), "tblpPr"));
		}
	}

	@Test
	void compactsLongContractNumberInApprovalTable() throws Exception {
		String contractNo = "ACCEPT-20260702-CN-165426";
		ContractNoticeFileVO file = render(CONTRACT_APPROVAL, Map.of("合同编号", contractNo), Map.of());

		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getFileBytes()))) {
			XWPFParagraph valueParagraph = document.getTables().stream()
				.flatMap(table -> table.getRows().stream())
				.flatMap(row -> row.getTableCells().stream())
				.flatMap(cell -> cell.getParagraphs().stream())
				.filter(paragraph -> paragraph.getText().contains(contractNo))
				.findFirst()
				.orElse(null);
			assertNotNull(valueParagraph);
			assertTrue(hasTextRunSize(valueParagraph.getCTP().getDomNode(), 16));
		}
	}

	@Test
	void doesNotApplyNormalParagraphReplacementTwice() throws Exception {
		String date = "2026年7月16日";
		ContractNoticeFileVO file = render(PAYMENT_NOTICE, Map.of(),
			Map.of("日期（Date）:", "日期（Date）:" + date));

		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getFileBytes()))) {
			XWPFParagraph dateParagraph = document.getParagraphs().stream()
				.filter(paragraph -> paragraph.getText().contains("Date"))
				.findFirst()
				.orElse(null);
			assertNotNull(dateParagraph);
			assertEquals(1, occurrences(dateParagraph.getText(), date));
		}
	}

	@Test
	void keepsInvoiceFinalRowAtOriginalCrossPageHeight() throws Exception {
		ContractNoticeFileVO file = render(INVOICE_APPLY, Map.of(), Map.of());

		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getFileBytes()))) {
			assertEquals(2600, document.getTables().get(0).getRow(3).getHeight());
			assertEquals(1, document.getTables().get(0).getRow(4).getCell(0).getParagraphs().size());
			assertEquals("开票内容及所属期", document.getTables().get(0).getRow(4).getCell(0).getText());
			assertEquals(2, document.getTables().get(0).getRow(4).getCell(1).getParagraphs().size());
		}
	}

	@Test
	void keepsMergedInlineWorkbookFieldsInsidePrintLayout() throws Exception {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("联系人", "-");
		fields.put("联系电话", "-");
		fields.put("退租理由", "验收通过");
		fields.put("招商部签字", "招商负责人（同意，2026-07-02）");
		ContractNoticeFileVO file = render(ROOM_REVIEW, fields, Map.of());

		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getFileBytes()))) {
			Sheet sheet = workbook.getSheetAt(0);
			DataFormatter formatter = new DataFormatter();
			assertEquals("联系人：-", formatter.formatCellValue(sheet.getRow(1).getCell(5)));
			assertEquals("联系电话：-", formatter.formatCellValue(sheet.getRow(2).getCell(5)));
			assertTrue(sheet.getRow(1).getCell(8) == null || formatter.formatCellValue(sheet.getRow(1).getCell(8)).isBlank());
			assertTrue(sheet.getRow(2).getCell(8) == null || formatter.formatCellValue(sheet.getRow(2).getCell(8)).isBlank());
			assertEquals("退租理由：验收通过", formatter.formatCellValue(sheet.getRow(4).getCell(0)));
			assertTrue(sheet.getRow(4).getCell(0).getCellStyle().getShrinkToFit());
		}
	}

	private ContractNoticeFileVO render(String template, Map<String, String> fields, Map<String, String> replacements) {
		return service.render("test", "模板测试", template, "模板测试", fields, replacements);
	}

	private String sourceParagraph(String template, String prefix) throws Exception {
		try (XWPFDocument document = sourceDocument(template)) {
			XWPFParagraph paragraph = findParagraph(document, prefix);
			assertNotNull(paragraph);
			return paragraph.getText();
		}
	}

	private XWPFDocument sourceDocument(String template) throws Exception {
		Path path = Path.of(System.getProperty("user.dir")).getParent()
			.resolve("saber3/public/系统所需材料").resolve(template);
		InputStream input = Files.newInputStream(path);
		return new XWPFDocument(input);
	}

	private XWPFParagraph findParagraph(XWPFDocument document, String prefix) {
		return document.getParagraphs().stream()
			.filter(paragraph -> paragraph.getText().startsWith(prefix))
			.findFirst()
			.orElse(null);
	}

	private int tableParagraphCount(XWPFTable table) {
		return table.getRows().stream()
			.flatMap(row -> row.getTableCells().stream())
			.mapToInt(cell -> cell.getParagraphs().size())
			.sum();
	}

	private String documentText(XWPFDocument document) {
		return document.getParagraphs().stream().map(XWPFParagraph::getText).reduce("", String::concat)
			+ document.getTables().stream()
			.flatMap(table -> table.getRows().stream())
			.flatMap(row -> row.getTableCells().stream())
			.map(XWPFTableCell::getText)
			.reduce("", String::concat);
	}

	private String underlinedDomText(Node root) {
		StringBuilder builder = new StringBuilder();
		collectUnderlinedDomText(root, false, builder);
		return builder.toString();
	}

	private void collectUnderlinedDomText(Node node, boolean underlinedRun, StringBuilder builder) {
		if (node == null) {
			return;
		}
		boolean underlined = underlinedRun || isUnderlinedRun(node);
		if (underlined && "t".equals(node.getLocalName())) {
			for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
				if (child.getNodeValue() != null) {
					builder.append(child.getNodeValue());
				}
			}
			return;
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			collectUnderlinedDomText(child, underlined, builder);
		}
	}

	private boolean isUnderlinedRun(Node node) {
		if (!"r".equals(node.getLocalName())) {
			return false;
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			if ("rPr".equals(child.getLocalName())) {
				for (Node property = child.getFirstChild(); property != null; property = property.getNextSibling()) {
					if ("u".equals(property.getLocalName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean hasPageBreak(Node node) {
		if (node == null) {
			return false;
		}
		if ("br".equals(node.getLocalName()) && node.getAttributes() != null) {
			Node type = node.getAttributes().getNamedItemNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "type");
			if (type != null && "page".equals(type.getNodeValue())) {
				return true;
			}
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (hasPageBreak(child)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasLocalName(Node node, String localName) {
		if (node == null) {
			return false;
		}
		if (localName.equals(node.getLocalName())) {
			return true;
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (hasLocalName(child, localName)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasTextRunSize(Node node, int halfPoints) {
		if (node == null) {
			return false;
		}
		if ("r".equals(node.getLocalName())) {
			for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
				if (!"rPr".equals(child.getLocalName())) {
					continue;
				}
				for (Node property = child.getFirstChild(); property != null; property = property.getNextSibling()) {
					if ("sz".equals(property.getLocalName()) && property.getAttributes() != null) {
						Node value = property.getAttributes().getNamedItemNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val");
						if (value != null && Integer.toString(halfPoints).equals(value.getNodeValue())) {
							return true;
						}
					}
				}
			}
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (hasTextRunSize(child, halfPoints)) {
				return true;
			}
		}
		return false;
	}

	private int occurrences(String text, String value) {
		int count = 0;
		int index = 0;
		while (text != null && value != null && !value.isEmpty() && (index = text.indexOf(value, index)) >= 0) {
			count++;
			index += value.length();
		}
		return count;
	}

	private String dumpRuns(XWPFParagraph paragraph) {
		return paragraph.getRuns().stream()
			.map(run -> "[" + run.text() + ", underline=" + run.getUnderline() + "]")
			.reduce("", String::concat);
	}
}
