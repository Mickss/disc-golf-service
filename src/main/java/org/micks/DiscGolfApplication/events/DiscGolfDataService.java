package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.micks.DiscGolfApplication.exceptions.ImportException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class DiscGolfDataService {

    private final DiscGolfEventService discGolfEventService;

    public DiscGolfDataService(DiscGolfEventService discGolfEventService) {
        this.discGolfEventService = discGolfEventService;
    }

    public byte[] generateEventsExcel() {
        List<DiscGolfEventDTO> events = discGolfEventService.getEvents(null, null);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Events");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Tournament Date", "Registration Start", "Registration End", "PDGA", "Title", "Region", "Link"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (DiscGolfEventDTO event : events) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(event.getId());
                row.createCell(1).setCellValue(event.getTournamentDate().toString());
                row.createCell(2).setCellValue(event.getRegistrationStart().toString());
                row.createCell(3).setCellValue(event.getRegistrationEnd().toString());
                row.createCell(4).setCellValue(event.getPdga());
                row.createCell(5).setCellValue(event.getTournamentTitle());
                row.createCell(6).setCellValue(event.getRegion());
                row.createCell(7).setCellValue(event.getExternalLink());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    public void importEventsExcel(InputStream is) {
        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Excel file", e);
        }
        Sheet sheet = workbook.getSheetAt(0);

        int created = 0;
        int updated = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Date tournamentDate = getCellValueAsDate(row.getCell(1));
            Date registrationStart = getCellValueAsDate(row.getCell(2));
            Date registrationEnd = getCellValueAsDate(row.getCell(3));
            String pdga = row.getCell(4) != null ? row.getCell(4).getStringCellValue() : "";
            String tournamentTitle = row.getCell(5) != null ? row.getCell(5).getStringCellValue().trim() : "";
            String region = row.getCell(6) != null ? row.getCell(6).getStringCellValue() : "";
            String externalLink = row.getCell(7) != null ? row.getCell(7).getStringCellValue() : "";

            if (tournamentTitle.isEmpty()) {
                log.warn("Skipping row {} - missing title", i);
                continue;
            }
            if (tournamentDate == null) {
                log.warn("Skipping row {} - missing tournament date", i);
                continue;
            }

            DiscGolfEventDTO event = new DiscGolfEventDTO(
                    null,
                    tournamentDate,
                    registrationStart,
                    registrationEnd,
                    pdga,
                    tournamentTitle,
                    region,
                    externalLink
            );

            if (discGolfEventService.eventExistsByTitle(tournamentTitle)) {
                discGolfEventService.updateEventByTitle(tournamentTitle, event);
                updated++;
            } else {
                discGolfEventService.createEvent(event);
                created++;
            }
        }

        log.info("Import completed. Created: {}, Updated: {}", created, updated);
    }

    private Date getCellValueAsDate(Cell cell) {
        if (cell == null || cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            return cell.getDateCellValue();
        }

        String dateStr = cell.getStringCellValue();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            throw new ImportException("Cannot parse date: " + dateStr);
        }
    }
}
