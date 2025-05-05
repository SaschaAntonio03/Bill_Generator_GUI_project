import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class ExcelExporter {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void export(File template,
                              File out,
                              String clientName,
                              LocalDateTime billDate,
                              Database db) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(template)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter df = new DataFormatter();

            // 1) locate the DETAIL row:
            int detailRow = -1;
            for (Row row : sheet) {
                for (Cell cell : row) {
                    String text = df.formatCellValue(cell).trim();
                    // debug print every cell if you like:
                    System.out.printf("R%d C%d → '%s'%n", row.getRowNum(), cell.getColumnIndex(), text);
                    if ("DÉTAILS".equalsIgnoreCase(text)) {
                        detailRow = row.getRowNum();
                        break;
                    }
                }
                if (detailRow >= 0) break;
            }
            if (detailRow < 0) {
                throw new IllegalStateException("Could not find the DÉTAILS row in the template");
            }

            // 2) locate "FACTURER À" cell to write client name below
            int factCol = -1, factRow = -1;
            for (Row r : sheet) {
                for (Cell c : r) {
                    if (c.getCellType() == CellType.STRING
                            && "FACTURER À".equalsIgnoreCase(c.getStringCellValue())) {
                        factCol = c.getColumnIndex();
                        factRow = r.getRowNum();
                        break;
                    }
                }
                if (factCol >= 0) break;
            }
            if (factCol < 0) throw new IllegalStateException("'FACTURER À' not found");

            // write client name one row below
            Row below = sheet.getRow(factRow + 1);
            if (below == null) below = sheet.createRow(factRow + 1);
            Cell nameCell = below.getCell(factCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            nameCell.setCellValue(clientName);

            // 3) fetch the bill
            Client cObj = db.getClients().get(clientName);
            if (cObj == null) throw new IllegalArgumentException("No such client");
            Bill bill = cObj.getBills().get(billDate);
            if (bill == null) throw new IllegalArgumentException("No such bill");

            // 4) use row after DETAIL as template
            Row templateRow = sheet.getRow(detailRow + 1);
            int insertAt   = detailRow + 1;

            for (Product p : bill.getProducts().values()) {
                sheet.shiftRows(insertAt, sheet.getLastRowNum(), 1, true, false);
                Row newRow = sheet.createRow(insertAt);
                newRow.setHeight(templateRow.getHeight());
                // copy styles
                for (int ci = 0; ci < templateRow.getLastCellNum(); ci++) {
                    Cell tc = templateRow.getCell(ci);
                    Cell nc = newRow.createCell(ci);
                    if (tc != null) {
                        nc.setCellStyle(tc.getCellStyle());
                    }
                }
                // populate product columns: assume col 0=name, col1=price
                newRow.getCell(0).setCellValue(p.getName());
                newRow.getCell(1).setCellValue(p.getPrice());
                insertAt++;
            }

            // 5) write out
            try (FileOutputStream fos = new FileOutputStream(out)) {
                wb.write(fos);
            }
        }
    }
}
