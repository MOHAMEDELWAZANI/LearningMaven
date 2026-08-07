package com.example.Database;
import java.io.InputStream;
import java.util.Properties;
public class DatabaseConfig {
    public static String url ;
    public static String user;
    public static String password;
    private static final Properties PROPS = new Properties();
    static {
        try (InputStream in = DatabaseConfig.class
                .getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "database.properties not found on the classpath");
            }
            PROPS.load(in);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(
                    "Could not load database configuration: " + e.getMessage());
        }
    }
    private DatabaseConfig(){
        url = PROPS.getProperty("database.url");
        user = PROPS.getProperty("database.user");
        password = PROPS.getProperty("database.password");
    }
    private static final DatabaseConfig INSTANCE = new DatabaseConfig();
    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }
    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }



}
