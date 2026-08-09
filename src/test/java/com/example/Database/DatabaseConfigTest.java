package com.example.Database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that database.properties is on the classpath and that Maven resource
 * filtering actually replaced the ${...} placeholders. A silent filtering
 * failure would otherwise only surface as a confusing JDBC error at runtime.
 */
@DisplayName("DatabaseConfig")
class DatabaseConfigTest {

    @Test
    void isASingleton() {
        assertSame(DatabaseConfig.getInstance(), DatabaseConfig.getInstance());
    }

    @Test
    void exposesEveryConnectionSetting() {
        DatabaseConfig config = DatabaseConfig.getInstance();

        assertAll(
                () -> assertNotNull(config.getUrl(), "database.url"),
                () -> assertNotNull(config.getUser(), "database.user"),
                () -> assertNotNull(config.getPassword(), "database.password")
        );
    }

    @Test
    void resolvesMavenPlaceholders() {
        DatabaseConfig config = DatabaseConfig.getInstance();

        assertAll(
                () -> assertFalse(config.getUrl().contains("${"),
                        "unfiltered placeholder in database.url: " + config.getUrl()),
                () -> assertFalse(config.getUser().contains("${"),
                        "unfiltered placeholder in database.user: " + config.getUser()),
                () -> assertFalse(config.getPassword().contains("${"),
                        "unfiltered placeholder in database.password")
        );
    }

    @Test
    void urlIsAJdbcUrl() {
        assertTrue(DatabaseConfig.getInstance().getUrl().startsWith("jdbc:"),
                DatabaseConfig.getInstance().getUrl());
    }
}
