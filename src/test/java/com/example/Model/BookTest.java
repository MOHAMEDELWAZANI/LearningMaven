package com.example.Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Book")
class BookTest {

    @Test
    void fullConstructorSetsEveryField() {
        Book book = new Book(7L, "Clean Code", "Robert C. Martin", 39.99);

        assertAll(
                () -> assertEquals(7L, book.getId()),
                () -> assertEquals("Clean Code", book.getTitle()),
                () -> assertEquals("Robert C. Martin", book.getAuthor()),
                () -> assertEquals(39.99, book.getPrice(), 0.001)
        );
    }

    @Test
    void constructorWithoutIdLeavesIdAtZero() {
        Book book = new Book("Clean Code", "Robert C. Martin", 39.99);

        assertEquals(0L, book.getId());
    }

    @Test
    void settersReplaceValues() {
        Book book = new Book();

        book.setId(3L);
        book.setTitle("Refactoring");
        book.setAuthor("Martin Fowler");
        book.setPrice(52.25);

        assertAll(
                () -> assertEquals(3L, book.getId()),
                () -> assertEquals("Refactoring", book.getTitle()),
                () -> assertEquals("Martin Fowler", book.getAuthor()),
                () -> assertEquals(52.25, book.getPrice(), 0.001)
        );
    }

    @Test
    void toStringContainsEveryField() {
        String text = new Book(7L, "Clean Code", "Robert C. Martin", 39.99).toString();

        assertAll(
                () -> assertTrue(text.contains("7"), text),
                () -> assertTrue(text.contains("Clean Code"), text),
                () -> assertTrue(text.contains("Robert C. Martin"), text),
                () -> assertTrue(text.contains("39.99"), text)
        );
    }

    /**
     * Book identity is (title, author) on purpose: id and price are ignored.
     * These tests pin that contract down so a future change to equals() is a
     * deliberate decision and not an accident.
     */
    @Nested
    @DisplayName("equals / hashCode")
    class EqualsAndHashCode {

        @Test
        void sameTitleAndAuthorAreEqualEvenWithDifferentIdAndPrice() {
            Book a = new Book(1L, "Clean Code", "Robert C. Martin", 39.99);
            Book b = new Book(99L, "Clean Code", "Robert C. Martin", 10.00);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void differentTitleIsNotEqual() {
            Book a = new Book("Clean Code", "Robert C. Martin", 39.99);
            Book b = new Book("Clean Architecture", "Robert C. Martin", 39.99);

            assertNotEquals(a, b);
        }

        @Test
        void differentAuthorIsNotEqual() {
            Book a = new Book("Clean Code", "Robert C. Martin", 39.99);
            Book b = new Book("Clean Code", "Someone Else", 39.99);

            assertNotEquals(a, b);
        }

        @Test
        void isReflexiveAndNullSafeAndTypeSafe() {
            Book book = new Book("Clean Code", "Robert C. Martin", 39.99);

            assertEquals(book, book);
            assertNotEquals(null, book);
            assertNotEquals("Clean Code", book);
        }
    }
}
