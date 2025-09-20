package org.micks.DiscGolfApplication.events;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
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
}
