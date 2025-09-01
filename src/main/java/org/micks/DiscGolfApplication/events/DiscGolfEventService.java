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
import java.util.List;

@Service
@Slf4j
public class DiscGolfEventService {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

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
            String query = "SELECT * FROM Events";
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
                        resultSet.getString("pdga"),
                        resultSet.getString("tournamentTitle"),
                        resultSet.getString("region"),
                        resultSet.getString("registration"),
                        resultSet.getString("vacancies")
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
            PreparedStatement statement = connection.prepareStatement("insert into Events values(UUID(),?,?,?,?,?,?)");
            statement.setString(1, DATE_FORMAT.format(discGolfEventDTO.getTournamentDate()));
            statement.setString(2, discGolfEventDTO.getPdga());
            statement.setString(3, discGolfEventDTO.getTournamentTitle());
            statement.setString(4, discGolfEventDTO.getRegion());
            statement.setString(5, discGolfEventDTO.getRegistration());
            statement.setString(6, discGolfEventDTO.getVacancies());
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error while creating event with title: " + discGolfEventDTO.getTournamentTitle(), e);
        }
    }

    public DiscGolfEventDTO getEvent(String eventId) {
        try (Connection connection = dbConnection.connect()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM Events WHERE ID = ?");
            statement.setString(1, eventId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                DiscGolfEventDTO discGolfEventDTO = new DiscGolfEventDTO(
                        resultSet.getString("id"),
                        resultSet.getDate("tournamentDate"),
                        resultSet.getString("pdga"),
                        resultSet.getString("tournamentTitle"),
                        resultSet.getString("region"),
                        resultSet.getString("registration"),
                        resultSet.getString("vacancies")
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
            PreparedStatement statement = connection.prepareStatement
                    ("UPDATE Events SET tournamentDate = ?, pdga = ?, tournamentTitle = ?, region = ?, registration = ? WHERE id = ?");
            statement.setString(1, DATE_FORMAT.format(discGolfEventDTO.getTournamentDate()));
            statement.setString(2, discGolfEventDTO.getPdga());
            statement.setString(3, discGolfEventDTO.getTournamentTitle());
            statement.setString(4, discGolfEventDTO.getRegion());
            statement.setString(5, discGolfEventDTO.getRegistration());
            statement.setString(6, eventId);
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error while editing event with id: " + eventId, e);
        }
    }

    public void deleteEvent(String eventId) {
        log.info("Deleting event with id: {}", eventId);
        try (Connection connection = dbConnection.connect()) {
            connection.setAutoCommit(false);

            validateEventExists(connection, eventId);
            deleteUserEvents(connection, eventId);
            deleteEventRecord(connection, eventId);

            connection.commit();
        } catch (SQLException e) {
            log.error("Error while deleting event with id: {}", eventId, e);
            throw new RuntimeException("Error while deleting event with id: " + eventId, e);
        }
    }

    private void validateEventExists(Connection connection, String eventId) throws SQLException {
        try (PreparedStatement checkStatement = connection.prepareStatement(
                "SELECT COUNT(*) FROM Events WHERE id = ?")) {
            checkStatement.setString(1, eventId);
            ResultSet rs = checkStatement.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                throw new IllegalArgumentException("Event with id " + eventId + " not found");
            }
        }
    }

    private void deleteUserEvents(Connection connection, String eventId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM user_event WHERE event_id = ?")) {
            stmt.setString(1, eventId);
            int deleted = stmt.executeUpdate();
            log.info("Deleted {} user registrations for event {}", deleted, eventId);
        }
    }

    private void deleteEventRecord(Connection connection, String eventId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM Events WHERE id = ?")) {
            stmt.setString(1, eventId);
            int deleted = stmt.executeUpdate();
            if (deleted == 0) {
                throw new IllegalStateException("Failed to delete event with id: " + eventId);
            }
            log.info("Successfully deleted event with id: {}", eventId);
        }
    }
}
