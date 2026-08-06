package com.example;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

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
    public String toJson(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this.Books);
    }

}
