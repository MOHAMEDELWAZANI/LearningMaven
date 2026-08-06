package com.example;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        BookShop bookShop = new BookShop();

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

        System.out.println(bookShop.totalValue());

        System.out.println(bookShop.toJson());
    }
}
