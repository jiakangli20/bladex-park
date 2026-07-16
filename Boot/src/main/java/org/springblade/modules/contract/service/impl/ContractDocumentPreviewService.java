/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
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
@RequiredArgsConstructor
public class ContractDocumentPreviewService {

	private static final int MAX_PREVIEW_ROWS = 160;
	private static final int MAX_PREVIEW_COLUMNS = 32;
	private static final double DEFAULT_A4_WIDTH_PT = 595.3D;
	private static final double DEFAULT_A4_HEIGHT_PT = 841.9D;
	private static final double DEFAULT_PAGE_MARGIN_PT = 72D;
	private static final double DEFAULT_PARAGRAPH_HEIGHT_PT = 18D;
	private static final double DEFAULT_TABLE_ROW_HEIGHT_PT = 24D;
	private static final double WORD_PAGE_SPLIT_BUFFER_PT = 20D;
	private final OfficePdfConversionService officePdfConversionService;

	@SneakyThrows
	public String render(ContractNoticeFileVO document, Map<String, String> summary, List<String> missingFields) {
		String extension = extension(document == null ? null : document.getFileName());
		String body = renderPdf(officePdfConversionService.convert(document));
		return wrapDocument(document, summary, missingFields, body, extension);
	}

	private String renderPdf(byte[] pdfBytes) {
		String source = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfBytes);
		return "<object class=\"pdf-preview\" data-office-preview-pdf=\"true\" type=\"application/pdf\" data=\"" + source + "\">"
			+ "<p class=\"pdf-preview-fallback\">当前浏览器无法内嵌 PDF，请下载原文件查看。</p>"
			+ "</object>";
	}

	private String renderDocx(byte[] fileBytes) throws Exception {
		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
			PageBox pageBox = pageBox(document);
			double pageBodyHeight = Math.max(120D, pageBox.heightPt() - pageBox.marginTopPt() - pageBox.marginBottomPt() - WORD_PAGE_SPLIT_BUFFER_PT);
			StringBuilder html = new StringBuilder();
			html.append("<div class=\"word-page\" style=\"").append(wordPageStyle(pageBox)).append("\">");
			double currentPageHeight = 0D;
			boolean hasContent = false;
			for (IBodyElement element : document.getBodyElements()) {
				RenderedBlock block = switch (element.getElementType()) {
					case PARAGRAPH -> renderParagraphBlock((XWPFParagraph) element);
					case TABLE -> renderTableBlock((XWPFTable) element);
					default -> null;
				};
				if (block == null || StringUtil.isBlank(block.html())) {
					continue;
				}
				if (hasContent && currentPageHeight + block.heightPt() > pageBodyHeight) {
					html.append("</div><div class=\"word-page\" style=\"").append(wordPageStyle(pageBox)).append("\">");
					currentPageHeight = 0D;
					hasContent = false;
				}
				html.append(block.html());
				currentPageHeight += block.heightPt();
				hasContent = true;
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
			return "<p class=\"word-paragraph word-blank\" style=\"" + paragraphStyle(paragraph) + "\">&nbsp;</p>";
		}
		String style = paragraphStyle(paragraph);
		StringBuilder html = new StringBuilder();
		html.append("<p class=\"word-paragraph\"");
		if (StringUtil.isNotBlank(style)) {
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

	private RenderedBlock renderParagraphBlock(XWPFParagraph paragraph) {
		return new RenderedBlock(renderParagraph(paragraph), estimateParagraphHeight(paragraph));
	}

	private String paragraphStyle(XWPFParagraph paragraph) {
		if (paragraph == null) {
			return "";
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
		if (paragraph.getSpacingBefore() > 0) {
			style.append("margin-top:").append(Math.min(paragraph.getSpacingBefore() / 20, 24)).append("pt;");
		}
		if (paragraph.getIndentationLeft() > 0) {
			style.append("padding-left:").append(twipsToPt(paragraph.getIndentationLeft())).append("pt;");
		}
		if (paragraph.getIndentationRight() > 0) {
			style.append("padding-right:").append(twipsToPt(paragraph.getIndentationRight())).append("pt;");
		}
		if (paragraph.getIndentationFirstLine() > 0) {
			style.append("text-indent:").append(twipsToPt(paragraph.getIndentationFirstLine())).append("pt;");
		}
		return style.toString();
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
		if (StringUtil.isNotBlank(run.getFontFamily())) {
			style.append("font-family:'").append(escapeStyle(run.getFontFamily())).append("';");
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
		List<List<WordCell>> rows = buildWordRows(table);
		html.append("<table class=\"word-table\" style=\"").append(wordTableStyle(table)).append("\">");
		html.append(wordColGroup(table));
		html.append("<tbody>");
		for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
			XWPFTableRow row = table.getRow(rowIndex);
			String rowStyle = wordRowStyle(row);
			html.append("<tr");
			if (StringUtil.isNotBlank(rowStyle)) {
				html.append(" style=\"").append(rowStyle).append("\"");
			}
			html.append(">");
			for (WordCell wordCell : rows.get(rowIndex)) {
				if (wordCell.vMergeContinue()) {
					continue;
				}
				XWPFTableCell cell = wordCell.cell();
				int rowSpan = wordRowSpan(rows, rowIndex, wordCell.startColumn());
				html.append("<td");
				if (rowSpan > 1) {
					html.append(" rowspan=\"").append(rowSpan).append("\"");
				}
				if (wordCell.colSpan() > 1) {
					html.append(" colspan=\"").append(wordCell.colSpan()).append("\"");
				}
				String cellStyle = wordCellStyle(cell);
				if (StringUtil.isNotBlank(cellStyle)) {
					html.append(" style=\"").append(cellStyle).append("\"");
				}
				html.append(">");
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

	private RenderedBlock renderTableBlock(XWPFTable table) {
		return new RenderedBlock(renderTable(table), estimateTableHeight(table));
	}

	private String renderWorkbook(byte[] fileBytes) throws Exception {
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
			DataFormatter formatter = new DataFormatter(Locale.CHINA);
			StringBuilder html = new StringBuilder();
			html.append("<div class=\"workbook-preview\">");
			int sheetIndex = 0;
			for (Sheet sheet : workbook) {
				SheetWindow window = previewWindow(workbook, sheet, sheetIndex++, formatter);
				if (window.empty()) {
					continue;
				}
				html.append("<section class=\"sheet-page\"><h3>").append(escapeHtml(sheet.getSheetName())).append("</h3>");
				html.append("<table class=\"excel-table\"><colgroup>");
				for (int columnIndex = window.firstColumn(); columnIndex < window.lastColumnExclusive(); columnIndex++) {
					html.append("<col style=\"width:").append(Math.max(24, Math.round(sheet.getColumnWidthInPixels(columnIndex)))).append("px;\">");
				}
				html.append("</colgroup><tbody>");
				Set<String> skippedCells = new HashSet<>();
				for (int rowIndex = window.firstRow(); rowIndex <= window.lastRow(); rowIndex++) {
					Row row = sheet.getRow(rowIndex);
					String rowStyle = excelRowStyle(row);
					html.append("<tr");
					if (StringUtil.isNotBlank(rowStyle)) {
						html.append(" style=\"").append(rowStyle).append("\"");
					}
					html.append(">");
					for (int columnIndex = window.firstColumn(); columnIndex < window.lastColumnExclusive(); columnIndex++) {
						String key = rowIndex + ":" + columnIndex;
						if (skippedCells.contains(key)) {
							continue;
						}
						CellRangeAddress merged = mergedRegion(sheet, rowIndex, columnIndex);
						if (merged != null && (merged.getFirstRow() != rowIndex || merged.getFirstColumn() != columnIndex)) {
							continue;
						}
						int rowSpan = merged == null ? 1 : Math.min(merged.getLastRow(), window.lastRow()) - merged.getFirstRow() + 1;
						int colSpan = merged == null ? 1 : Math.min(merged.getLastColumn() + 1, window.lastColumnExclusive()) - merged.getFirstColumn();
						markSkipped(skippedCells, rowIndex, columnIndex, rowSpan, colSpan);
						html.append("<td");
						if (rowSpan > 1) {
							html.append(" rowspan=\"").append(rowSpan).append("\"");
						}
						if (colSpan > 1) {
							html.append(" colspan=\"").append(colSpan).append("\"");
						}
						Cell cell = row == null ? null : row.getCell(columnIndex);
						String cellStyle = excelCellStyle(workbook, cell);
						if (StringUtil.isNotBlank(cellStyle)) {
							html.append(" style=\"").append(cellStyle).append("\"");
						}
						html.append(">");
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

	private String wordPageStyle(PageBox box) {
		return "width:" + pt(box.widthPt()) + "pt;"
			+ "min-height:" + pt(box.heightPt()) + "pt;"
			+ "padding:" + pt(box.marginTopPt()) + "pt " + pt(box.marginRightPt()) + "pt "
			+ pt(box.marginBottomPt()) + "pt " + pt(box.marginLeftPt()) + "pt;";
	}

	private PageBox pageBox(XWPFDocument document) {
		CTSectPr sectPr = document == null || document.getDocument() == null || document.getDocument().getBody() == null
			? null
			: document.getDocument().getBody().getSectPr();
		CTPageSz pageSize = sectPr == null ? null : sectPr.getPgSz();
		CTPageMar pageMar = sectPr == null ? null : sectPr.getPgMar();
		double width = toPoint(pageSize == null ? null : pageSize.getW(), DEFAULT_A4_WIDTH_PT);
		double height = toPoint(pageSize == null ? null : pageSize.getH(), DEFAULT_A4_HEIGHT_PT);
		double left = toPoint(pageMar == null ? null : pageMar.getLeft(), DEFAULT_PAGE_MARGIN_PT);
		double right = toPoint(pageMar == null ? null : pageMar.getRight(), DEFAULT_PAGE_MARGIN_PT);
		double top = toPoint(pageMar == null ? null : pageMar.getTop(), DEFAULT_PAGE_MARGIN_PT);
		double bottom = toPoint(pageMar == null ? null : pageMar.getBottom(), DEFAULT_PAGE_MARGIN_PT);
		return new PageBox(width, height, top, right, bottom, left);
	}

	private List<List<WordCell>> buildWordRows(XWPFTable table) {
		List<List<WordCell>> rows = new ArrayList<>();
		if (table == null || table.getRows() == null) {
			return rows;
		}
		for (XWPFTableRow row : table.getRows()) {
			List<WordCell> cells = new ArrayList<>();
			int column = 0;
			for (XWPFTableCell cell : row.getTableCells()) {
				int colSpan = gridSpan(cell);
				cells.add(new WordCell(cell, column, colSpan, isVerticalMergeContinue(cell)));
				column += colSpan;
			}
			rows.add(cells);
		}
		return rows;
	}

	private int wordRowSpan(List<List<WordCell>> rows, int rowIndex, int startColumn) {
		if (rows == null || rowIndex < 0 || rowIndex >= rows.size()) {
			return 1;
		}
		int span = 1;
		for (int nextRowIndex = rowIndex + 1; nextRowIndex < rows.size(); nextRowIndex++) {
			WordCell cell = findWordCell(rows.get(nextRowIndex), startColumn);
			if (cell == null || !cell.vMergeContinue()) {
				break;
			}
			span++;
		}
		return span;
	}

	private WordCell findWordCell(List<WordCell> cells, int startColumn) {
		if (cells == null) {
			return null;
		}
		for (WordCell cell : cells) {
			if (cell.startColumn() == startColumn) {
				return cell;
			}
		}
		return null;
	}

	private int gridSpan(XWPFTableCell cell) {
		CTTcPr tcPr = cell == null || cell.getCTTc() == null ? null : cell.getCTTc().getTcPr();
		if (tcPr == null || !tcPr.isSetGridSpan() || tcPr.getGridSpan() == null) {
			return 1;
		}
		BigInteger value = toBigInteger(tcPr.getGridSpan().getVal());
		return value == null ? 1 : Math.max(1, value.intValue());
	}

	private boolean isVerticalMergeContinue(XWPFTableCell cell) {
		CTTcPr tcPr = cell == null || cell.getCTTc() == null ? null : cell.getCTTc().getTcPr();
		if (tcPr == null || tcPr.getVMerge() == null) {
			return false;
		}
		Object value = tcPr.getVMerge().getVal();
		return value == null || STMerge.CONTINUE.equals(value);
	}

	private String wordColGroup(XWPFTable table) {
		if (table == null || table.getCTTbl() == null || table.getCTTbl().getTblGrid() == null) {
			return "";
		}
		List<CTTblGridCol> gridCols = table.getCTTbl().getTblGrid().getGridColList();
		if (gridCols == null || gridCols.isEmpty()) {
			return "";
		}
		StringBuilder html = new StringBuilder("<colgroup>");
		for (CTTblGridCol col : gridCols) {
			BigInteger width = toBigInteger(col.getW());
			if (width == null || width.signum() <= 0) {
				html.append("<col>");
			} else {
				html.append("<col style=\"width:").append(pt(twipsToPt(width.intValue()))).append("pt;\">");
			}
		}
		html.append("</colgroup>");
		return html.toString();
	}

	private String wordTableStyle(XWPFTable table) {
		if (table == null || table.getCTTbl() == null || table.getCTTbl().getTblPr() == null) {
			return "";
		}
		CTTblWidth width = table.getCTTbl().getTblPr().getTblW();
		if (width == null || width.getW() == null || width.getType() == null) {
			return "";
		}
		if (STTblWidth.DXA.equals(width.getType())) {
			return "width:" + pt(twipsToPt(toBigInteger(width.getW()).intValue())) + "pt;";
		}
		if (STTblWidth.PCT.equals(width.getType())) {
			BigInteger pct = toBigInteger(width.getW());
			if (pct != null && pct.signum() > 0) {
				return "width:" + pt(pct.doubleValue() / 50D) + "%;";
			}
		}
		return "";
	}

	private String wordRowStyle(XWPFTableRow row) {
		if (row == null || row.getHeight() <= 0) {
			return "";
		}
		return "height:" + pt(twipsToPt(row.getHeight())) + "pt;";
	}

	private String wordCellStyle(XWPFTableCell cell) {
		StringBuilder style = new StringBuilder();
		if (cell == null) {
			return "";
		}
		CTTcPr tcPr = cell.getCTTc() == null ? null : cell.getCTTc().getTcPr();
		CTTblWidth width = tcPr == null ? null : tcPr.getTcW();
		if (width != null && width.getW() != null && STTblWidth.DXA.equals(width.getType())) {
			BigInteger widthValue = toBigInteger(width.getW());
			if (widthValue != null && widthValue.signum() > 0) {
				style.append("width:").append(pt(twipsToPt(widthValue.intValue()))).append("pt;");
			}
		}
		String cellColor = wordCellColor(cell);
		if (StringUtil.isNotBlank(cellColor)) {
			style.append("background:#").append(cellColor).append(";");
		}
		XWPFTableCell.XWPFVertAlign vertical = cell.getVerticalAlignment();
		if (vertical == XWPFTableCell.XWPFVertAlign.TOP) {
			style.append("vertical-align:top;");
		} else if (vertical == XWPFTableCell.XWPFVertAlign.BOTTOM) {
			style.append("vertical-align:bottom;");
		} else {
			style.append("vertical-align:middle;");
		}
		return style.toString();
	}

	private String wordCellColor(XWPFTableCell cell) {
		try {
			CTTcPr tcPr = cell == null || cell.getCTTc() == null ? null : cell.getCTTc().getTcPr();
			if (tcPr == null || tcPr.getShd() == null || tcPr.getShd().xgetFill() == null) {
				return "";
			}
			String fill = tcPr.getShd().xgetFill().getStringValue();
			return StringUtil.isBlank(fill) || "auto".equalsIgnoreCase(fill) ? "" : fill;
		} catch (Exception ignored) {
			return "";
		}
	}

	private double estimateParagraphHeight(XWPFParagraph paragraph) {
		if (paragraph == null) {
			return DEFAULT_PARAGRAPH_HEIGHT_PT;
		}
		String text = paragraph.getText();
		int fontSize = paragraph.getRuns() == null || paragraph.getRuns().isEmpty()
			? 11
			: paragraph.getRuns().stream()
			.map(XWPFRun::getFontSize)
			.filter(size -> size > 0)
			.findFirst()
			.orElse(11);
		double lineHeight = Math.max(14D, fontSize * 1.55D);
		int textLength = StringUtil.isBlank(text) ? 1 : text.trim().length();
		int estimatedLines = Math.max(1, (int) Math.ceil(textLength / 54D));
		double spacingBefore = paragraph.getSpacingBefore() > 0 ? twipsToPt(paragraph.getSpacingBefore()) : 0D;
		double spacingAfter = paragraph.getSpacingAfter() > 0 ? twipsToPt(paragraph.getSpacingAfter()) : 8D;
		return Math.min(220D, estimatedLines * lineHeight + spacingBefore + spacingAfter);
	}

	private double estimateTableHeight(XWPFTable table) {
		if (table == null || table.getRows() == null || table.getRows().isEmpty()) {
			return DEFAULT_TABLE_ROW_HEIGHT_PT;
		}
		double height = 12D;
		for (XWPFTableRow row : table.getRows()) {
			if (row.getHeight() > 0) {
				height += twipsToPt(row.getHeight());
			} else {
				height += DEFAULT_TABLE_ROW_HEIGHT_PT;
			}
		}
		return Math.min(900D, height + 12D);
	}

	private SheetWindow previewWindow(Workbook workbook, Sheet sheet, int sheetIndex, DataFormatter formatter) {
		CellRangeAddress printArea = printArea(workbook, sheetIndex);
		if (printArea != null) {
			int firstColumn = Math.max(0, printArea.getFirstColumn());
			int lastColumnExclusive = Math.min(printArea.getLastColumn() + 1, firstColumn + MAX_PREVIEW_COLUMNS);
			int firstRow = Math.max(0, printArea.getFirstRow());
			int lastRow = Math.min(printArea.getLastRow(), firstRow + MAX_PREVIEW_ROWS);
			return new SheetWindow(firstRow, lastRow, firstColumn, lastColumnExclusive, lastRow < firstRow || lastColumnExclusive <= firstColumn);
		}
		return detectedSheetWindow(sheet, formatter);
	}

	private CellRangeAddress printArea(Workbook workbook, int sheetIndex) {
		if (workbook == null || sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
			return null;
		}
		String printArea = workbook.getPrintArea(sheetIndex);
		if (StringUtil.isBlank(printArea)) {
			return null;
		}
		String area = printArea;
		int separator = area.lastIndexOf('!');
		if (separator >= 0) {
			area = area.substring(separator + 1);
		}
		if (area.contains(",")) {
			area = area.substring(0, area.indexOf(','));
		}
		area = area.replace("$", "").replace("'", "").trim();
		try {
			return CellRangeAddress.valueOf(area);
		} catch (Exception ignored) {
			return null;
		}
	}

	private SheetWindow detectedSheetWindow(Sheet sheet, DataFormatter formatter) {
		if (sheet == null || sheet.getLastRowNum() < 0) {
			return SheetWindow.emptyWindow();
		}
		int firstRow = Integer.MAX_VALUE;
		int lastRow = -1;
		int firstColumn = Integer.MAX_VALUE;
		int lastColumn = -1;
		int maxRow = Math.min(sheet.getLastRowNum(), MAX_PREVIEW_ROWS);
		for (int rowIndex = 0; rowIndex <= maxRow; rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null || row.getLastCellNum() < 0) {
				continue;
			}
			for (int columnIndex = 0; columnIndex < Math.min(row.getLastCellNum(), MAX_PREVIEW_COLUMNS); columnIndex++) {
				Cell cell = row.getCell(columnIndex);
				String value = cell == null ? "" : formatter.formatCellValue(cell);
				if (!isMeaningfulExcelValue(value)) {
					continue;
				}
				firstRow = Math.min(firstRow, rowIndex);
				lastRow = Math.max(lastRow, rowIndex);
				firstColumn = Math.min(firstColumn, columnIndex);
				lastColumn = Math.max(lastColumn, columnIndex);
			}
		}
		for (CellRangeAddress region : sheet.getMergedRegions()) {
			if (region.getFirstRow() > maxRow) {
				continue;
			}
			firstRow = Math.min(firstRow, region.getFirstRow());
			lastRow = Math.max(lastRow, Math.min(region.getLastRow(), maxRow));
			firstColumn = Math.min(firstColumn, region.getFirstColumn());
			lastColumn = Math.max(lastColumn, Math.min(region.getLastColumn(), MAX_PREVIEW_COLUMNS - 1));
		}
		if (lastRow < 0 || lastColumn < 0) {
			return SheetWindow.emptyWindow();
		}
		return new SheetWindow(
			Math.max(0, firstRow),
			Math.min(lastRow, firstRow + MAX_PREVIEW_ROWS),
			Math.max(0, firstColumn),
			Math.min(lastColumn + 1, firstColumn + MAX_PREVIEW_COLUMNS),
			false
		);
	}

	private boolean isMeaningfulExcelValue(String value) {
		if (StringUtil.isBlank(value)) {
			return false;
		}
		String normalized = value.trim();
		return !".".equals(normalized) && !"c".equalsIgnoreCase(normalized);
	}

	private String excelRowStyle(Row row) {
		if (row == null || row.getHeightInPoints() <= 0) {
			return "";
		}
		return "height:" + pt(row.getHeightInPoints()) + "pt;";
	}

	private String excelCellStyle(Workbook workbook, Cell cell) {
		if (cell == null || workbook == null) {
			return "";
		}
		CellStyle cellStyle = cell.getCellStyle();
		if (cellStyle == null) {
			return "";
		}
		StringBuilder style = new StringBuilder();
		HorizontalAlignment horizontal = cellStyle.getAlignment();
		if (horizontal == HorizontalAlignment.CENTER || horizontal == HorizontalAlignment.CENTER_SELECTION) {
			style.append("text-align:center;");
		} else if (horizontal == HorizontalAlignment.RIGHT) {
			style.append("text-align:right;");
		} else if (horizontal == HorizontalAlignment.JUSTIFY || horizontal == HorizontalAlignment.DISTRIBUTED) {
			style.append("text-align:justify;");
		}
		VerticalAlignment vertical = cellStyle.getVerticalAlignment();
		if (vertical == VerticalAlignment.TOP) {
			style.append("vertical-align:top;");
		} else if (vertical == VerticalAlignment.BOTTOM) {
			style.append("vertical-align:bottom;");
		} else {
			style.append("vertical-align:middle;");
		}
		if (cellStyle.getWrapText()) {
			style.append("white-space:pre-wrap;");
		}
		Font font = workbook.getFontAt(cellStyle.getFontIndexAsInt());
		if (font != null) {
			if (font.getBold()) {
				style.append("font-weight:700;");
			}
			if (font.getItalic()) {
				style.append("font-style:italic;");
			}
			if (font.getFontHeightInPoints() > 0) {
				style.append("font-size:").append(font.getFontHeightInPoints()).append("pt;");
			}
			if (StringUtil.isNotBlank(font.getFontName())) {
				style.append("font-family:'").append(escapeStyle(font.getFontName())).append("';");
			}
		}
		return style.toString();
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
			+ ".pdf-preview{display:block;box-sizing:border-box;width:min(1100px,100%);height:calc(100vh - 190px);min-height:720px;margin:0 auto;border:1px solid #d8dde6;background:#fff;box-shadow:0 10px 30px rgba(15,23,42,.12);}.pdf-preview-fallback{padding:24px;text-align:center;color:#6b7280;}"
			+ ".word-page,.sheet-page{box-sizing:border-box;margin:0 auto 18px;background:#fff;border:1px solid #d8dde6;box-shadow:0 10px 30px rgba(15,23,42,.12);}"
			+ ".word-page{overflow:visible;}.sheet-page{overflow:auto;}"
			+ ".word-paragraph{margin:0 0 8px;line-height:1.55;font-size:10.5pt;white-space:pre-wrap;}.word-blank{min-height:12pt;}"
			+ ".word-table,.excel-table{border-collapse:collapse;margin:8px 0 12px;table-layout:fixed;font-size:10.5pt;}.word-table td,.excel-table td{box-sizing:border-box;border:1px solid #3f3f46;padding:4px 6px;vertical-align:middle;word-break:break-word;line-height:1.45;}"
			+ ".word-table .word-paragraph{margin:0;line-height:1.45;}.workbook-preview{max-width:100%;overflow:auto;}.sheet-page{min-height:0;max-width:max-content;min-width:860px;padding:28px;overflow:auto;}"
			+ ".sheet-page h3{margin:0 0 14px;text-align:left;font-size:16px;}.excel-table{width:auto;min-width:820px;}.excel-table td{white-space:pre-wrap;background:#fff;}"
			+ "@media (max-width:900px){.contract-document-preview{padding:12px}.word-page,.sheet-page{max-width:none;}.preview-toolbar{flex-direction:column;align-items:flex-start;}}"
			+ "</style>"
			+ "<div class=\"preview-toolbar\"><strong>" + escapeHtml(document == null ? "文书预览" : document.getNoticeName()) + "</strong><code>"
			+ escapeHtml(extension == null ? "" : extension.toUpperCase(Locale.ROOT)) + " 转 PDF 原版预览</code></div>"
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

	private String escapeStyle(String value) {
		return value == null ? "" : value.replace("'", "\\'");
	}

	private double toPoint(Object twips, double fallback) {
		BigInteger value = toBigInteger(twips);
		return value == null || value.signum() <= 0 ? fallback : twipsToPt(value.intValue());
	}

	private double twipsToPt(int twips) {
		return twips / 20D;
	}

	private BigInteger toBigInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigInteger bigInteger) {
			return bigInteger;
		}
		if (value instanceof Number number) {
			return BigInteger.valueOf(number.longValue());
		}
		try {
			return new BigInteger(value.toString());
		} catch (Exception ignored) {
			return null;
		}
	}

	private String pt(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private record PageBox(double widthPt, double heightPt, double marginTopPt, double marginRightPt,
						   double marginBottomPt, double marginLeftPt) {
	}

	private record RenderedBlock(String html, double heightPt) {
	}

	private record WordCell(XWPFTableCell cell, int startColumn, int colSpan, boolean vMergeContinue) {
	}

	private record SheetWindow(int firstRow, int lastRow, int firstColumn, int lastColumnExclusive, boolean empty) {
		private static SheetWindow emptyWindow() {
			return new SheetWindow(0, -1, 0, 0, true);
		}
	}

}
