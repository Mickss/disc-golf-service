package org.micks.DiscGolfApplication;

import lombok.extern.slf4j.Slf4j;
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
public class DiscGolfEventService {

    @Autowired
    private DiscGolfDbConnection dbConnection;

    public List<DiscGolfEventDTO> getEvents() {
        try (Connection connection = dbConnection.connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM Events");
            List<DiscGolfEventDTO> discGolfEventDTOList = new ArrayList<>();
            while (resultSet.next()) {
                DiscGolfEventDTO discGolfEventDTO = new DiscGolfEventDTO(
                        resultSet.getString("id"),
                        resultSet.getString("tournamentDate"),
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
            statement.setString(1, discGolfEventDTO.getTournamentDate());
            statement.setString(2, discGolfEventDTO.getPdga());
            statement.setString(3, discGolfEventDTO.getTournamentTitle());
            statement.setString(4, discGolfEventDTO.getRegion());
            statement.setString(5, discGolfEventDTO.getRegistration());
            statement.setString(6, discGolfEventDTO.getVacancies());
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
