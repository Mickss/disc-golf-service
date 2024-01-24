package org.micks.DiscGolfApplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DiscGolfDbConnection {

    @Autowired
    private DatabaseConfigProperties databaseConfigProperties;

    public ResultSet executeQuery(String sqlQuery) throws SQLException {
        String url = String.format("jdbc:mariadb://app.disc-golf.pl:3306/%s?user=%s&password=%s",
                databaseConfigProperties.getName(),
                databaseConfigProperties.getUser(),
                databaseConfigProperties.getPassword()
        );
        Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement();
        return statement.executeQuery(sqlQuery);
    }
}
