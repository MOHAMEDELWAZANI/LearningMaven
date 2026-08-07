package com.example.Model;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BookShop {
    private List<Book> Books ;

    public BookShop(){
        Books = new ArrayList<>();
    }
    public void addBook(Book b){
        this.Books.add(b);
    }
    public double totalValue(){
        return this.Books.stream().mapToDouble(Book::getPrice).sum();
    }
    public List<Book> findByAuthor(String author){
        return this.Books.stream().filter(book -> book.getAuthor().equals(author)).toList();
    }
    public Book findById(long id) {
        return Books.stream()
                .filter(book -> book.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<Book> getBooks() {
        return Books;
    }

    public String toJson(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this.Books);
    }
    public boolean removeByTittle(String title){
        return Books.removeIf(b -> b.getTitle().equals(title));
    }
    public Optional<Book> findByTitle(String title){
        return Books.stream().filter(b -> b.getTitle().equals(title)).findFirst();
    }

    public void loadFrom(InputStream json) {
        Gson gson = new Gson();
        Book[] loaded = gson.fromJson(
                new InputStreamReader(json, StandardCharsets.UTF_8), Book[].class);
        Books.addAll(Arrays.asList(loaded));
    }


}
