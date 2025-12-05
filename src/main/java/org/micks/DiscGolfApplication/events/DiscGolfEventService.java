package org.micks.DiscGolfApplication.events;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.micks.DiscGolfApplication.connection.DiscGolfDbConnection;
import org.micks.DiscGolfApplication.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class DiscGolfEventService {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private String safeFormat(java.util.Date date) {
        return date != null ? DATE_FORMAT.format(date) : null;
    }

    @Autowired
    private DiscGolfDbConnection dbConnection;
    private List<String> allowedColumnNames;

    @PostConstruct
    private void init() {
        Field[] declaredFields = DiscGolfEventDTO.class.getDeclaredFields();
        allowedColumnNames = Arrays.stream(declaredFields).map(Field::getName).toList();
    }

    public List<DiscGolfEventDTO> getEvents(String valueToOrderBy, OrderDirection orderDirection) {
        try (Connection connection = dbConnection.connect()) {
            String query = "SELECT * FROM events WHERE status != 'DELETED'";
            if (valueToOrderBy != null) {
                if (!allowedColumnNames.contains(valueToOrderBy)) {
                    throw new BadRequestException("Incorrect order column name: " + valueToOrderBy);
                }
                query += " ORDER BY " + valueToOrderBy;
                if (orderDirection != null) {
                    query += " " + orderDirection;
                }
            }
            log.info("Executing query: {}", query);
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            List<DiscGolfEventDTO> discGolfEventDTOList = new ArrayList<>();
            while (resultSet.next()) {
                DiscGolfEventDTO discGolfEventDTO = new DiscGolfEventDTO(
                        resultSet.getString("id"),
                        resultSet.getDate("tournamentDate"),
                        resultSet.getDate("registrationStart"),
                        resultSet.getDate("registrationEnd"),
                        resultSet.getString("pdga"),
                        resultSet.getString("tournamentTitle"),
                        resultSet.getString("region"),
                        resultSet.getString("externalLink")
                );
                discGolfEventDTOList.add(discGolfEventDTO);
            }
            return discGolfEventDTOList;
        } catch (SQLException e) {
            throw new IllegalStateException("Error while getting events from database", e);
        }
    }

    public void createEvent(DiscGolfEventDTO discGolfEventDTO) {
        log.info("Creating new event: {}", discGolfEventDTO.getTournamentTitle());
        try (Connection connection = dbConnection.connect()) {
            PreparedStatement statement = connection.prepareStatement("insert into events values(UUID(),?,?,?,?,?,?,?, 'ACTIVE')");
            statement.setString(1, safeFormat(discGolfEventDTO.getTournamentDate()));
            statement.setString(2, safeFormat(discGolfEventDTO.getRegistrationStart()));
            statement.setString(3, safeFormat(discGolfEventDTO.getRegistrationEnd()));
            statement.setString(4, discGolfEventDTO.getPdga());
            statement.setString(5, discGolfEventDTO.getTournamentTitle());
            statement.setString(6, discGolfEventDTO.getRegion());
            statement.setString(7, discGolfEventDTO.getExternalLink());
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error while creating event with title: " + discGolfEventDTO.getTournamentTitle(), e);
        }
    }

    public DiscGolfEventDTO getEvent(String eventId) {
        try (Connection connection = dbConnection.connect()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM events WHERE ID = ? AND status != 'DELETED'");
            statement.setString(1, eventId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                DiscGolfEventDTO discGolfEventDTO = new DiscGolfEventDTO(
                        resultSet.getString("id"),
                        resultSet.getDate("tournamentDate"),
                        resultSet.getDate("registrationStart"),
                        resultSet.getDate("registrationEnd"),
                        resultSet.getString("pdga"),
                        resultSet.getString("tournamentTitle"),
                        resultSet.getString("region"),
                        resultSet.getString("externalLink")
                );
                statement.close();
                return discGolfEventDTO;
            } else {
                throw new IllegalStateException("Could not find event for id: " + eventId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while getting event with id: " + eventId, e);
        }
    }

    public void editEvents(String eventId, DiscGolfEventDTO discGolfEventDTO) {
        log.info("Editing event with id: {}", eventId);
        try (Connection connection = dbConnection.connect()) {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE events SET " +
                            "tournamentDate = ?, " +
                            "registrationStart = ?, " +
                            "registrationEnd = ?, " +
                            "pdga = ?, " +
                            "tournamentTitle = ?, " +
                            "region = ?, " +
                            "externalLink = ? " +
                            "WHERE id = ? AND status != 'DELETED'");
            statement.setString(1, safeFormat(discGolfEventDTO.getTournamentDate()));
            statement.setString(2, safeFormat(discGolfEventDTO.getRegistrationStart()));
            statement.setString(3, safeFormat(discGolfEventDTO.getRegistrationEnd()));
            statement.setString(4, discGolfEventDTO.getPdga());
            statement.setString(5, discGolfEventDTO.getTournamentTitle());
            statement.setString(6, discGolfEventDTO.getRegion());
            statement.setString(7, discGolfEventDTO.getExternalLink());
            statement.setString(8, eventId);
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error while editing event with id: " + eventId, e);
        }
    }

    public void deleteEvent(String eventId) {
        log.info("Soft deleting event with id: {}", eventId);

        Connection connection = null;
        try {
            connection = dbConnection.connect();
            connection.setAutoCommit(false);

            validateEventExists(connection, eventId);
            deactivateUserEvents(connection, eventId);
            markEventAsDeleted(connection, eventId);

            connection.commit();
            log.info("Successfully soft deleted event with id: {}", eventId);

        } catch (SQLException e) {
            log.error("Error while soft deleting event with id: {}", eventId, e);
            if (connection != null) {
                try {
                    connection.rollback();
                    log.info("Transaction rolled back for event: {}", eventId);
                } catch (SQLException rollbackException) {
                    log.error("Error during rollback for event: {}", eventId, rollbackException);
                }
            }
            throw new RuntimeException("Error while soft deleting event with id: " + eventId, e);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    log.error("Error while closing connection", e);
                }
            }
        }
    }

    private void validateEventExists(Connection connection, String eventId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM events WHERE id = ? AND status != 'DELETED'")) {
            stmt.setString(1, eventId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                throw new IllegalArgumentException("Event with id " + eventId + " not found or already deleted");
            }
        }
    }

    private void deactivateUserEvents(Connection connection, String eventId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE user_event SET active = false WHERE event_id = ?")) {
            stmt.setString(1, eventId);
            int updated = stmt.executeUpdate();
            log.info("Deactivated {} user registrations for event {}", updated, eventId);
        }
    }

    private void markEventAsDeleted(Connection connection, String eventId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE events SET status = 'DELETED' WHERE id = ?")) {
            stmt.setString(1, eventId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("Failed to mark event as deleted: " + eventId);
            }
            log.info("Event {} marked as DELETED", eventId);
        }
    }

    public boolean eventExistsByTitle(String tournamentTitle) {
        try (Connection connection = dbConnection.connect()) {
            String query = "SELECT COUNT(*) FROM events WHERE tournamentTitle = ? AND status != 'DELETED'";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, tournamentTitle);

            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error checking if event exists: {}", tournamentTitle, e);
            throw new IllegalStateException("Error checking if event exists: " + tournamentTitle, e);
        }
    }

    public void updateEventByTitle(String tournamentTitle, DiscGolfEventDTO event) {
        try (Connection connection = dbConnection.connect()) {
            String query = "UPDATE events SET " +
                    "tournamentDate = ?, " +
                    "registrationStart = ?, " +
                    "registrationEnd = ?, " +
                    "pdga = ?, " +
                    "region = ?, " +
                    "externalLink = ? " +
                    "WHERE tournamentTitle = ? AND status != 'DELETED'";

            PreparedStatement stmt = connection.prepareStatement(query);

            setDateParameter(stmt, 1, event.getTournamentDate());
            setDateParameter(stmt, 2, event.getRegistrationStart());
            setDateParameter(stmt, 3, event.getRegistrationEnd());

            stmt.setString(4, event.getPdga());
            stmt.setString(5, event.getRegion());
            stmt.setString(6, event.getExternalLink());
            stmt.setString(7, tournamentTitle);

            stmt.executeUpdate();
            log.info("Updated event: {}", tournamentTitle);

        } catch (SQLException e) {
            log.error("Error updating event: {}", tournamentTitle, e);
            throw new RuntimeException("Failed to update event", e);
        }
    }

    private void setDateParameter(PreparedStatement stmt, int parameterIndex, Date date) throws SQLException {
        if (date != null) {
            stmt.setDate(parameterIndex, new java.sql.Date(date.getTime()));
        } else {
            stmt.setNull(parameterIndex, java.sql.Types.DATE);
        }
    }
}
