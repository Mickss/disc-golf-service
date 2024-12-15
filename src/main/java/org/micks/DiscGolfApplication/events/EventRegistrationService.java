package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.micks.DiscGolfApplication.connection.DiscGolfDbConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
}
