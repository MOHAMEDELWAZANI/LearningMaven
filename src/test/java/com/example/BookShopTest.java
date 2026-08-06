package com.example;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookShopTest {

    private BookShop bookShop;
    @BeforeEach
    void setUp(){
        bookShop = new BookShop();
        bookShop.addBook(new Book(
                "Clean Code",
                "Robert C. Martin",
                39.99
        ));

        bookShop.addBook(new Book(
                "Effective Java",
                "Joshua Bloch",
                45.50
        ));

        bookShop.addBook(new Book(
                "Design Patterns",
                "Erich Gamma",
                55.00
        ));
        bookShop.addBook(new Book(
                "Design",
                "Erich Gamma",
                50
        ));
    }

    @Test
    void totalValueShouldBeCorrect(){
        double expectedValue = 190.49;
        double actualValue = bookShop.totalValue();
        assertEquals(expectedValue, actualValue,0.001);
    }
    @Test
    void findByAuthorShouldBeCorrect(){
        assertTrue(bookShop.findByAuthor("Erich Gamma").stream().filter(book -> !book.getAuthor().equals("Erich Gamma")).toList().isEmpty());
    }
    @Test
    void toJsonShouldBeCorrect(){

        String json = bookShop.toJson();
        assertTrue(json.contains("Clean Code"));
        assertTrue(json.contains("Effective Java"));
        assertTrue(json.contains("Design Patterns"));
        assertTrue(json.contains("Design"));
    }

    @ParameterizedTest
    @CsvSource({
            "10.0,20.0,30.0",
            "5.5,4.5,10.0",
            "100.0,50.0,150.0"
    })
    void totalValueShouldWorkForDifferentPrices(
            double price1,
            double price2,
            double expectedTotal) {

        BookShop shop = new BookShop();

        shop.addBook(new Book("Book1", "Author1", price1));
        shop.addBook(new Book("Book2", "Author2", price2));

        assertEquals(expectedTotal, shop.totalValue(), 0.001);
    }
}
