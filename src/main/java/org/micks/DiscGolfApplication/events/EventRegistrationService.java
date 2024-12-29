package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.micks.DiscGolfApplication.connection.DiscGolfDbConnection;
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

    public List<String> getMyEvents(String userId) {
        log.info("Fetching event IDs for user: {}", userId);
        List<String> eventIds = new ArrayList<>();

        try (Connection connection = dbConnection.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT event_id FROM user_event WHERE user_id = ?")) {
            statement.setString(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String eventId = resultSet.getString("event_id");
                    log.debug("Fetched event ID: {}", eventId);
                    eventIds.add(eventId);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching event IDs for user: {}", userId, e);
            throw new RuntimeException("Error fetching user's event IDs", e);
        }

        log.info("Fetched event IDs for user {}: {}", userId, eventIds);
        return eventIds;
    }
}
