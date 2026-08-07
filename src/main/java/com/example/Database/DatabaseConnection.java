package com.example.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance = new DatabaseConnection();
            private DatabaseConnection(){
            try{
                Class.forName("oracle.jdbc.OracleDriver");
            }catch(ClassNotFoundException e){
                throw new IllegalStateException(
                        "Oracle JDBC driver not on the classpath", e);
            }
            }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.getInstance().getUrl() ,
                DatabaseConfig.getInstance().getUser(),
                DatabaseConfig.getInstance().getPassword());
    }
}
