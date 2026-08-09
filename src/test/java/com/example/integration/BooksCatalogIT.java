package com.example.integration;

import com.example.Model.Book;
import com.example.Model.BookShop;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: the real books.json shipped in src/main/resources, read
 * off the classpath and parsed by the real Gson binding. Nothing is stubbed —
 * this is the wiring between the packaged resource, Gson and BookShop.
 *
 * The catalog is read once (@BeforeAll) because none of these tests mutate it.
 */
@DisplayName("books.json catalog")
class BooksCatalogIT {

    private static final int EXPECTED_BOOKS = 1000;
    private static final double EXPECTED_TOTAL = 58849.26;

    private static BookShop catalog;

    @BeforeAll
    static void loadCatalog() throws Exception {
        catalog = new BookShop();
        try (InputStream in = BooksCatalogIT.class.getResourceAsStream("/books.json")) {
            assertNotNull(in, "books.json is missing from the classpath");
            catalog.loadFrom(in);
        }
    }

    @Test
    void everyBookIsLoaded() {
        assertEquals(EXPECTED_BOOKS, catalog.getBooks().size());
    }

    @Test
    void totalValueMatchesTheCatalog() {
        assertEquals(EXPECTED_TOTAL, catalog.totalValue(), 0.01);
    }

    @Test
    void everyBookHasATitleAnAuthorAndAPositivePrice() {
        List<Book> invalid = catalog.getBooks().stream()
                .filter(b -> b.getTitle() == null || b.getTitle().isBlank()
                        || b.getAuthor() == null || b.getAuthor().isBlank()
                        || b.getPrice() <= 0)
                .toList();

        assertTrue(invalid.isEmpty(), () -> "malformed entries in books.json: " + invalid);
    }

    @Test
    void findByTitleLocatesAKnownBook() {
        Book book = catalog.findByTitle("Effective Java - Edition 1").orElseThrow();

        assertEquals("Joshua Bloch", book.getAuthor());
        assertEquals(40.36, book.getPrice(), 0.001);
    }

    @Test
    void findByAuthorReturnsOnlyThatAuthor() {
        List<Book> bloch = catalog.findByAuthor("Joshua Bloch");

        assertFalse(bloch.isEmpty(), "Joshua Bloch should be in the catalog");
        assertTrue(bloch.stream().allMatch(b -> b.getAuthor().equals("Joshua Bloch")));
    }

    @Test
    void findByPriceRangeStaysInsideTheBounds() {
        List<Book> midRange = catalog.findByPriceRange(20.0, 30.0);

        assertFalse(midRange.isEmpty());
        assertTrue(midRange.stream().allMatch(b -> b.getPrice() >= 20.0 && b.getPrice() <= 30.0));
    }

    @Test
    void sortedByPriceIsAscendingOverTheWholeCatalog() {
        List<Book> sorted = catalog.sortedByPrice();

        assertEquals(EXPECTED_BOOKS, sorted.size());
        assertTrue(isSorted(sorted, Comparator.comparingDouble(Book::getPrice)));
    }

    @Test
    void sortedByTitleIsAlphabeticalOverTheWholeCatalog() {
        List<Book> sorted = catalog.sortedByTitle();

        assertEquals(EXPECTED_BOOKS, sorted.size());
        assertTrue(isSorted(sorted, Comparator.comparing(Book::getTitle)));
    }

    @Test
    void serialisingAndReloadingThePlainCatalogPreservesIt() {
        BookShop reloaded = new BookShop();

        reloaded.loadFrom(new ByteArrayInputStream(
                catalog.toJson().getBytes(StandardCharsets.UTF_8)));

        assertEquals(catalog.getBooks().size(), reloaded.getBooks().size());
        assertEquals(catalog.totalValue(), reloaded.totalValue(), 0.01);
        assertEquals(catalog.getBooks(), reloaded.getBooks());
    }

    @Test
    void accentedAuthorsSurviveTheUtf8Decoding() {
        assertFalse(catalog.findByAuthor("Aurélien Géron").isEmpty(),
                "books.json must be read as UTF-8");
    }

    private static boolean isSorted(List<Book> books, Comparator<Book> order) {
        for (int i = 1; i < books.size(); i++) {
            if (order.compare(books.get(i - 1), books.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }
}
