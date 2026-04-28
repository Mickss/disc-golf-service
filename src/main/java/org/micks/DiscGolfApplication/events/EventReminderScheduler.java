package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.micks.DiscGolfApplication.connection.DiscGolfDbConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class EventReminderScheduler {

    @Autowired
    private DiscGolfDbConnection dbConnection;

    @Autowired
    private TournamentEmailService tournamentEmailService;

    @Scheduled(fixedRate = 60000)
    public void processReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(10);

        log.info("Checking reminders in the window: {} - {}", now, windowEnd);

        String selectSql = """
                SELECT u.email as user_email, u.user_id, e.id as event_id, e.tournamentTitle, 
                       e.registrationStart, e.email_template, e.email_subject
                FROM user_event ue
                JOIN events e ON ue.event_id = e.id
                JOIN users u ON ue.user_id = u.user_id
                LEFT JOIN sent_notifications sn ON (sn.user_id = u.user_id AND sn.event_id = e.id)
                WHERE sn.id IS NULL 
                  AND e.reminder_datetime BETWEEN ? AND ?
                """;

        String insertHistorySql = "INSERT INTO sent_notifications (id, user_id, event_id) VALUES (?, ?, ?)";

        try (Connection connection = dbConnection.connect();
             PreparedStatement selectStmt = connection.prepareStatement(selectSql);
             PreparedStatement historyStmt = connection.prepareStatement(insertHistorySql)) {

            selectStmt.setTimestamp(1, Timestamp.valueOf(now));
            selectStmt.setTimestamp(2, Timestamp.valueOf(windowEnd));
            ResultSet rs = selectStmt.executeQuery();

            while (rs.next()) {
                String email = rs.getString("user_email");
                String userId = rs.getString("user_id");
                String eventId = rs.getString("event_id");
                String title = rs.getString("tournamentTitle");
                String rawTemplate = rs.getString("email_template");
                String subject = rs.getString("email_subject");

                Timestamp regStart = rs.getTimestamp("registrationStart");
                String dateStr = regStart != null ? new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(regStart) : "TBD";

                if (rawTemplate == null || rawTemplate.isEmpty()) continue;

                String finalBody = rawTemplate
                        .replace("[TOURNAMENT]", title)
                        .replace("[DATE]", dateStr)
                        .replace("[LINK]", "https://app.disc-golf.pl/events/" + eventId);

                String finalSubject = (subject != null && !subject.isEmpty()) ? subject : "Reminder: " + title;

                boolean isSent = tournamentEmailService.sendRawEmail(email, finalSubject, finalBody);

                if (isSent) {
                    historyStmt.setString(1, UUID.randomUUID().toString());
                    historyStmt.setString(2, userId);
                    historyStmt.setString(3, eventId);
                    historyStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            log.error("Database error in scheduler", e);
        }
    }
}
