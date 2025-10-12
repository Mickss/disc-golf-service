package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.micks.DiscGolfApplication.connection.DiscGolfDbConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class DiscGolfDataService {

    private final DiscGolfEventService discGolfEventService;

    @Autowired
    private DiscGolfDbConnection dbConnection;

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
    public void importEventsExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            int created = 0;
            int updated = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Date tournamentDate = getCellValueAsDate(row.getCell(1));
                String pdga = getCellValueAsString(row.getCell(2));
                String tournamentTitle = getCellValueAsString(row.getCell(3)).trim();
                String region = getCellValueAsString(row.getCell(4));
                String registration = getCellValueAsString(row.getCell(5));
                String vacancies = getCellValueAsString(row.getCell(6));

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

                if (eventExistsByTitle(tournamentTitle)) {
                    updateEventByTitle(tournamentTitle, event);
                    updated++;
                } else {
                    discGolfEventService.createEvent(event);
                    created++;
                }
            }

            log.info("Import completed. Created: {}, Updated: {}", created, updated);

        } catch (IOException e) {
            throw new RuntimeException("Failed to import Excel file", e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Date getCellValueAsDate(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String dateStr = cell.getStringCellValue();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(dateStr);
            }
        } catch (Exception e) {
            log.error("Failed to parse date from cell", e);
        }
        return null;
    }
    public boolean eventExistsByTitle(String tournamentTitle) {
        try (Connection connection = dbConnection.connect()) {
            String query = "SELECT COUNT(*) FROM Events WHERE tournamentTitle = ? AND status != 'DELETED'";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, tournamentTitle);

            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error checking if event exists: {}", tournamentTitle, e);
            return false;
        }
    }

    private void updateEventByTitle(String tournamentTitle, DiscGolfEventDTO event) {
        try (Connection connection = dbConnection.connect()) {
            String query = "UPDATE Events SET " +
                    "tournamentDate = ?, " +
                    "pdga = ?, " +
                    "region = ?, " +
                    "registration = ?, " +
                    "vacancies = ? " +
                    "WHERE tournamentTitle = ? AND status != 'DELETED'";

            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setDate(1, new java.sql.Date(event.getTournamentDate().getTime()));
            stmt.setString(2, event.getPdga());
            stmt.setString(3, event.getRegion());
            stmt.setString(4, event.getRegistration());
            stmt.setString(5, event.getVacancies());
            stmt.setString(6, tournamentTitle);

            stmt.executeUpdate();
            log.info("Updated event: {}", tournamentTitle);

        } catch (SQLException e) {
            log.error("Error updating event: {}", tournamentTitle, e);
            throw new RuntimeException("Failed to update event", e);
        }
    }
}
