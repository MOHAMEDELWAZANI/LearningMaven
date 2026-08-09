package com.example;
import com.example.DAO.DAOBook;
import com.example.DAO.DAOFactory;
import com.example.Model.Book;
import com.example.Model.BookShop;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        Properties config = new Properties();
        try(InputStream in = App.class.getResourceAsStream("/shop.properties")){
            if (in == null){
                throw new IllegalStateException(" shop.properties not found in classpath !");
            }
            config.load(in);
        } catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("Welcome to " + config.getProperty("shop.name")
                + " v" + config.getProperty("version"));






    }
}
