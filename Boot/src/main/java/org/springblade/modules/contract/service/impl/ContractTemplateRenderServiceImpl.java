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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.TableRowHeightRule;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractTemplateRenderService;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
	private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";

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
			ensureTemplatePagination(document, templatePath.getFileName().toString());
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
					applyCompactCellStyle(cells.get(targetIndex), label, fields.get(label));
				}
			}
		}

	private void fillInlineFieldParagraphs(List<XWPFParagraph> paragraphs, Map<String, String> fields) {
		if (paragraphs == null || fields == null || fields.isEmpty()) {
			return;
		}
		for (XWPFParagraph paragraph : paragraphs) {
			String text = editableParagraphText(paragraph.getCTP().getDomNode());
			String replaced = replaceInlineFieldText(text, fields);
			if (!Objects.equals(text, replaced)) {
				replaceParagraphTextPreservingStyles(paragraph.getCTP().getDomNode(), text, replaced);
				applyCompactParagraphStyle(paragraph.getCTP().getDomNode(), replaced);
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
		if (table.getNumberOfRows() < 3) {
			return true;
		}
		int totalRowIndex = table.getNumberOfRows() - 1;
		int dataRowIndex = findNoticeFeeRow(table, fields.get("项目名称"), totalRowIndex);
		XWPFTableRow dataRow = table.getRow(dataRowIndex);
		setRowCellText(dataRow, 0, fields.get("费用期间"));
		if (StringUtil.isNotBlank(fields.get("项目名称"))) {
			setRowCellText(dataRow, 1, fields.get("项目名称"));
		}
		setRowCellText(dataRow, 2, moneyWithUnit(fields.get("应收金额")));
		setRowCellText(dataRow, 3, fields.get("应付日期"));
		setRowCellText(dataRow, 4, moneyWithUnit(fields.get("违约金")));
		setRowCellText(dataRow, 5, moneyWithUnit(fields.get("总计")));
		if (totalRowIndex > dataRowIndex) {
			XWPFTableRow totalRow = table.getRow(totalRowIndex);
			setRowCellText(totalRow, 0, "合计人民币");
			setRowCellText(totalRow, 1, moneyWithUnit(fields.get("合计人民币")));
			for (int index = 2; index < totalRow.getTableCells().size(); index++) {
				setRowCellText(totalRow, index, "");
			}
		}
		return true;
	}

	private int findNoticeFeeRow(XWPFTable table, String projectName, int totalRowIndex) {
		String normalizedProject = normalizeLabel(projectName);
		for (int rowIndex = 1; rowIndex < totalRowIndex; rowIndex++) {
			XWPFTableRow row = table.getRow(rowIndex);
			if (row == null || row.getTableCells().size() < 2) {
				continue;
			}
			String templateProject = normalizeLabel(cellText(row.getCell(1)));
			if (noticeProjectMatches(normalizedProject, templateProject)) {
				return rowIndex;
			}
		}
		return Math.min(1, Math.max(0, totalRowIndex - 1));
	}

	private boolean noticeProjectMatches(String projectName, String templateProject) {
		if (StringUtil.isBlank(projectName) || StringUtil.isBlank(templateProject)) {
			return false;
		}
		if (projectName.contains(templateProject) || templateProject.contains(projectName)) {
			return true;
		}
		return (projectName.contains("租") && templateProject.contains("租"))
			|| ((projectName.contains("押金") || projectName.contains("保证金")) && templateProject.contains("保证金"))
			|| (projectName.contains("物业") && templateProject.contains("物业"))
			|| (projectName.contains("电") && templateProject.contains("电"))
			|| (projectName.contains("水") && templateProject.contains("水"));
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
			String text = editableParagraphText(paragraph.getCTP().getDomNode());
			String replaced = replaceText(text, replacements);
			if (!Objects.equals(text, replaced)) {
				replaceParagraphTextPreservingStyles(paragraph.getCTP().getDomNode(), text, replaced);
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
		Set<Integer> filledColumns = new java.util.HashSet<>();
		for (int columnIndex = 0; columnIndex < lastCellNum; columnIndex++) {
			if (filledColumns.contains(columnIndex)) {
				continue;
			}
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
			String inlineFieldText = replaceInlineFieldText(text, fields);
			if (!Objects.equals(text, inlineFieldText)) {
				cell.setCellValue(inlineFieldText);
				applyWorkbookInlineCellStyle(cell, text);
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
			filledColumns.add(targetColumn);
		}
	}

	private void applyWorkbookInlineCellStyle(Cell cell, String sourceText) {
		if (cell == null || !normalizeLabel(sourceText).startsWith("退租理由")) {
			return;
		}
		CellStyle style = cell.getSheet().getWorkbook().createCellStyle();
		style.cloneStyleFrom(cell.getCellStyle());
		style.setShrinkToFit(true);
		cell.setCellStyle(style);
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
		String containsMatchedLabel = null;
		int containsMatchedLength = -1;
		for (String label : fields.keySet()) {
			String normalizedLabel = normalizeLabel(label);
			if (StringUtil.isBlank(normalizedLabel)) {
				continue;
			}
			if (normalizedText.equals(normalizedLabel)) {
				return label;
			}
			if (normalizedText.contains(normalizedLabel)
				&& allowContainsFieldLabelMatch(normalizedText, normalizedLabel)
				&& normalizedLabel.length() > containsMatchedLength) {
				containsMatchedLabel = label;
				containsMatchedLength = normalizedLabel.length();
			}
		}
		return containsMatchedLabel;
	}

	private boolean allowContainsFieldLabelMatch(String normalizedText, String normalizedLabel) {
		if (StringUtil.isBlank(normalizedText) || StringUtil.isBlank(normalizedLabel)) {
			return false;
		}
		if (normalizedText.contains("审批") && "部门".equals(normalizedLabel)) {
			return false;
		}
		return !isGenericManagerLabelMatchedTotalManager(normalizedText, normalizedLabel);
	}

	private boolean isGenericManagerLabelMatchedTotalManager(String normalizedText, String normalizedLabel) {
		return normalizedText.contains("总经理")
			&& ("经理".equals(normalizedLabel) || "经理审批".equals(normalizedLabel) || "部门经理".equals(normalizedLabel));
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
		String safeValue = value == null ? "" : value;
		String[] lines = safeValue.split("\\R", -1);
		List<XWPFParagraph> paragraphs = cell.getParagraphs();
		if (paragraphs.isEmpty()) {
			paragraphs = List.of(cell.addParagraph());
		}
		for (int index = 0; index < paragraphs.size(); index++) {
			setParagraphText(paragraphs.get(index), index < lines.length ? lines[index] : "");
		}
		for (int index = paragraphs.size(); index < lines.length; index++) {
			XWPFParagraph paragraph = cell.addParagraph();
			setParagraphText(paragraph, lines[index]);
		}
	}

	private void applyCompactCellStyle(XWPFTableCell cell, String label, String value) {
		if (cell == null || StringUtil.isBlank(label) || StringUtil.isBlank(value)) {
			return;
		}
		String normalizedLabel = normalizeLabel(label);
		int fontSize = -1;
		if ("合同编号".equals(normalizedLabel) && value.replaceAll("\\s+", "").length() > 18) {
			fontSize = 8;
		} else if ("合同事项".equals(normalizedLabel) && value.replaceAll("\\s+", "").length() > 24) {
			fontSize = 8;
		}
		if (fontSize <= 0) {
			return;
		}
		for (XWPFParagraph paragraph : cell.getParagraphs()) {
			for (XWPFRun run : paragraph.getRuns()) {
				if (StringUtil.isNotBlank(run.text())) {
					run.setFontSize(fontSize);
				}
			}
		}
	}

	private void applyCompactParagraphStyle(Node paragraphNode, String value) {
		int fontSize = compactFontSize(value);
		if (paragraphNode == null || fontSize <= 0) {
			return;
		}
		for (Node textNode : editableTextNodes(paragraphNode)) {
			if (StringUtil.isNotBlank(textNodeValue(textNode))) {
				setRunFontSize(ancestor(textNode, "r"), fontSize);
			}
		}
	}

	private int compactFontSize(String value) {
		if (StringUtil.isBlank(value)) {
			return -1;
		}
		String normalized = value.replaceAll("\\s+", "").trim();
		if (normalized.startsWith("合同编号：") && normalized.length() > 18) {
			return 8;
		}
		if (normalized.startsWith("合同事项：") && normalized.length() > 24) {
			return 8;
		}
		return -1;
	}

	private void setRunFontSize(Node runNode, int fontSize) {
		if (runNode == null || fontSize <= 0) {
			return;
		}
		Node runProperties = null;
		for (Node child = runNode.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (hasLocalName(child, "rPr")) {
				runProperties = child;
				break;
			}
		}
		if (runProperties == null) {
			runProperties = runNode.getOwnerDocument().createElementNS(WORD_NAMESPACE, "w:rPr");
			runNode.insertBefore(runProperties, runNode.getFirstChild());
		}
		setWordPropertyValue(runProperties, "sz", Integer.toString(fontSize * 2));
		setWordPropertyValue(runProperties, "szCs", Integer.toString(fontSize * 2));
	}

	private void setWordPropertyValue(Node properties, String propertyName, String value) {
		Node property = null;
		for (Node child = properties.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (hasLocalName(child, propertyName)) {
				property = child;
				break;
			}
		}
		if (property == null) {
			property = properties.getOwnerDocument().createElementNS(WORD_NAMESPACE, "w:" + propertyName);
			properties.appendChild(property);
		}
		((Element) property).setAttributeNS(WORD_NAMESPACE, "w:val", value);
	}

	private void ensureTemplatePagination(XWPFDocument document, String templateFileName) {
		if (document == null) {
			return;
		}
		if ("附件八：退租审批表.docx".equals(templateFileName)) {
			List<XWPFParagraph> paragraphs = document.getParagraphs();
			if (paragraphs.size() >= 2 && !hasPageBreak(paragraphs.get(1))) {
				paragraphs.get(1).createRun().addBreak(BreakType.PAGE);
			}
		}
		if ("附件四：君联大厦付款通知单.docx".equals(templateFileName)) {
			for (XWPFParagraph paragraph : document.getParagraphs()) {
				String text = editableParagraphText(paragraph.getCTP().getDomNode());
				if (!text.contains("日期（Date）:")) {
					continue;
				}
				String alignedText = text.stripLeading();
				if (!Objects.equals(text, alignedText)) {
					replaceParagraphTextPreservingStyles(paragraph.getCTP().getDomNode(), text, alignedText);
				}
				paragraph.setAlignment(ParagraphAlignment.RIGHT);
			}
		}
		if ("开票申请.docx".equals(templateFileName) && !document.getTables().isEmpty()) {
			XWPFTable table = document.getTables().get(0);
			if (table.getNumberOfRows() >= 5) {
				XWPFTableRow accountRow = table.getRow(3);
				accountRow.setHeight(3000);
				accountRow.setHeightRule(TableRowHeightRule.AT_LEAST);
				XWPFTableRow lastRow = table.getRow(4);
				preserveInvoiceCrossPageRow(lastRow);
			}
		}
	}

	private void preserveInvoiceCrossPageRow(XWPFTableRow row) {
		if (row == null || row.getTableCells().size() < 2) {
			return;
		}
		XWPFTableCell contentCell = row.getCell(1);
		if (contentCell.getParagraphs().size() == 2) {
			contentCell.getCTTc().insertNewP(1).addNewR().addNewT().setStringValue(" ");
		}
	}

	private boolean hasPageBreak(XWPFParagraph paragraph) {
		if (paragraph == null) {
			return false;
		}
		Node paragraphNode = paragraph.getCTP().getDomNode();
		for (Node node = paragraphNode.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (hasPageBreakNode(node)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasPageBreakNode(Node node) {
		if (node == null) {
			return false;
		}
		if (hasLocalName(node, "br") && node instanceof Element element
			&& "page".equals(element.getAttributeNS(WORD_NAMESPACE, "type"))) {
			return true;
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (hasPageBreakNode(child)) {
				return true;
			}
		}
		return false;
	}

	private void setParagraphText(XWPFParagraph paragraph, String value) {
		if (paragraph == null) {
			return;
		}
		Node paragraphNode = paragraph.getCTP().getDomNode();
		List<Node> textNodes = editableTextNodes(paragraphNode);
		if (textNodes.isEmpty()) {
			appendTextToFirstRun(paragraphNode, value);
		} else {
			setTextNodeValue(textNodes.get(0), value);
			for (int index = 1; index < textNodes.size(); index++) {
				setTextNodeValue(textNodes.get(index), "");
			}
		}
		for (XWPFRun run : paragraph.getRuns()) {
			if (StringUtil.isNotBlank(run.text())) {
				applyCompactFieldStyle(run, value);
				break;
			}
		}
	}

	private void applyCompactFieldStyle(XWPFRun run, String value) {
		if (run == null) {
			return;
		}
		int fontSize = compactFontSize(value);
		if (fontSize > 0) {
			run.setFontSize(fontSize);
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
			String text = editableParagraphText(root);
			String replaced = replaceText(text, replacements);
			if (!Objects.equals(text, replaced)) {
				replaceParagraphTextPreservingStyles(root, text, replaced);
			}
			return;
		}
		for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
			replaceDomParagraphs(child, replacements);
		}
	}

	private void replaceParagraphTextPreservingStyles(Node paragraphNode, String originalText, String targetText) {
		List<Node> textNodes = editableTextNodes(paragraphNode);
		if (textNodes.isEmpty()) {
			appendTextToFirstRun(paragraphNode, targetText);
			return;
		}
		String actualText = editableParagraphText(paragraphNode);
		String sourceText = Objects.equals(actualText, originalText) ? originalText : actualText;
		List<Integer> charNodeIndexes = new ArrayList<>(sourceText.length());
		for (int nodeIndex = 0; nodeIndex < textNodes.size(); nodeIndex++) {
			String value = textNodeValue(textNodes.get(nodeIndex));
			for (int index = 0; index < value.length(); index++) {
				charNodeIndexes.add(nodeIndex);
			}
		}
		if (charNodeIndexes.size() != sourceText.length()) {
			setTextNodeValue(textNodes.get(0), targetText);
			for (int index = 1; index < textNodes.size(); index++) {
				setTextNodeValue(textNodes.get(index), "");
			}
			return;
		}
		int[] sourceIndexes = alignTargetCharacters(sourceText, targetText, charNodeIndexes, textNodes);
		StringBuilder[] values = new StringBuilder[textNodes.size()];
		for (int index = 0; index < values.length; index++) {
			values[index] = new StringBuilder();
		}
		int lastNodeIndex = 0;
		for (int index = 0; index < targetText.length(); index++) {
			int nodeIndex = Math.max(lastNodeIndex, Math.min(sourceIndexes[index], textNodes.size() - 1));
			values[nodeIndex].append(targetText.charAt(index));
			lastNodeIndex = nodeIndex;
		}
		for (int index = 0; index < textNodes.size(); index++) {
			Node textNode = textNodes.get(index);
			String originalNodeValue = textNodeValue(textNode);
			String replacementNodeValue = values[index].toString();
			if (originalNodeValue.indexOf('_') >= 0
				&& replacementNodeValue.indexOf('_') < 0
				&& StringUtil.isNotBlank(replacementNodeValue)) {
				ensureRunUnderline(textNode);
			}
			setTextNodeValue(textNode, replacementNodeValue);
		}
	}

	private int[] alignTargetCharacters(String sourceText, String targetText, List<Integer> charNodeIndexes, List<Node> textNodes) {
		int sourceLength = sourceText.length();
		int targetLength = targetText.length();
		int[] sourceIndexes = new int[targetLength];
		Arrays.fill(sourceIndexes, 0);
		if (sourceLength == 0 || targetLength == 0) {
			return sourceIndexes;
		}
		int[][] lcs = buildLcs(sourceText, targetText);
		List<TextMatch> matches = new ArrayList<>();
		int sourceIndex = 0;
		int targetIndex = 0;
		while (sourceIndex < sourceLength && targetIndex < targetLength) {
			if (sourceText.charAt(sourceIndex) == targetText.charAt(targetIndex)) {
				matches.add(new TextMatch(sourceIndex++, targetIndex++));
			} else if (lcs[sourceIndex + 1][targetIndex] >= lcs[sourceIndex][targetIndex + 1]) {
				sourceIndex++;
			} else {
				targetIndex++;
			}
		}
		TextMatch previous = new TextMatch(-1, -1);
		for (int matchIndex = 0; matchIndex <= matches.size(); matchIndex++) {
			TextMatch next = matchIndex < matches.size() ? matches.get(matchIndex) : new TextMatch(sourceLength, targetLength);
			int chosenSource = chooseSlotSource(sourceText, previous.sourceIndex() + 1, next.sourceIndex() - 1,
				previous.sourceIndex(), next.sourceIndex(), charNodeIndexes, textNodes);
			for (int index = previous.targetIndex() + 1; index < next.targetIndex(); index++) {
				sourceIndexes[index] = chosenSource;
			}
			if (next.sourceIndex() < sourceLength && next.targetIndex() < targetLength) {
				sourceIndexes[next.targetIndex()] = charNodeIndexes.get(next.sourceIndex());
			}
			previous = next;
		}
		return sourceIndexes;
	}

	private int[][] buildLcs(String sourceText, String targetText) {
		int[][] result = new int[sourceText.length() + 1][targetText.length() + 1];
		for (int sourceIndex = sourceText.length() - 1; sourceIndex >= 0; sourceIndex--) {
			for (int targetIndex = targetText.length() - 1; targetIndex >= 0; targetIndex--) {
				result[sourceIndex][targetIndex] = sourceText.charAt(sourceIndex) == targetText.charAt(targetIndex)
					? result[sourceIndex + 1][targetIndex + 1] + 1
					: Math.max(result[sourceIndex + 1][targetIndex], result[sourceIndex][targetIndex + 1]);
			}
		}
		return result;
	}

	private int chooseSlotSource(String sourceText, int rangeStart, int rangeEnd, int previousIndex, int nextIndex,
								 List<Integer> charNodeIndexes, List<Node> textNodes) {
		int bestCharIndex = -1;
		int bestScore = Integer.MIN_VALUE;
		for (int index = Math.max(0, rangeStart); index <= rangeEnd && index < sourceText.length(); index++) {
			int nodeIndex = charNodeIndexes.get(index);
			int score = slotScore(sourceText.charAt(index), textNodes.get(nodeIndex));
			if (score > bestScore) {
				bestScore = score;
				bestCharIndex = index;
			}
		}
		if (bestCharIndex >= 0) {
			return charNodeIndexes.get(bestCharIndex);
		}
		if (previousIndex >= 0 && previousIndex < charNodeIndexes.size()) {
			return charNodeIndexes.get(previousIndex);
		}
		if (nextIndex >= 0 && nextIndex < charNodeIndexes.size()) {
			return charNodeIndexes.get(nextIndex);
		}
		return 0;
	}

	private int slotScore(char character, Node textNode) {
		int score = Character.isWhitespace(character) || character == '_' ? 4 : 0;
		Node run = ancestor(textNode, "r");
		if (run != null && hasDescendant(run, "u")) {
			score += 8;
		}
		return score;
	}

	private List<Node> editableTextNodes(Node root) {
		List<Node> result = new ArrayList<>();
		collectEditableTextNodes(root, result);
		return result;
	}

	private void collectEditableTextNodes(Node node, List<Node> result) {
		if (node == null) {
			return;
		}
		if (hasLocalName(node, "t")) {
			result.add(node);
			return;
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			collectEditableTextNodes(child, result);
		}
	}

	private String editableParagraphText(Node paragraphNode) {
		StringBuilder builder = new StringBuilder();
		for (Node textNode : editableTextNodes(paragraphNode)) {
			builder.append(textNodeValue(textNode));
		}
		return builder.toString();
	}

	private String textNodeValue(Node textNode) {
		if (textNode == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (Node child = textNode.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeValue() != null) {
				builder.append(child.getNodeValue());
			}
		}
		return builder.toString();
	}

	private void setTextNodeValue(Node textNode, String value) {
		String safeValue = value == null ? "" : value;
		Node firstTextChild = null;
		List<Node> extraTextChildren = new ArrayList<>();
		for (Node child = textNode.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeValue() == null) {
				continue;
			}
			if (firstTextChild == null) {
				firstTextChild = child;
			} else {
				extraTextChildren.add(child);
			}
		}
		if (firstTextChild == null) {
			textNode.appendChild(textNode.getOwnerDocument().createTextNode(safeValue));
		} else {
			firstTextChild.setNodeValue(safeValue);
			extraTextChildren.forEach(textNode::removeChild);
		}
		if (textNode instanceof Element element) {
			if (!safeValue.isEmpty() && (Character.isWhitespace(safeValue.charAt(0))
				|| Character.isWhitespace(safeValue.charAt(safeValue.length() - 1)))) {
				element.setAttributeNS(XML_NAMESPACE, "xml:space", "preserve");
			} else {
				element.removeAttributeNS(XML_NAMESPACE, "space");
			}
		}
	}

	private void appendTextToFirstRun(Node paragraphNode, String value) {
		Node runNode = firstDescendant(paragraphNode, "r");
		if (runNode == null) {
			runNode = paragraphNode.getOwnerDocument().createElementNS(WORD_NAMESPACE, "w:r");
			paragraphNode.appendChild(runNode);
		}
		Node textNode = paragraphNode.getOwnerDocument().createElementNS(WORD_NAMESPACE, "w:t");
		runNode.appendChild(textNode);
		setTextNodeValue(textNode, value);
	}

	private Node firstDescendant(Node root, String localName) {
		if (root == null) {
			return null;
		}
		if (hasLocalName(root, localName)) {
			return root;
		}
		for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
			Node result = firstDescendant(child, localName);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private Node ancestor(Node node, String localName) {
		for (Node current = node == null ? null : node.getParentNode(); current != null; current = current.getParentNode()) {
			if (hasLocalName(current, localName)) {
				return current;
			}
		}
		return null;
	}

	private boolean hasDescendant(Node root, String localName) {
		return firstDescendant(root, localName) != null;
	}

	private void ensureRunUnderline(Node textNode) {
		Node runNode = ancestor(textNode, "r");
		if (runNode == null || hasDescendant(runNode, "u")) {
			return;
		}
		Node runProperties = null;
		for (Node child = runNode.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (hasLocalName(child, "rPr")) {
				runProperties = child;
				break;
			}
		}
		if (runProperties == null) {
			runProperties = runNode.getOwnerDocument().createElementNS(WORD_NAMESPACE, "w:rPr");
			runNode.insertBefore(runProperties, runNode.getFirstChild());
		}
		Node underline = runNode.getOwnerDocument().createElementNS(WORD_NAMESPACE, "w:u");
		((Element) underline).setAttributeNS(WORD_NAMESPACE, "w:val", "single");
		runProperties.appendChild(underline);
	}

	private record TextMatch(int sourceIndex, int targetIndex) {
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
