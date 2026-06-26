/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service.impl;

import lombok.SneakyThrows;
import org.apache.poi.hwpf.HWPFDocument;
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
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractTemplateRenderService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
			for (XWPFTable table : document.getTables()) {
				fillDocxTable(table, fields, replacements);
			}
			document.getHeaderList().forEach(header -> {
				replaceParagraphs(header.getParagraphs(), replacements);
				header.getTables().forEach(table -> fillDocxTable(table, fields, replacements));
			});
			document.getFooterList().forEach(footer -> {
				replaceParagraphs(footer.getParagraphs(), replacements);
				footer.getTables().forEach(table -> fillDocxTable(table, fields, replacements));
			});
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
		replacements.forEach((key, value) -> {
			if (StringUtil.isNotBlank(key)) {
				range.replaceText(key, value == null ? "" : value);
			}
		});
	}

	private void fillDocxTable(XWPFTable table, Map<String, String> fields, Map<String, String> replacements) {
		for (XWPFTableRow row : table.getRows()) {
			List<XWPFTableCell> cells = row.getTableCells();
			for (XWPFTableCell cell : cells) {
				replaceParagraphs(cell.getParagraphs(), replacements);
				for (XWPFTable nestedTable : cell.getTables()) {
					fillDocxTable(nestedTable, fields, replacements);
				}
			}
			for (int index = 0; index < cells.size(); index++) {
				String label = matchFieldLabel(cellText(cells.get(index)), fields);
				if (StringUtil.isBlank(label) || index + 1 >= cells.size()) {
					continue;
				}
				setCellText(cells.get(index + 1), fields.get(label));
			}
		}
	}

	private void replaceParagraphs(List<XWPFParagraph> paragraphs, Map<String, String> replacements) {
		if (paragraphs == null || replacements.isEmpty()) {
			return;
		}
		for (XWPFParagraph paragraph : paragraphs) {
			String text = paragraph.getText();
			String replaced = replaceText(text, replacements);
			if (!Objects.equals(text, replaced)) {
				setParagraphText(paragraph, replaced);
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
		String wholeTextReplacement = findWholeTextReplacement(text, replacements);
		if (wholeTextReplacement != null) {
			return wholeTextReplacement;
		}
		String result = text;
		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			String key = entry.getKey();
			result = result.replace("${" + key + "}", entry.getValue())
				.replace("{{" + key + "}}", entry.getValue())
				.replace(key, entry.getValue());
		}
		return result;
	}

	private String findWholeTextReplacement(String text, Map<String, String> replacements) {
		String normalizedText = normalizeReplacementText(text);
		if (StringUtil.isBlank(normalizedText)) {
			return null;
		}
		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			String normalizedKey = normalizeReplacementText(entry.getKey());
			if (StringUtil.isNotBlank(normalizedKey) && normalizedText.equals(normalizedKey)) {
				return entry.getValue();
			}
		}
		return null;
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
		return cell.getParagraphs().stream().map(XWPFParagraph::getText).reduce("", String::concat);
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
		if (runs.isEmpty()) {
			paragraph.createRun().setText(StringUtil.isBlank(value) ? "" : value);
			return;
		}
		runs.get(0).setText(StringUtil.isBlank(value) ? "" : value, 0);
		for (int index = runs.size() - 1; index > 0; index--) {
			paragraph.removeRun(index);
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
			.trim();
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
