package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.micks.DiscGolfApplication.exceptions.ImportException;
import org.springframework.stereotype.Service;

import static org.apache.poi.ss.usermodel.CellType.*;

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
            String[] headers = {"ID", "Tournament Start", "Tournament End", "Registration Start", "Registration End", "PDGA", "Title", "Region", "Link", "Director", "Capacity"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (DiscGolfEventDTO event : events) {
                Row row = sheet.createRow(rowIdx++);
                String linksForExcel = convertSemicolonToNewline(event.getExternalLink());

                row.createCell(0).setCellValue(event.getId());
                row.createCell(1).setCellValue(event.getTournamentDateStart() != null ? event.getTournamentDateStart().toString() : "");
                row.createCell(2).setCellValue(event.getTournamentDateEnd() != null ? event.getTournamentDateEnd().toString() : "");
                row.createCell(3).setCellValue(event.getRegistrationStart() != null ? event.getRegistrationStart().toString() : "");
                row.createCell(4).setCellValue(event.getRegistrationEnd() != null ? event.getRegistrationEnd().toString() : "");
                row.createCell(5).setCellValue(event.getPdga());
                row.createCell(6).setCellValue(event.getTournamentTitle());
                row.createCell(7).setCellValue(event.getRegion());

                Cell linkCell = row.createCell(8);
                linkCell.setCellValue(linksForExcel);
                CellStyle style = workbook.createCellStyle();
                style.setWrapText(true);
                linkCell.setCellStyle(style);

                row.createCell(9).setCellValue(event.getTournamentDirector() != null ? event.getTournamentDirector() : "");
                row.createCell(10).setCellValue(event.getCapacity() != null ? event.getCapacity().toString() : "");
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

    private String convertSemicolonToNewline(String externalLink) {
        if (externalLink == null || externalLink.isEmpty()) {
            return "";
        }
        return externalLink.replace(";", "\n");
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

            Date tournamentDateStart = getCellValueAsDate(row.getCell(1));
            Date tournamentDateEnd = getCellValueAsDate(row.getCell(2));
            Date registrationStart = getCellValueAsDate(row.getCell(3));
            Date registrationEnd = getCellValueAsDate(row.getCell(4));
            String pdga = row.getCell(5) != null ? row.getCell(5).getStringCellValue() : "";
            String tournamentTitle = row.getCell(6) != null ? row.getCell(6).getStringCellValue().trim() : "";
            String region = row.getCell(7) != null ? row.getCell(7).getStringCellValue() : "";
            String externalLinkRaw = row.getCell(8) != null ? row.getCell(8).getStringCellValue() : "";
            String externalLink = convertNewlineToSemicolon(externalLinkRaw);
            String tournamentDirector = row.getCell(9) != null ? row.getCell(9).getStringCellValue() : "";
            Integer capacity = null;
            if (row.getCell(10) != null) {
                try {
                    capacity = (int) row.getCell(10).getNumericCellValue();
                } catch (Exception e) {
                    log.warn("Invalid capacity value at row {}: {}", i, e.getMessage());
                }
            }

            if (tournamentTitle.isEmpty()) {
                log.warn("Skipping row {} - missing title", i);
                continue;
            }
            if (tournamentDateStart == null) {
                log.warn("Skipping row {} - missing tournament start date", i);
                continue;
            }

            DiscGolfEventDTO event = new DiscGolfEventDTO(
                    null,
                    tournamentDateStart,
                    tournamentDateEnd,
                    registrationStart,
                    registrationEnd,
                    pdga,
                    tournamentTitle,
                    region,
                    externalLink,
                    tournamentDirector,
                    capacity
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

    private String convertNewlineToSemicolon(String externalLink) {
        if (externalLink == null || externalLink.isEmpty()) {
            return "";
        }
        return externalLink.replace("\n", ";");
    }

    private Date getCellValueAsDate(Cell cell) {
        if (cell == null || cell.getCellType() == BLANK) {
            return null;
        }

        if (cell.getCellType() == NUMERIC) {
            return cell.getDateCellValue();
        }

        String dateStr = cell.getStringCellValue();
        if (dateStr.isEmpty()) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            throw new ImportException("Cannot parse date: " + dateStr);
        }
    }
}
