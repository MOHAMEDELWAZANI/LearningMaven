package com.example.DAO;

public class DAOFactory {
    private static final DAOBook BOOK_DAO = new DAOBook();
    private DAOFactory(){}
    public static DAOBook getDAOBook(){
        return BOOK_DAO;
    }
}
