package com.example.DAO;
import com.example.Model.Book;
import java.util.List;
public interface DAO {
    boolean save(Book b);
    List<Book> findAll();
    Book findByTitle(String title);
    Book findById(long id);
    boolean deleteById(long id);
    boolean update(Book book);
    List<Book> findByAuthor(String author);
    int count();
    void deleteAll();
}
