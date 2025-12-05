package org.micks.DiscGolfApplication.connection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DiscGolfDbConnection {

    @Autowired
    private DataSource dataSource;

    public Connection connect() throws SQLException {
        return dataSource.getConnection();
    }
}
