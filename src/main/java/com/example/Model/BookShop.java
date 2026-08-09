package com.example.Model;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    public boolean removeByTitle(String title){
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

    public boolean removeById(long id ){
        return Books.removeIf(b -> b.getId() == id);
    }
    public boolean update(Book book){
        for(Book b : Books){
            if(b.getId() == book.getId()){
                b.setTitle(book.getTitle());
                b.setPrice(book.getPrice());
                b.setAuthor(book.getAuthor());
                return true;
            }
        } return false;
    }

    public List<Book> findByPriceRange(double minPrice , double maxPrice){
        return Books.stream().filter(b -> b.getPrice()>= minPrice && b.getPrice()<= maxPrice).toList();
    }
    public List<Book> sortedByPrice() {
        return Books.stream()
                .sorted(Comparator.comparingDouble(Book::getPrice))
                .toList();
    }

    public List<Book> sortedByTitle() {
        return Books.stream()
                .sorted(Comparator.comparing(Book::getTitle))
                .toList();
    }


}
