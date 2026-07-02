/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service.impl;

import lombok.SneakyThrows;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 合同文书预览渲染服务.
 *
 * @author BladeX
 */
@Service
public class ContractDocumentPreviewService {

	private static final int MAX_PREVIEW_ROWS = 160;
	private static final int MAX_PREVIEW_COLUMNS = 32;

	@SneakyThrows
	public String render(ContractNoticeFileVO document, Map<String, String> summary, List<String> missingFields) {
		String extension = extension(document == null ? null : document.getFileName());
		String body = switch (extension) {
			case "docx" -> renderDocx(document.getFileBytes());
			case "doc" -> renderDoc(document.getFileBytes());
			case "xls", "xlsx" -> renderWorkbook(document.getFileBytes());
			default -> renderUnsupported(document);
		};
		return wrapDocument(document, summary, missingFields, body, extension);
	}

	private String renderDocx(byte[] fileBytes) throws Exception {
		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
			StringBuilder html = new StringBuilder();
			html.append("<div class=\"word-page\">");
			for (IBodyElement element : document.getBodyElements()) {
				switch (element.getElementType()) {
					case PARAGRAPH -> html.append(renderParagraph((XWPFParagraph) element));
					case TABLE -> html.append(renderTable((XWPFTable) element));
					default -> {
					}
				}
			}
			html.append("</div>");
			return html.toString();
		}
	}

	private String renderDoc(byte[] fileBytes) throws Exception {
		try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(fileBytes))) {
			String text = document.getRange() == null ? "" : document.getRange().text();
			StringBuilder html = new StringBuilder();
			html.append("<div class=\"word-page\">");
			for (String line : text.split("\\r?\\n")) {
				if (StringUtil.isBlank(line)) {
					html.append("<p class=\"word-paragraph word-blank\">&nbsp;</p>");
				} else {
					html.append("<p class=\"word-paragraph\">").append(escapeHtml(line)).append("</p>");
				}
			}
			html.append("</div>");
			return html.toString();
		}
	}

	private String renderParagraph(XWPFParagraph paragraph) {
		String text = paragraph == null ? "" : paragraph.getText();
		if (StringUtil.isBlank(text)) {
			return "<p class=\"word-paragraph word-blank\">&nbsp;</p>";
		}
		StringBuilder style = new StringBuilder();
		ParagraphAlignment alignment = paragraph.getAlignment();
		if (alignment == ParagraphAlignment.CENTER) {
			style.append("text-align:center;");
		} else if (alignment == ParagraphAlignment.RIGHT) {
			style.append("text-align:right;");
		} else if (alignment == ParagraphAlignment.BOTH || alignment == ParagraphAlignment.DISTRIBUTE) {
			style.append("text-align:justify;");
		}
		if (paragraph.getSpacingAfter() > 0) {
			style.append("margin-bottom:").append(Math.min(paragraph.getSpacingAfter() / 20, 24)).append("pt;");
		}
		StringBuilder html = new StringBuilder();
		html.append("<p class=\"word-paragraph\"");
		if (!style.isEmpty()) {
			html.append(" style=\"").append(style).append("\"");
		}
		html.append(">");
		if (paragraph.getRuns().isEmpty()) {
			html.append(escapeHtml(text));
		} else {
			for (XWPFRun run : paragraph.getRuns()) {
				html.append(renderRun(run));
			}
		}
		html.append("</p>");
		return html.toString();
	}

	private String renderRun(XWPFRun run) {
		String text = run == null ? "" : run.toString();
		if (StringUtil.isBlank(text)) {
			return "";
		}
		StringBuilder style = new StringBuilder();
		if (run.isBold()) {
			style.append("font-weight:700;");
		}
		if (run.isItalic()) {
			style.append("font-style:italic;");
		}
		if (run.getUnderline() != null && run.getUnderline().getValue() > 0) {
			style.append("text-decoration:underline;");
		}
		if (run.getFontSize() > 0) {
			style.append("font-size:").append(run.getFontSize()).append("pt;");
		}
		if (StringUtil.isNotBlank(run.getColor()) && !"auto".equalsIgnoreCase(run.getColor())) {
			style.append("color:#").append(run.getColor()).append(";");
		}
		String value = escapeHtml(text).replace("\t", "&emsp;").replace("\n", "<br>");
		if (style.isEmpty()) {
			return value;
		}
		return "<span style=\"" + style + "\">" + value + "</span>";
	}

	private String renderTable(XWPFTable table) {
		StringBuilder html = new StringBuilder();
		html.append("<table class=\"word-table\"><tbody>");
		for (XWPFTableRow row : table.getRows()) {
			html.append("<tr>");
			for (XWPFTableCell cell : row.getTableCells()) {
				html.append("<td>");
				if (cell.getBodyElements().isEmpty()) {
					html.append("&nbsp;");
				} else {
					for (IBodyElement element : cell.getBodyElements()) {
						switch (element.getElementType()) {
							case PARAGRAPH -> html.append(renderParagraph((XWPFParagraph) element));
							case TABLE -> html.append(renderTable((XWPFTable) element));
							default -> {
							}
						}
					}
				}
				html.append("</td>");
			}
			html.append("</tr>");
		}
		html.append("</tbody></table>");
		return html.toString();
	}

	private String renderWorkbook(byte[] fileBytes) throws Exception {
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
			DataFormatter formatter = new DataFormatter(Locale.CHINA);
			StringBuilder html = new StringBuilder();
			html.append("<div class=\"workbook-preview\">");
			for (Sheet sheet : workbook) {
				html.append("<section class=\"sheet-page\"><h3>").append(escapeHtml(sheet.getSheetName())).append("</h3>");
				html.append("<table class=\"excel-table\"><tbody>");
				Set<String> skippedCells = new HashSet<>();
				int lastRow = Math.min(sheet.getLastRowNum(), MAX_PREVIEW_ROWS);
				for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
					Row row = sheet.getRow(rowIndex);
					if (row == null) {
						continue;
					}
					html.append("<tr>");
					int lastCell = Math.min(Math.max(row.getLastCellNum(), 0), MAX_PREVIEW_COLUMNS);
					for (int columnIndex = 0; columnIndex < lastCell; columnIndex++) {
						String key = rowIndex + ":" + columnIndex;
						if (skippedCells.contains(key)) {
							continue;
						}
						CellRangeAddress merged = mergedRegion(sheet, rowIndex, columnIndex);
						if (merged != null && (merged.getFirstRow() != rowIndex || merged.getFirstColumn() != columnIndex)) {
							continue;
						}
						int rowSpan = merged == null ? 1 : merged.getLastRow() - merged.getFirstRow() + 1;
						int colSpan = merged == null ? 1 : merged.getLastColumn() - merged.getFirstColumn() + 1;
						markSkipped(skippedCells, rowIndex, columnIndex, rowSpan, colSpan);
						html.append("<td");
						if (rowSpan > 1) {
							html.append(" rowspan=\"").append(rowSpan).append("\"");
						}
						if (colSpan > 1) {
							html.append(" colspan=\"").append(colSpan).append("\"");
						}
						html.append(">");
						Cell cell = row.getCell(columnIndex);
						String value = cell == null ? "" : formatter.formatCellValue(cell);
						html.append(StringUtil.isBlank(value) ? "&nbsp;" : escapeHtml(value).replace("\n", "<br>"));
						html.append("</td>");
					}
					html.append("</tr>");
				}
				html.append("</tbody></table></section>");
			}
			html.append("</div>");
			return html.toString();
		}
	}

	private CellRangeAddress mergedRegion(Sheet sheet, int rowIndex, int columnIndex) {
		for (CellRangeAddress region : sheet.getMergedRegions()) {
			if (region.isInRange(rowIndex, columnIndex)) {
				return region;
			}
		}
		return null;
	}

	private void markSkipped(Set<String> skippedCells, int rowIndex, int columnIndex, int rowSpan, int colSpan) {
		for (int rowOffset = 0; rowOffset < rowSpan; rowOffset++) {
			for (int columnOffset = 0; columnOffset < colSpan; columnOffset++) {
				if (rowOffset != 0 || columnOffset != 0) {
					skippedCells.add((rowIndex + rowOffset) + ":" + (columnIndex + columnOffset));
				}
			}
		}
	}

	private String renderUnsupported(ContractNoticeFileVO document) {
		return "<div class=\"word-page\"><p class=\"word-paragraph\">暂不支持该文件格式预览，请下载原文件查看："
			+ escapeHtml(document == null ? "" : document.getFileName()) + "</p></div>";
	}

	private String wrapDocument(ContractNoticeFileVO document, Map<String, String> summary, List<String> missingFields, String body, String extension) {
		String summaryHtml = summary == null || summary.isEmpty() ? "" : summary.entrySet().stream()
			.map(entry -> "<span><em>" + escapeHtml(entry.getKey()) + "</em>" + escapeHtml(entry.getValue()) + "</span>")
			.reduce("", String::concat);
		String missingHtml = missingFields == null || missingFields.isEmpty()
			? "<div class=\"preview-status preview-status-success\">字段已完整回填</div>"
			: "<div class=\"preview-status preview-status-warning\">待补字段：" + escapeHtml(String.join("、", missingFields)) + "</div>";
		return "<div class=\"contract-document-preview\">"
			+ "<style>"
			+ ".contract-document-preview{min-height:100%;padding:22px;background:#eef1f5;color:#111827;font-family:Arial,'Microsoft YaHei',sans-serif;}"
			+ ".preview-toolbar{max-width:980px;margin:0 auto 14px;display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:13px;color:#4b5563;}"
			+ ".preview-toolbar strong{font-size:16px;color:#111827;}.preview-toolbar code{padding:4px 8px;border-radius:6px;background:#fff;border:1px solid #d7dce5;color:#374151;}"
			+ ".preview-summary{max-width:980px;margin:0 auto 12px;display:flex;flex-wrap:wrap;gap:8px;}"
			+ ".preview-summary span{display:inline-flex;gap:6px;align-items:center;padding:7px 10px;border:1px solid #dfe4ec;border-radius:6px;background:#fff;font-size:12px;}"
			+ ".preview-summary em{font-style:normal;color:#6b7280;}"
			+ ".preview-status{max-width:980px;margin:0 auto 14px;padding:10px 12px;border-radius:6px;font-size:13px;}"
			+ ".preview-status-success{background:#f0fdf4;border:1px solid #bbf7d0;color:#15803d;}.preview-status-warning{background:#fff7ed;border:1px solid #fdba74;color:#c2410c;}"
			+ ".word-page,.sheet-page{box-sizing:border-box;max-width:980px;min-height:1120px;margin:0 auto 18px;padding:52px 58px;background:#fff;border:1px solid #d8dde6;box-shadow:0 10px 30px rgba(15,23,42,.12);}"
			+ ".word-paragraph{margin:0 0 8px;line-height:1.75;font-size:14px;white-space:pre-wrap;}.word-blank{min-height:18px;}"
			+ ".word-table,.excel-table{width:100%;border-collapse:collapse;margin:8px 0 12px;table-layout:fixed;font-size:13px;}.word-table td,.excel-table td{border:1px solid #3f3f46;padding:6px 8px;vertical-align:middle;word-break:break-word;line-height:1.55;}"
			+ ".word-table .word-paragraph{margin:0;line-height:1.55;}.workbook-preview{max-width:100%;overflow:auto;}.sheet-page{min-height:0;max-width:max-content;min-width:860px;padding:28px;}"
			+ ".sheet-page h3{margin:0 0 14px;text-align:left;font-size:16px;}.excel-table{width:auto;min-width:820px;}.excel-table td{min-width:92px;white-space:pre-wrap;background:#fff;}"
			+ "@media (max-width:900px){.contract-document-preview{padding:12px}.word-page,.sheet-page{padding:24px 18px;min-width:0;}.preview-toolbar{flex-direction:column;align-items:flex-start;}}"
			+ "</style>"
			+ "<div class=\"preview-toolbar\"><strong>" + escapeHtml(document == null ? "文书预览" : document.getNoticeName()) + "</strong><code>"
			+ escapeHtml(extension == null ? "" : extension.toUpperCase(Locale.ROOT)) + " 原文件预览</code></div>"
			+ "<div class=\"preview-summary\">" + summaryHtml + "</div>"
			+ missingHtml
			+ body
			+ "</div>";
	}

	private String extension(String fileName) {
		if (StringUtil.isBlank(fileName)) {
			return "";
		}
		int index = fileName.lastIndexOf('.');
		if (index < 0 || index == fileName.length() - 1) {
			return "";
		}
		return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
	}

	private String escapeHtml(String value) {
		String safeValue = value == null ? "" : value;
		return safeValue
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

}
