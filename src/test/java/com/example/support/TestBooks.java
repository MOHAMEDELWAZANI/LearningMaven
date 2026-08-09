package com.example.support;

import com.example.Model.Book;
import com.example.Model.BookShop;

/**
 * Shared fixtures. Every factory returns a brand new object graph so tests
 * can mutate what they get without leaking state into the next test.
 */
public final class TestBooks {

    public static final double CLEAN_CODE_PRICE = 39.99;
    public static final double EFFECTIVE_JAVA_PRICE = 45.50;
    public static final double DESIGN_PATTERNS_PRICE = 55.00;
    public static final double DESIGN_PRICE = 50.00;

    /** Sum of the four books in {@link #shopWithFourBooks()}. */
    public static final double FOUR_BOOKS_TOTAL =
            CLEAN_CODE_PRICE + EFFECTIVE_JAVA_PRICE + DESIGN_PATTERNS_PRICE + DESIGN_PRICE;

    private TestBooks() {
    }

    public static Book cleanCode() {
        return new Book(1L, "Clean Code", "Robert C. Martin", CLEAN_CODE_PRICE);
    }

    public static Book effectiveJava() {
        return new Book(2L, "Effective Java", "Joshua Bloch", EFFECTIVE_JAVA_PRICE);
    }

    public static Book designPatterns() {
        return new Book(3L, "Design Patterns", "Erich Gamma", DESIGN_PATTERNS_PRICE);
    }

    public static Book design() {
        return new Book(4L, "Design", "Erich Gamma", DESIGN_PRICE);
    }

    /** Four books, two of them by Erich Gamma. */
    public static BookShop shopWithFourBooks() {
        BookShop shop = new BookShop();
        shop.addBook(cleanCode());
        shop.addBook(effectiveJava());
        shop.addBook(designPatterns());
        shop.addBook(design());
        return shop;
    }
}
