package org.messenger.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseConnection implements Closeable {

    private static final HikariDataSource dataSource;
    private Connection connection;

    static {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://127.0.0.1/postgres");
        config.setUsername("postgres");
        config.setPassword("password");
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);
    }

    public DataBaseConnection(){
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public ResultSet executeQuery(String query) throws SQLException {
        if(connection == null) throw new SQLException("Connection was closed");
        ResultSet resultSet = null;
        Statement statement = connection.createStatement();
        resultSet = statement.executeQuery(query);
        return resultSet;
    }
    public void execute(String query) throws SQLException{
        if(connection == null) throw new SQLException("Connection was closed");
        Statement statement = connection.createStatement();
        statement.execute(query);
    }
    @Override
    public void close(){
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        connection = null;
    }

}
