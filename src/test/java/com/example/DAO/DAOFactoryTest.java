package com.example.DAO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DAOFactory")
class DAOFactoryTest {

    @Test
    void handsOutABookDao() {
        DAO dao = DAOFactory.getDAOBook();

        assertNotNull(dao);
        assertInstanceOf(DAOBook.class, dao);
    }

    @Test
    void alwaysHandsOutTheSameInstance() {
        assertSame(DAOFactory.getDAOBook(), DAOFactory.getDAOBook());
    }
}
