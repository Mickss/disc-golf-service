package org.micks.DiscGolfApplication;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DiscGolfDbConnection {

    public ResultSet executeQuery(String sqlQuery) throws SQLException {
            Connection connection = DriverManager.getConnection("jdbc:mariadb://app.disc-golf.pl:3306/disc_golf?user=dg_user2&password=MBV6qsa5rufDAHUe");
            Statement statement = connection.createStatement();
            return statement.executeQuery(sqlQuery);
    }
}
