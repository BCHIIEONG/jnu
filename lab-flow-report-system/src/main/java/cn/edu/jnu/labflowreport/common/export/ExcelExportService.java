package cn.edu.jnu.labflowreport.common.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public byte[] writeWorkbook(List<SheetSpec> sheets) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle bodyStyle = buildBodyStyle(workbook);
            Set<String> usedNames = new LinkedHashSet<>();
            for (SheetSpec spec : sheets) {
                String sheetName = uniqueSheetName(usedNames, spec.name());
                XSSFSheet sheet = workbook.createSheet(sheetName);
                int rowIndex = 0;
                if (spec.headers() != null && !spec.headers().isEmpty()) {
                    Row headerRow = sheet.createRow(rowIndex++);
                    for (int i = 0; i < spec.headers().size(); i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(Objects.toString(spec.headers().get(i), ""));
                        cell.setCellStyle(headerStyle);
                    }
                }
                List<Integer> widths = new ArrayList<>();
                for (List<?> rowData : spec.rows()) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < rowData.size(); i++) {
                        Cell cell = row.createCell(i);
                        String text = formatCellValue(rowData.get(i));
                        cell.setCellValue(text);
                        cell.setCellStyle(bodyStyle);
                        ensureWidth(widths, i, text);
                    }
                }
                for (int i = 0; i < (spec.headers() == null ? 0 : spec.headers().size()); i++) {
                    ensureWidth(widths, i, spec.headers().get(i));
                }
                for (int i = 0; i < widths.size(); i++) {
                    int width = Math.min(Math.max(widths.get(i) + 2, 12), 60);
                    sheet.setColumnWidth(i, width * 256);
                }
                if (sheet.getPhysicalNumberOfRows() > 0) {
                    sheet.createFreezePane(0, spec.headers() == null || spec.headers().isEmpty() ? 0 : 1);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成 Excel 失败", e);
        }
    }

    private CellStyle buildHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle buildBodyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private String uniqueSheetName(Set<String> usedNames, String name) {
        String base = sanitizeSheetName(name);
        String candidate = base;
        int index = 2;
        while (!usedNames.add(candidate)) {
            String suffix = "-" + index++;
            int maxBaseLength = Math.max(1, 31 - suffix.length());
            candidate = base.substring(0, Math.min(base.length(), maxBaseLength)) + suffix;
        }
        return candidate;
    }

    private String sanitizeSheetName(String name) {
        String value = name == null || name.isBlank() ? "Sheet" : name.trim();
        value = value.replaceAll("[\\\\/:*?\\[\\]]", "_");
        if (value.length() > 31) {
            value = value.substring(0, 31);
        }
        return value;
    }

    private String formatCellValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime localDateTime) {
            return DATE_TIME_FORMATTER.format(localDateTime);
        }
        if (value instanceof LocalDate localDate) {
            return DATE_FORMATTER.format(localDate);
        }
        if (value instanceof LocalTime localTime) {
            return TIME_FORMATTER.format(localTime);
        }
        return String.valueOf(value);
    }

    private void ensureWidth(List<Integer> widths, int index, Object value) {
        while (widths.size() <= index) {
            widths.add(0);
        }
        String text = formatCellValue(value);
        int current = widths.get(index);
        widths.set(index, Math.max(current, text == null ? 0 : text.length()));
    }

    public record SheetSpec(String name, List<String> headers, List<? extends List<?>> rows) {
    }
}
