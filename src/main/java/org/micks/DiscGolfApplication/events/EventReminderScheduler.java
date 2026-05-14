package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.micks.DiscGolfApplication.connection.DiscGolfDbConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;

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
        LocalDateTime yesterday = now.minusHours(24);

        log.info("Checking reminders in the window: {} - {}", now, windowEnd);

        String selectSql = """
                SELECT u.email as user_email, u.user_id, e.id as event_id, e.tournamentTitle, 
                       e.registrationStart, e.email_template, e.email_subject
                FROM user_event ue
                JOIN events e ON ue.event_id = e.id
                JOIN users u ON ue.user_id = u.user_id
                WHERE ue.reminder_sent = 0 
                  AND e.reminder_datetime BETWEEN ? AND ?
                  AND ue.created_at < ?
                """;

        String updateHistorySql = "UPDATE user_event SET reminder_sent = 1 WHERE user_id = ? AND event_id = ?";

        try (Connection connection = dbConnection.connect();
             PreparedStatement selectStmt = connection.prepareStatement(selectSql);
             PreparedStatement updateStmt = connection.prepareStatement(updateHistorySql)) {

            selectStmt.setTimestamp(1, Timestamp.valueOf(now));
            selectStmt.setTimestamp(2, Timestamp.valueOf(windowEnd));
            selectStmt.setTimestamp(3, Timestamp.valueOf(yesterday));

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

                String rawSubject = (subject != null && !subject.isEmpty()) ? subject : "Reminder: [TOURNAMENT]";
                String finalSubject = rawSubject
                        .replace("[TOURNAMENT]", title)
                        .replace("[DATE]", dateStr);

                boolean isSent = tournamentEmailService.sendRawEmail(email, finalSubject, finalBody);

                if (isSent) {
                    updateStmt.setString(1, userId);
                    updateStmt.setString(2, eventId);
                    updateStmt.executeUpdate();
                    log.info("Successfully updated reminder_sent flag to 1 for user: {}", userId);
                }
            }
        } catch (SQLException e) {
            log.error("Database error in scheduler", e);
        }
    }
}
