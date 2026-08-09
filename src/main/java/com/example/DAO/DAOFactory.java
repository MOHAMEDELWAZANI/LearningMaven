package com.example.DAO;

public class DAOFactory {
    private static final DAO BOOK_DAO = new DAOBook();
    private DAOFactory(){}
    public static DAO getDAOBook(){
        return BOOK_DAO;
    }
}
