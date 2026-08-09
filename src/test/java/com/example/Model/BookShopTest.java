package com.example.Model;

import com.example.support.TestBooks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for {@link BookShop} — no files, no database.
 *
 * The shop is rebuilt in @BeforeEach rather than @BeforeAll so that tests
 * which mutate it (remove, update) cannot influence the tests that run after
 * them. With a shared static fixture the suite passes or fails depending on
 * execution order, which is what made the original totalValue assertion look
 * correct while it was not.
 */
@DisplayName("BookShop")
class BookShopTest {

    private BookShop bookShop;

    @BeforeEach
    void setUp() {
        bookShop = TestBooks.shopWithFourBooks();
    }

    @Test
    void newShopIsEmpty() {
        BookShop empty = new BookShop();

        assertTrue(empty.getBooks().isEmpty());
        assertEquals(0.0, empty.totalValue(), 0.001);
    }

    @Test
    void addBookAppendsToTheCatalog() {
        bookShop.addBook(new Book(5L, "Refactoring", "Martin Fowler", 52.25));

        assertEquals(5, bookShop.getBooks().size());
        assertTrue(bookShop.findByTitle("Refactoring").isPresent());
    }

    @Nested
    @DisplayName("totalValue")
    class TotalValue {

        @Test
        void sumsThePriceOfEveryBook() {
            // 39.99 + 45.50 + 55.00 + 50.00 = 190.49
            assertEquals(TestBooks.FOUR_BOOKS_TOTAL, bookShop.totalValue(), 0.001);
            assertEquals(190.49, bookShop.totalValue(), 0.001);
        }

        @Test
        void dropsTheRemovedBookFromTheTotal() {
            bookShop.removeByTitle("Design");

            assertEquals(140.49, bookShop.totalValue(), 0.001);
        }

        @ParameterizedTest(name = "{0} + {1} = {2}")
        @CsvSource({
                "10.0, 20.0, 30.0",
                "5.5,   4.5, 10.0",
                "100.0, 50.0, 150.0",
                "0.0,   0.0,  0.0"
        })
        void addsUpForDifferentPrices(double price1, double price2, double expectedTotal) {
            BookShop shop = new BookShop();
            shop.addBook(new Book("Book1", "Author1", price1));
            shop.addBook(new Book("Book2", "Author2", price2));

            assertEquals(expectedTotal, shop.totalValue(), 0.001);
        }
    }

    @Nested
    @DisplayName("lookups")
    class Lookups {

        @Test
        void findByAuthorReturnsEveryMatchAndNothingElse() {
            List<Book> gamma = bookShop.findByAuthor("Erich Gamma");

            assertEquals(2, gamma.size(), "Erich Gamma wrote two of the fixture books");
            assertTrue(gamma.stream().allMatch(b -> b.getAuthor().equals("Erich Gamma")));
            assertEquals(List.of("Design Patterns", "Design"),
                    gamma.stream().map(Book::getTitle).toList());
        }

        @Test
        void findByAuthorIsEmptyForAnUnknownAuthor() {
            assertTrue(bookShop.findByAuthor("Nobody").isEmpty());
        }

        @Test
        void findByTitleReturnsTheMatchingBook() {
            Optional<Book> found = bookShop.findByTitle("Clean Code");

            assertTrue(found.isPresent());
            assertEquals("Robert C. Martin", found.get().getAuthor());
        }

        @Test
        void findByTitleIsEmptyForAnUnknownTitle() {
            assertTrue(bookShop.findByTitle("Nope").isEmpty());
        }

        @Test
        void findByTitleIsCaseSensitive() {
            assertTrue(bookShop.findByTitle("clean code").isEmpty());
        }

        @Test
        void findByIdReturnsTheMatchingBook() {
            Book found = bookShop.findById(2L);

            assertNotNull(found);
            assertEquals("Effective Java", found.getTitle());
        }

        @Test
        void findByIdReturnsNullForAnUnknownId() {
            assertNull(bookShop.findById(999L));
        }

        @Test
        void findByPriceRangeIsInclusiveOnBothBounds() {
            List<Book> inRange = bookShop.findByPriceRange(39.99, 50.00);

            assertEquals(List.of("Clean Code", "Effective Java", "Design"),
                    inRange.stream().map(Book::getTitle).toList());
        }

        @Test
        void findByPriceRangeIsEmptyWhenNothingMatches() {
            assertTrue(bookShop.findByPriceRange(1000.0, 2000.0).isEmpty());
        }
    }

    @Nested
    @DisplayName("mutations")
    class Mutations {

        @Test
        void removeByTitleDeletesTheBookAndReportsSuccess() {
            assertTrue(bookShop.removeByTitle("Design"));

            assertTrue(bookShop.findByTitle("Design").isEmpty());
            assertEquals(3, bookShop.getBooks().size());
        }

        @Test
        void removeByTitleLeavesSimilarTitlesAlone() {
            bookShop.removeByTitle("Design");

            assertTrue(bookShop.findByTitle("Design Patterns").isPresent(),
                    "removeByTitle must match exactly, not by prefix");
        }

        @Test
        void removeByTitleReportsFailureForAnUnknownTitle() {
            assertFalse(bookShop.removeByTitle("Nope"));
            assertEquals(4, bookShop.getBooks().size());
        }

        @Test
        void removeByIdDeletesTheBookAndReportsSuccess() {
            assertTrue(bookShop.removeById(1L));

            assertNull(bookShop.findById(1L));
            assertEquals(3, bookShop.getBooks().size());
        }

        @Test
        void removeByIdReportsFailureForAnUnknownId() {
            assertFalse(bookShop.removeById(999L));
            assertEquals(4, bookShop.getBooks().size());
        }

        @Test
        void updateOverwritesTitleAuthorAndPriceOfTheMatchingId() {
            assertTrue(bookShop.update(new Book(1L, "Clean Coder", "Uncle Bob", 20.00)));

            Book updated = bookShop.findById(1L);
            assertAll(
                    () -> assertEquals("Clean Coder", updated.getTitle()),
                    () -> assertEquals("Uncle Bob", updated.getAuthor()),
                    () -> assertEquals(20.00, updated.getPrice(), 0.001)
            );
        }

        @Test
        void updateReportsFailureAndChangesNothingForAnUnknownId() {
            assertFalse(bookShop.update(new Book(999L, "Ghost", "Nobody", 1.0)));

            assertEquals(TestBooks.FOUR_BOOKS_TOTAL, bookShop.totalValue(), 0.001);
        }
    }

    @Nested
    @DisplayName("sorting")
    class Sorting {

        @Test
        void sortedByPriceIsAscending() {
            assertEquals(
                    List.of("Clean Code", "Effective Java", "Design", "Design Patterns"),
                    bookShop.sortedByPrice().stream().map(Book::getTitle).toList());
        }

        @Test
        void sortedByTitleIsAlphabetical() {
            assertEquals(
                    List.of("Clean Code", "Design", "Design Patterns", "Effective Java"),
                    bookShop.sortedByTitle().stream().map(Book::getTitle).toList());
        }

        @Test
        void sortingDoesNotMutateTheCatalog() {
            bookShop.sortedByPrice();

            assertEquals(List.of("Clean Code", "Effective Java", "Design Patterns", "Design"),
                    bookShop.getBooks().stream().map(Book::getTitle).toList());
        }
    }

    @Nested
    @DisplayName("json")
    class Json {

        @Test
        void toJsonContainsEveryTitle() {
            String json = bookShop.toJson();

            assertAll(
                    () -> assertTrue(json.contains("Clean Code"), json),
                    () -> assertTrue(json.contains("Effective Java"), json),
                    () -> assertTrue(json.contains("Design Patterns"), json),
                    () -> assertTrue(json.contains("\"title\": \"Design\""), json)
            );
        }

        @Test
        void toJsonOfAnEmptyShopIsAnEmptyArray() {
            assertEquals("[]", new BookShop().toJson());
        }

        @Test
        void loadFromAddsToTheExistingCatalogInsteadOfReplacingIt() {
            String json = """
                    [ { "title": "Refactoring", "author": "Martin Fowler", "price": 52.25 } ]
                    """;

            bookShop.loadFrom(stream(json));

            assertEquals(5, bookShop.getBooks().size());
            assertTrue(bookShop.findByTitle("Refactoring").isPresent());
            assertEquals(TestBooks.FOUR_BOOKS_TOTAL + 52.25, bookShop.totalValue(), 0.001);
        }

        @Test
        void loadFromReadsTitleAuthorAndPrice() {
            BookShop shop = new BookShop();

            shop.loadFrom(stream("""
                    [ { "title": "Refactoring", "author": "Martin Fowler", "price": 52.25 } ]
                    """));

            Book book = shop.getBooks().get(0);
            assertAll(
                    () -> assertEquals("Refactoring", book.getTitle()),
                    () -> assertEquals("Martin Fowler", book.getAuthor()),
                    () -> assertEquals(52.25, book.getPrice(), 0.001)
            );
        }

        @Test
        void toJsonThenLoadFromRoundTripsTheCatalog() {
            BookShop reloaded = new BookShop();

            reloaded.loadFrom(stream(bookShop.toJson()));

            assertEquals(bookShop.getBooks(), reloaded.getBooks());
            assertEquals(bookShop.totalValue(), reloaded.totalValue(), 0.001);
        }

        private InputStream stream(String json) {
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }
    }
}
