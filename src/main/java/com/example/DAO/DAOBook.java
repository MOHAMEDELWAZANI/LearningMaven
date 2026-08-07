package com.example.DAO;

import com.example.Model.Book;
import com.example.Database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DAOBook implements DAO{

    @Override
    public boolean save(Book b) {
        String sql =
                "INSERT INTO books (title, author, price) VALUES (?, ?, ?)";

        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            ps.setDouble(3, b.getPrice());

            int rows = ps.executeUpdate();
            return rows == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        try(Connection c = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT * from books");
            ResultSet rs = ps.executeQuery()){

            while(rs.next()){
                Book b = new Book();
                b.setId(rs.getLong("id"));
                 b.setAuthor(rs.getString("author"));
                 b.setPrice(rs.getDouble("price"));
                 b.setTitle(rs.getString("title"));
                books.add(b);
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return books;
    }


    @Override
    public Book findById(long id) {
        Book b = new Book();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("select * from books where id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                    b.setId(id);
                    b.setAuthor(rs.getString("author"));
                    b.setPrice(rs.getDouble("price"));
                    b.setTitle(rs.getString("title"));

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return b;
    }

    @Override
    public boolean update(Book book) {
        String sql = """
            UPDATE books
            SET title = ?, author = ?, price = ?
            WHERE id = ?
            """;

        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setDouble(3, book.getPrice());
            ps.setLong(4, book.getId());

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public boolean deleteById(long id) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("delete from books where id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1 ;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Book findByTitle(String title) {
        Book b = new Book();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("select * from books where title = ?")) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;          // ← rien trouvé, et ça se voit
                }

                    b.setId(rs.getLong("id"));
                    b.setAuthor(rs.getString("author"));
                    b.setPrice(rs.getDouble("price"));
                    b.setTitle(title);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return b;
    }
}
