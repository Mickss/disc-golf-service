package org.micks.DiscGolfApplication.connection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class DiscGolfDbConnection {

    @Autowired
    private DatabaseConfigProperties databaseConfigProperties;

    public Connection connect() throws SQLException {
        String url = String.format("jdbc:mariadb://frog01.mikr.us:3306/%s?user=%s&password=%s",
                databaseConfigProperties.getName(),
                databaseConfigProperties.getUser(),
                databaseConfigProperties.getPassword()
        );
        return DriverManager.getConnection(url);
    }
}
