package org.micks.DiscGolfApplication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DiscGolfEventService {

    public List<DiscGolfEventDTO> getEvents() throws SQLException {

        Connection connection = DriverManager.getConnection("jdbc:mariadb://app.disc-golf.pl:3306/disc_golf?user=dg_user2&password=MBV6qsa5rufDAHUe");

        String selectSQL = "SELECT * FROM Events";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(selectSQL);

        ArrayList<DiscGolfEventDTO> discGolfEventDTOList = new ArrayList<>();
        while (resultSet.next()) {
            DiscGolfEventDTO discGolfEventDTO = new DiscGolfEventDTO(
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
    }

    public void createEvent(DiscGolfEventDTO discGolfEventDTO) {
        log.info("Creating new event: {}", discGolfEventDTO.getTournamentTitle());
    }
}
