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
            String[] headers = {"ID", "Tournament Date", "PDGA", "Title", "Region", "Registration", "Vacancies"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (DiscGolfEventDTO event : events) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(event.getId());
                row.createCell(1).setCellValue(event.getTournamentDate().toString());
                row.createCell(2).setCellValue(event.getPdga());
                row.createCell(3).setCellValue(event.getTournamentTitle());
                row.createCell(4).setCellValue(event.getRegion());
                row.createCell(5).setCellValue(event.getRegistration());
                row.createCell(6).setCellValue(event.getVacancies());
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
            String pdga = row.getCell(2).getStringCellValue();
            String tournamentTitle = row.getCell(3).getStringCellValue().trim();
            String region = row.getCell(4).getStringCellValue();
            String registration = row.getCell(5).getStringCellValue();
            String vacancies = row.getCell(6).getStringCellValue();

            if (tournamentTitle.isEmpty() || tournamentDate == null) {
                log.warn("Skipping row {} - missing title or date", i);
                continue;
            }

            DiscGolfEventDTO event = new DiscGolfEventDTO(
                    null,
                    tournamentDate,
                    pdga,
                    tournamentTitle,
                    region,
                    registration,
                    vacancies
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
        String dateStr = cell.getStringCellValue();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            throw new ImportException("Cannot parse date: " + dateStr);
        }
    }
}
