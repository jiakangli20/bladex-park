/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service.impl;

import lombok.SneakyThrows;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractTemplateRenderService;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 合同模板填充服务实现.
 *
 * @author BladeX
 */
@Service
public class ContractTemplateRenderServiceImpl implements IContractTemplateRenderService {

	private static final String MATERIAL_ROOT = "saber3/public/系统所需材料";
	private static final String CONTENT_TYPE_DOC = "application/msword";
	private static final String CONTENT_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static final String CONTENT_TYPE_XLS = "application/vnd.ms-excel";
	private static final String CONTENT_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private static final String EXACT_REPLACEMENT_PREFIX = "__exact__:";
	private static final String WORD_NAMESPACE = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

	@Override
	@SneakyThrows
	public ContractNoticeFileVO render(String noticeType,
									 String noticeName,
									 String templateRelativePath,
									 String fileNamePrefix,
									 Map<String, String> fields,
									 Map<String, String> replacements) {
		Path templatePath = resolveTemplate(templateRelativePath);
		String extension = extension(templatePath.getFileName().toString());
		Map<String, String> normalizedFields = normalizeMap(fields);
		Map<String, String> normalizedReplacements = createPlaceholderReplacements(normalizedFields);
		normalizedReplacements.putAll(normalizeMap(replacements));
		byte[] bytes = switch (extension) {
			case "doc" -> fillDoc(templatePath, normalizedFields, normalizedReplacements);
			case "docx" -> fillDocx(templatePath, normalizedFields, normalizedReplacements);
			case "xls", "xlsx" -> fillWorkbook(templatePath, normalizedFields, normalizedReplacements);
			default -> throw new ServiceException("暂不支持该模板格式：" + extension + "，请先转换为 doc/docx/xls/xlsx");
		};

		ContractNoticeFileVO vo = new ContractNoticeFileVO();
		vo.setNoticeType(noticeType);
		vo.setNoticeName(noticeName);
		vo.setFileName(buildFileName(fileNamePrefix, extension));
		vo.setContentType(contentType(extension));
		vo.setGeneratedAt(DateUtil.formatDateTime(new Date()));
		vo.setFileBytes(bytes);
		return vo;
	}

	private byte[] fillDocx(Path templatePath, Map<String, String> fields, Map<String, String> replacements) throws Exception {
		try (InputStream input = Files.newInputStream(templatePath);
			 XWPFDocument document = new XWPFDocument(input);
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
				replaceParagraphs(document.getParagraphs(), replacements);
				fillInlineFieldParagraphs(document.getParagraphs(), fields);
				fillSignatureDate(document.getParagraphs(), fields);
				for (XWPFTable table : document.getTables()) {
					fillDocxTable(table, fields, replacements);
				}
				document.getHeaderList().forEach(header -> {
					replaceParagraphs(header.getParagraphs(), replacements);
					fillInlineFieldParagraphs(header.getParagraphs(), fields);
					fillSignatureDate(header.getParagraphs(), fields);
					header.getTables().forEach(table -> fillDocxTable(table, fields, replacements));
				});
				document.getFooterList().forEach(footer -> {
					replaceParagraphs(footer.getParagraphs(), replacements);
					fillInlineFieldParagraphs(footer.getParagraphs(), fields);
					fillSignatureDate(footer.getParagraphs(), fields);
					footer.getTables().forEach(table -> fillDocxTable(table, fields, replacements));
				});
			replaceDomParagraphs(document.getDocument().getBody().getDomNode(), replacements);
			document.write(output);
			return output.toByteArray();
		}
	}

	private byte[] fillDoc(Path templatePath, Map<String, String> fields, Map<String, String> replacements) throws Exception {
		try (InputStream input = Files.newInputStream(templatePath);
			 HWPFDocument document = new HWPFDocument(input);
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Map<String, String> mergedReplacements = new LinkedHashMap<>(replacements);
			fields.forEach((key, value) -> {
				mergedReplacements.put("${" + key + "}", value);
				mergedReplacements.put("{{" + key + "}}", value);
			});
			replaceDocRange(document.getRange(), mergedReplacements);
			replaceDocRange(document.getHeaderStoryRange(), mergedReplacements);
			replaceDocRange(document.getFootnoteRange(), mergedReplacements);
			replaceDocRange(document.getEndnoteRange(), mergedReplacements);
			replaceDocRange(document.getCommentsRange(), mergedReplacements);
			document.write(output);
			return output.toByteArray();
		}
	}

	private void replaceDocRange(Range range, Map<String, String> replacements) {
		if (range == null || replacements.isEmpty()) {
			return;
		}
		replaceDocParagraphs(range, replacements);
		replacements.forEach((key, value) -> {
			if (StringUtil.isNotBlank(key) && !isExactReplacement(key)) {
				range.replaceText(key, value == null ? "" : value);
			}
		});
	}

	private void replaceDocParagraphs(Range range, Map<String, String> replacements) {
		for (int index = 0; index < range.numParagraphs(); index++) {
			Paragraph paragraph = range.getParagraph(index);
			String text = trimParagraphEnd(paragraph.text());
			String replacement = findExactWholeTextReplacement(text, replacements);
			if (replacement != null && StringUtil.isNotBlank(text)) {
				paragraph.replaceText(text, replacement);
			}
		}
	}

	private void fillDocxTable(XWPFTable table, Map<String, String> fields, Map<String, String> replacements) {
		if (fillNoticeFeeTable(table, fields)) {
			return;
		}
		for (XWPFTableRow row : table.getRows()) {
			List<XWPFTableCell> cells = row.getTableCells();
				for (XWPFTableCell cell : cells) {
					replaceParagraphs(cell.getParagraphs(), replacements);
					fillInlineFieldParagraphs(cell.getParagraphs(), fields);
					fillSignatureDate(cell.getParagraphs(), fields);
					for (XWPFTable nestedTable : cell.getTables()) {
						fillDocxTable(nestedTable, fields, replacements);
					}
				}
				for (int index = 0; index < cells.size(); index++) {
					String label = matchFieldLabel(cellText(cells.get(index)), fields);
					int targetIndex = nextDocxValueCell(cells, index, fields);
					if (StringUtil.isBlank(label) || targetIndex < 0) {
						continue;
					}
					setCellText(cells.get(targetIndex), fields.get(label));
				}
			}
		}

	private void fillInlineFieldParagraphs(List<XWPFParagraph> paragraphs, Map<String, String> fields) {
		if (paragraphs == null || fields == null || fields.isEmpty()) {
			return;
		}
		for (XWPFParagraph paragraph : paragraphs) {
			String text = paragraphText(paragraph);
			String replaced = replaceInlineFieldText(text, fields);
			if (!Objects.equals(text, replaced)) {
				setParagraphText(paragraph, replaced);
			}
		}
	}

	private String replaceInlineFieldText(String text, Map<String, String> fields) {
		if (StringUtil.isBlank(text) || fields == null || fields.isEmpty()) {
			return text;
		}
		int delimiterIndex = firstFieldDelimiter(text);
		if (delimiterIndex < 0 || StringUtil.isNotBlank(text.substring(delimiterIndex + 1))) {
			return text;
		}
		String normalizedText = normalizeLabel(text);
		for (Map.Entry<String, String> entry : fields.entrySet()) {
			String normalizedLabel = normalizeLabel(entry.getKey());
			if (StringUtil.isNotBlank(normalizedLabel) && normalizedText.equals(normalizedLabel)) {
				return text.substring(0, delimiterIndex + 1) + entry.getValue();
			}
		}
		return text;
	}

	private int nextDocxValueCell(List<XWPFTableCell> cells, int labelIndex, Map<String, String> fields) {
		if (cells == null || labelIndex < 0 || labelIndex + 1 >= cells.size()) {
			return -1;
		}
		for (int index = labelIndex + 1; index < cells.size(); index++) {
			String text = cellText(cells.get(index));
			if (StringUtil.isBlank(text)) {
				return index;
			}
			if (isInlineFieldLabel(text, fields) || StringUtil.isNotBlank(matchFieldLabel(text, fields))) {
				return -1;
			}
			if (isValueUnitMarker(text)) {
				continue;
			}
			return -1;
		}
		return -1;
	}

	private boolean isInlineFieldLabel(String text, Map<String, String> fields) {
		return !Objects.equals(text, replaceInlineFieldText(text, fields));
	}

	private boolean isValueUnitMarker(String text) {
		String normalized = normalizeLabel(text).toLowerCase(Locale.ROOT);
		return normalized.contains("人民币") || normalized.contains("rmb") || normalized.equals("元");
	}

	private int firstFieldDelimiter(String text) {
		if (text == null) {
			return -1;
		}
		int chineseDelimiter = text.indexOf('：');
		int englishDelimiter = text.indexOf(':');
		if (chineseDelimiter < 0) {
			return englishDelimiter;
		}
		if (englishDelimiter < 0) {
			return chineseDelimiter;
		}
		return Math.min(chineseDelimiter, englishDelimiter);
	}

	private boolean fillNoticeFeeTable(XWPFTable table, Map<String, String> fields) {
		if (table == null || fields == null || fields.isEmpty() || !isNoticeFeeTable(table)) {
			return false;
		}
		while (table.getNumberOfRows() > 3) {
			table.removeRow(2);
		}
		if (table.getNumberOfRows() < 2) {
			return true;
		}
		XWPFTableRow dataRow = table.getRow(1);
		setRowCellText(dataRow, 0, fields.get("费用期间"));
		setRowCellText(dataRow, 1, fields.get("项目名称"));
		setRowCellText(dataRow, 2, moneyWithUnit(fields.get("应收金额")));
		setRowCellText(dataRow, 3, fields.get("应付日期"));
		setRowCellText(dataRow, 4, moneyWithUnit(fields.get("违约金")));
		setRowCellText(dataRow, 5, moneyWithUnit(fields.get("总计")));
		if (table.getNumberOfRows() > 2) {
			XWPFTableRow totalRow = table.getRow(2);
			setRowCellText(totalRow, 0, "合计人民币");
			setRowCellText(totalRow, 1, moneyWithUnit(fields.get("合计人民币")));
			for (int index = 2; index < totalRow.getTableCells().size(); index++) {
				setRowCellText(totalRow, index, "");
			}
		}
		return true;
	}

	private boolean isNoticeFeeTable(XWPFTable table) {
		if (table == null || table.getNumberOfRows() == 0) {
			return false;
		}
		String header = table.getRow(0).getTableCells().stream()
			.map(this::cellText)
			.reduce("", (left, right) -> left + "|" + right);
		String normalizedHeader = normalizeLabel(header);
		return normalizedHeader.contains("费用期间")
			&& normalizedHeader.contains("项目名称")
			&& normalizedHeader.contains("应收金额")
			&& normalizedHeader.contains("应付日期")
			&& normalizedHeader.contains("违约金")
			&& normalizedHeader.contains("总计");
	}

	private void setRowCellText(XWPFTableRow row, int index, String value) {
		if (row == null || index < 0 || index >= row.getTableCells().size()) {
			return;
		}
		setCellText(row.getCell(index), value);
	}

	private String moneyWithUnit(String value) {
		if (StringUtil.isBlank(value)) {
			return "";
		}
		String normalized = value.trim();
		if (normalized.endsWith("元")) {
			return normalized;
		}
		return normalized + "元";
	}

	private void replaceParagraphs(List<XWPFParagraph> paragraphs, Map<String, String> replacements) {
		if (paragraphs == null || replacements.isEmpty()) {
			return;
		}
		for (XWPFParagraph paragraph : paragraphs) {
			String text = paragraphText(paragraph);
			String replaced = replaceText(text, replacements);
			if (!Objects.equals(text, replaced)) {
				setParagraphText(paragraph, replaced);
			}
		}
	}

	private void fillSignatureDate(List<XWPFParagraph> paragraphs, Map<String, String> fields) {
		if (paragraphs == null || paragraphs.size() < 2 || fields == null) {
			return;
		}
		String noticeDate = fields.get("通知日期");
		if (StringUtil.isBlank(noticeDate)) {
			return;
		}
		String company = normalizeReplacementText("苏州市吴中金融招商服务有限公司");
		for (int index = 0; index < paragraphs.size() - 1; index++) {
			XWPFParagraph current = paragraphs.get(index);
			XWPFParagraph next = paragraphs.get(index + 1);
			String currentText = normalizeReplacementText(paragraphText(current));
			String nextText = normalizeReplacementText(paragraphText(next));
			if (company.equals(currentText) && (StringUtil.isBlank(nextText) || "年月日".equals(nextText))) {
				setParagraphText(next, noticeDate);
			}
		}
	}

	private byte[] fillWorkbook(Path templatePath, Map<String, String> fields, Map<String, String> replacements) throws Exception {
		try (InputStream input = Files.newInputStream(templatePath);
			 Workbook workbook = WorkbookFactory.create(input);
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			DataFormatter formatter = new DataFormatter(Locale.CHINA);
			for (Sheet sheet : workbook) {
				for (Row row : sheet) {
					fillWorkbookRow(sheet, row, fields, replacements, formatter);
				}
			}
			workbook.write(output);
			return output.toByteArray();
		}
	}

	private void fillWorkbookRow(Sheet sheet, Row row, Map<String, String> fields, Map<String, String> replacements, DataFormatter formatter) {
		short lastCellNum = row.getLastCellNum();
		if (lastCellNum < 0) {
			return;
		}
		for (int columnIndex = 0; columnIndex < lastCellNum; columnIndex++) {
			Cell cell = row.getCell(columnIndex);
			String text = cellText(cell, formatter);
			if (StringUtil.isBlank(text)) {
				continue;
			}
			String replaced = replaceText(text, replacements);
			if (!Objects.equals(text, replaced)) {
				cell.setCellValue(replaced);
				continue;
			}
			String label = matchFieldLabel(text, fields);
			if (StringUtil.isBlank(label)) {
				continue;
			}
			int targetColumn = nextEditableColumn(sheet, row.getRowNum(), columnIndex);
			Cell targetCell = row.getCell(targetColumn);
			if (targetCell == null) {
				targetCell = row.createCell(targetColumn);
			}
			targetCell.setCellValue(fields.get(label));
		}
	}

	private String replaceText(String text, Map<String, String> replacements) {
		if (StringUtil.isBlank(text) || replacements.isEmpty()) {
			return text;
		}
		String wholeTextReplacement = findExactWholeTextReplacement(text, replacements);
		if (wholeTextReplacement != null) {
			return wholeTextReplacement;
		}
		String result = text;
		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			String key = entry.getKey();
			if (isExactReplacement(key)) {
				continue;
			}
			result = result.replace("${" + key + "}", entry.getValue())
				.replace("{{" + key + "}}", entry.getValue())
				.replace(key, entry.getValue());
		}
		return result;
	}

	private String findExactWholeTextReplacement(String text, Map<String, String> replacements) {
		String normalizedText = normalizeReplacementText(text);
		if (StringUtil.isBlank(normalizedText)) {
			return null;
		}
		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			if (!isExactReplacement(entry.getKey())) {
				continue;
			}
			String normalizedKey = normalizeReplacementText(replacementKey(entry.getKey()));
			if (StringUtil.isNotBlank(normalizedKey) && normalizedText.equals(normalizedKey)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private boolean isExactReplacement(String key) {
		return key != null && key.startsWith(EXACT_REPLACEMENT_PREFIX);
	}

	private String replacementKey(String key) {
		return isExactReplacement(key) ? key.substring(EXACT_REPLACEMENT_PREFIX.length()) : key;
	}

	private String matchFieldLabel(String text, Map<String, String> fields) {
		if (StringUtil.isBlank(text) || fields.isEmpty()) {
			return null;
		}
		String normalizedText = normalizeLabel(text);
		for (String label : fields.keySet()) {
			String normalizedLabel = normalizeLabel(label);
			if (StringUtil.isBlank(normalizedLabel)) {
				continue;
			}
			if (normalizedText.equals(normalizedLabel) || normalizedText.contains(normalizedLabel)) {
				return label;
			}
		}
		return null;
	}

	private int nextEditableColumn(Sheet sheet, int rowIndex, int columnIndex) {
		for (CellRangeAddress mergedRegion : sheet.getMergedRegions()) {
			if (mergedRegion.isInRange(rowIndex, columnIndex)) {
				return mergedRegion.getLastColumn() + 1;
			}
		}
		return columnIndex + 1;
	}

	private String cellText(XWPFTableCell cell) {
		if (cell == null) {
			return "";
		}
		return cell.getParagraphs().stream().map(this::paragraphText).reduce("", String::concat);
	}

	private String cellText(Cell cell, DataFormatter formatter) {
		return cell == null ? "" : formatter.formatCellValue(cell);
	}

	private void setCellText(XWPFTableCell cell, String value) {
		if (cell == null) {
			return;
		}
		XWPFParagraph paragraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
		setParagraphText(paragraph, value);
		for (int index = cell.getParagraphs().size() - 1; index > 0; index--) {
			cell.removeParagraph(index);
		}
	}

	private void setParagraphText(XWPFParagraph paragraph, String value) {
		if (paragraph == null) {
			return;
		}
		List<XWPFRun> runs = paragraph.getRuns();
		RunStyle style = runs.isEmpty() ? RunStyle.empty() : RunStyle.from(runs.get(0));
		for (int index = runs.size() - 1; index > 0; index--) {
			paragraph.removeRun(index);
		}
		if (!runs.isEmpty()) {
			paragraph.removeRun(0);
		}
		removeParagraphContentControls(paragraph);
			XWPFRun run = paragraph.createRun();
			style.apply(run);
			applyCompactFieldStyle(run, value);
			writeRunText(run, value);
		}

		private void applyCompactFieldStyle(XWPFRun run, String value) {
			if (run == null || StringUtil.isBlank(value)) {
				return;
			}
			String normalized = value.replaceAll("\\s+", "").trim();
			if (normalized.startsWith("合同编号：") && normalized.length() > 18) {
				run.setFontSize(8);
			} else if (normalized.startsWith("合同事项：") && normalized.length() > 24) {
				run.setFontSize(8);
			}
		}

		private void removeParagraphContentControls(XWPFParagraph paragraph) {
		Node paragraphNode = paragraph.getCTP().getDomNode();
		List<Node> contentControls = new ArrayList<>();
		for (Node child = paragraphNode.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (hasLocalName(child, "sdt")) {
				contentControls.add(child);
			}
		}
		contentControls.forEach(paragraphNode::removeChild);
	}

	private void writeRunText(XWPFRun run, String value) {
		String safeValue = StringUtil.isBlank(value) ? "" : value;
		String[] lines = safeValue.split("\\R", -1);
		if (lines.length == 0) {
			run.setText("");
			return;
		}
		run.setText(lines[0]);
		for (int index = 1; index < lines.length; index++) {
			run.addBreak();
			run.setText(lines[index]);
		}
	}

	private record RunStyle(String fontFamily,
							int fontSize,
							String color,
							boolean bold,
							boolean italic,
							UnderlinePatterns underline) {
		private static RunStyle empty() {
			return new RunStyle(null, -1, null, false, false, UnderlinePatterns.NONE);
		}

		private static RunStyle from(XWPFRun run) {
			return new RunStyle(
				run.getFontFamily(),
				run.getFontSize(),
				run.getColor(),
				run.isBold(),
				run.isItalic(),
				run.getUnderline()
			);
		}

		private void apply(XWPFRun run) {
			if (StringUtil.isNotBlank(fontFamily)) {
				run.setFontFamily(fontFamily);
			}
			if (fontSize > 0) {
				run.setFontSize(fontSize);
			}
			if (StringUtil.isNotBlank(color)) {
				run.setColor(color);
			}
			run.setBold(bold);
			run.setItalic(italic);
			if (underline != null && underline != UnderlinePatterns.NONE) {
				run.setUnderline(underline);
			}
		}
	}

	private Map<String, String> createPlaceholderReplacements(Map<String, String> fields) {
		Map<String, String> result = new LinkedHashMap<>();
		if (fields == null || fields.isEmpty()) {
			return result;
		}
		fields.forEach((key, value) -> {
			if (StringUtil.isNotBlank(key)) {
				result.put("{{" + key + "}}", value == null ? "" : value);
				result.put("${" + key + "}", value == null ? "" : value);
			}
		});
		return result;
	}

	private Map<String, String> normalizeMap(Map<String, String> source) {
		Map<String, String> result = new LinkedHashMap<>();
		if (source == null || source.isEmpty()) {
			return result;
		}
		source.forEach((key, value) -> {
			if (StringUtil.isNotBlank(key)) {
				result.put(key, value == null ? "" : value);
			}
		});
		return result;
	}

	private String normalizeLabel(String text) {
		return text == null ? "" : text
			.replace("：", "")
			.replace(":", "")
			.replace(" ", "")
			.replace("\u00a0", "")
			.replace("\n", "")
			.replace("\r", "")
			.trim();
	}

	private String normalizeReplacementText(String text) {
		return text == null ? "" : text
			.replaceAll("\\s+", "")
			.replace("\u00a0", "")
			.replace("\u0007", "")
			.trim();
	}

	private String trimParagraphEnd(String text) {
		if (text == null) {
			return "";
		}
		return text.replaceAll("[\\r\\n\\u0007]+$", "");
	}

	private void replaceDomParagraphs(Node root, Map<String, String> replacements) {
		if (root == null || replacements == null || replacements.isEmpty()) {
			return;
		}
		if (hasLocalName(root, "p")) {
			String text = domParagraphText(root);
			String replaced = findExactWholeTextReplacement(text, replacements);
			if (replaced != null && !Objects.equals(text, replaced)) {
				setDomParagraphText(root, replaced);
			}
			return;
		}
		for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
			replaceDomParagraphs(child, replacements);
		}
	}

	private String domParagraphText(Node paragraphNode) {
		StringBuilder builder = new StringBuilder();
		appendNodeText(paragraphNode, builder);
		return builder.toString();
	}

	private void setDomParagraphText(Node paragraphNode, String value) {
		List<Node> removeNodes = new ArrayList<>();
		for (Node child = paragraphNode.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (!hasLocalName(child, "pPr")) {
				removeNodes.add(child);
			}
		}
		removeNodes.forEach(paragraphNode::removeChild);
		org.w3c.dom.Document owner = paragraphNode.getOwnerDocument();
		Node runNode = owner.createElementNS(WORD_NAMESPACE, "w:r");
		writeDomRunText(owner, runNode, value);
		paragraphNode.appendChild(runNode);
	}

	private void writeDomRunText(org.w3c.dom.Document owner, Node runNode, String value) {
		String safeValue = StringUtil.isBlank(value) ? "" : value;
		String[] lines = safeValue.split("\\R", -1);
		if (lines.length == 0) {
			appendDomText(owner, runNode, "");
			return;
		}
		appendDomText(owner, runNode, lines[0]);
		for (int index = 1; index < lines.length; index++) {
			runNode.appendChild(owner.createElementNS(WORD_NAMESPACE, "w:br"));
			appendDomText(owner, runNode, lines[index]);
		}
	}

	private void appendDomText(org.w3c.dom.Document owner, Node runNode, String value) {
		Node textNode = owner.createElementNS(WORD_NAMESPACE, "w:t");
		textNode.appendChild(owner.createTextNode(value == null ? "" : value));
		runNode.appendChild(textNode);
	}

	private String paragraphText(XWPFParagraph paragraph) {
		if (paragraph == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		appendNodeText(paragraph.getCTP().getDomNode(), builder);
		return builder.toString();
	}

	private void appendNodeText(Node node, StringBuilder builder) {
		if (node == null) {
			return;
		}
		String localName = node.getLocalName();
		if (hasLocalName(node, "t") || hasLocalName(node, "instrText") || hasLocalName(node, "delText")) {
			for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
				if (child.getNodeValue() != null) {
					builder.append(child.getNodeValue());
				}
			}
			return;
		}
		if (hasLocalName(node, "tab")) {
			builder.append('\t');
			return;
		}
		if (hasLocalName(node, "br") || hasLocalName(node, "cr")) {
			builder.append('\n');
			return;
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			appendNodeText(child, builder);
		}
	}

	private boolean hasLocalName(Node node, String localName) {
		if (node == null || localName == null) {
			return false;
		}
		return localName.equals(node.getLocalName()) || ("w:" + localName).equals(node.getNodeName());
	}

	private Path resolveTemplate(String templateRelativePath) {
		if (StringUtil.isBlank(templateRelativePath)) {
			throw new ServiceException("模板路径不能为空");
		}
		Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		for (Path cursor = userDir; cursor != null; cursor = cursor.getParent()) {
			Path candidate = cursor.resolve(MATERIAL_ROOT).resolve(templateRelativePath);
			if (Files.exists(candidate)) {
				return candidate;
			}
		}
		throw new ServiceException("模板文件不存在：" + templateRelativePath);
	}

	private String extension(String fileName) {
		int index = fileName.lastIndexOf('.');
		if (index < 0 || index == fileName.length() - 1) {
			return "";
		}
		return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
	}

	private String contentType(String extension) {
		return switch (extension) {
			case "doc" -> CONTENT_TYPE_DOC;
			case "docx" -> CONTENT_TYPE_DOCX;
			case "xlsx" -> CONTENT_TYPE_XLSX;
			case "xls" -> CONTENT_TYPE_XLS;
			default -> "application/octet-stream";
		};
	}

	private String buildFileName(String prefix, String extension) {
		String safePrefix = StringUtil.isBlank(prefix) ? "合同文书" : prefix.replaceAll("[\\\\/:*?\"<>|]", "-");
		return safePrefix + "-" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + "." + extension;
	}

}
