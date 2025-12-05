package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.micks.DiscGolfApplication.connection.DiscGolfDbConnection;
import org.micks.DiscGolfApplication.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EventRegistrationService {

    @Autowired
    private DiscGolfDbConnection dbConnection;

    public void registerUserForEvent(String userId, String eventId) {
        log.info("Registered user {} for event: {}", userId, eventId);
        try (Connection connection = dbConnection.connect();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO user_event (user_id, event_id) VALUES (?, ?)")) {
            statement.setString(1, userId);
            statement.setString(2, eventId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error registering user for event", e);
        }
    }

    public void unregisterUserFromEvent(String userId, String eventId) {
        log.info("Unregistering user {} from event: {}", userId, eventId);
        try (Connection connection = dbConnection.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM user_event WHERE user_id = ? AND event_id = ?")) {
            statement.setString(1, userId);
            statement.setString(2, eventId);
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new BadRequestException("User was not registered for this event");
            }
            log.info("Successfully unregistered user {} from event {}", userId, eventId);
        } catch (SQLException e) {
            throw new RuntimeException("Error unregistering user from event", e);
        }
    }

    public List<DiscGolfEventDTO> getMyEventsWithDetails(String userId) {
        log.info("Fetching event details for user: {}", userId);
        List<DiscGolfEventDTO> events = new ArrayList<>();

        String sql = """
                    SELECT e.id, e.tournamentDate, e.registrationStart, e.registrationEnd, e.pdga, e.tournamentTitle, e.region, e.externalLink
                    FROM user_event ue 
                    JOIN events e ON ue.event_id = e.id 
                    WHERE ue.user_id = ? 
                    AND ue.active = true 
                    AND e.status != 'DELETED'
                    ORDER BY e.tournamentDate
                """;

        try (Connection connection = dbConnection.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                DiscGolfEventDTO event = new DiscGolfEventDTO(
                        resultSet.getString("id"),
                        resultSet.getDate("tournamentDate"),
                        resultSet.getDate("registrationStart"),
                        resultSet.getDate("registrationEnd"),
                        resultSet.getString("pdga"),
                        resultSet.getString("tournamentTitle"),
                        resultSet.getString("region"),
                        resultSet.getString("externalLink")
                );
                log.debug("Fetched event: {}", event);
                events.add(event);
            }
        } catch (SQLException e) {
            log.error("Error fetching event details for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error fetching event details", e);
        }
        log.info("Fetched {} events for user {}", events.size(), userId);
        return events;
    }
}
