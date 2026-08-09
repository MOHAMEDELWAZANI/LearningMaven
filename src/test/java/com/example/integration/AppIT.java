package com.example.integration;

import com.example.App;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: runs the real entry point and inspects what it prints.
 * This covers the chain shop.properties -> Maven filtering -> classpath
 * resource -> App.main, which no unit test can exercise on its own.
 */
@DisplayName("App entry point")
class AppIT {

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void captureStdOut() {
        originalOut = System.out;
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdOut() {
        System.setOut(originalOut);
    }

    @Test
    void printsTheConfiguredShopNameAndVersion() {
        App.main(new String[0]);

        String output = captured.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(output.startsWith("Welcome to "), output),
                () -> assertTrue(output.contains("Bookshop"),
                        "shop.name should come from the active Maven profile: " + output),
                () -> assertTrue(output.contains(" v1.0-SNAPSHOT"),
                        "version should be filtered in from ${project.version}: " + output)
        );
    }

    @Test
    void resolvesEveryPlaceholderInShopProperties() {
        App.main(new String[0]);

        String output = captured.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertFalse(output.contains("${"),
                        "unfiltered placeholder reached stdout: " + output),
                () -> assertFalse(output.contains("null"),
                        "a property was missing from shop.properties: " + output)
        );
    }

    @Test
    void ignoresCommandLineArguments() {
        assertDoesNotThrow(() -> App.main(new String[]{"unexpected", "args"}));
    }
}
