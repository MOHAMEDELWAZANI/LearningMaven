package com.example.integration;

import com.example.DAO.DAO;
import com.example.DAO.DAOFactory;
import com.example.Database.DatabaseConnection;
import com.example.Model.Book;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test against the real Oracle database configured in
 * database.properties.
 *
 * If the database is unreachable the whole class is *skipped*, not failed, so
 * `mvn verify` still works on a machine without the container running. Start
 * it and the tests light up automatically.
 *
 * Every row this test creates carries the {@value #TITLE_PREFIX} prefix and is
 * deleted again after each test, so it never touches existing data.
 */
@DisplayName("DAOBook against a real database")
class DAOBookIT {

    static final String TITLE_PREFIX = "IT-BOOK-";
    private static final int LOGIN_TIMEOUT_SECONDS = 5;

    private static DAO dao;

    private Book saved;

    @BeforeAll
    static void requireADatabase() {
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        assumeTrue(booksTableIsReachable(),
                "Oracle database or BOOKS table unavailable - skipping DAO integration tests");
        dao = DAOFactory.getDAOBook();
    }

    @BeforeEach
    void insertOneBook() {
        saved = newBook("Clean Code", "Robert C. Martin", 39.99);
        assertTrue(dao.save(saved), "fixture insert failed");
        saved = dao.findByTitle(saved.getTitle());
        assertNotNull(saved, "fixture insert was not readable back");
    }

    @AfterEach
    void deleteRowsCreatedByThisTest() throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM books WHERE title LIKE '" + TITLE_PREFIX + "%'");
        }
    }

    @Test
    void saveThenFindByTitleReturnsThePersistedBook() {
        Book found = dao.findByTitle(saved.getTitle());

        assertNotNull(found);
        assertAll(
                () -> assertTrue(found.getId() > 0, "the database should have assigned an id"),
                () -> assertEquals("Robert C. Martin", found.getAuthor()),
                () -> assertEquals(39.99, found.getPrice(), 0.001)
        );
    }

    @Test
    void findByTitleReturnsNullForAnUnknownTitle() {
        assertNull(dao.findByTitle(TITLE_PREFIX + "does-not-exist"));
    }

    @Test
    void findByIdReturnsThePersistedBook() {
        Book found = dao.findById(saved.getId());

        assertNotNull(found);
        assertEquals(saved.getTitle(), found.getTitle());
        assertEquals(saved.getId(), found.getId());
    }

    @Test
    void findByIdReturnsNullForAnUnknownId() {
        assertNull(dao.findById(-1L));
    }

    @Test
    void findAllContainsTheSavedBook() {
        List<Book> all = dao.findAll();

        assertTrue(all.stream().anyMatch(b -> b.getId() == saved.getId()),
                "findAll should return the row inserted by this test");
    }

    @Test
    void findByAuthorReturnsOnlyThatAuthor() {
        dao.save(newBook("Clean Architecture", "Robert C. Martin", 42.00));

        List<Book> byAuthor = dao.findByAuthor("Robert C. Martin");

        assertTrue(byAuthor.size() >= 2);
        assertTrue(byAuthor.stream().allMatch(b -> b.getAuthor().equals("Robert C. Martin")));
    }

    @Test
    void findByAuthorIsEmptyForAnUnknownAuthor() {
        assertTrue(dao.findByAuthor(TITLE_PREFIX + "nobody").isEmpty());
    }

    @Test
    void updateChangesThePersistedRow() {
        saved.setTitle(TITLE_PREFIX + "renamed");
        saved.setAuthor("Uncle Bob");
        saved.setPrice(11.11);

        assertTrue(dao.update(saved));

        Book reloaded = dao.findById(saved.getId());
        assertAll(
                () -> assertEquals(TITLE_PREFIX + "renamed", reloaded.getTitle()),
                () -> assertEquals("Uncle Bob", reloaded.getAuthor()),
                () -> assertEquals(11.11, reloaded.getPrice(), 0.001)
        );
    }

    @Test
    void updateReportsFailureForAnUnknownId() {
        assertFalse(dao.update(new Book(-1L, TITLE_PREFIX + "ghost", "Nobody", 1.0)));
    }

    @Test
    void deleteByIdRemovesTheRow() {
        assertTrue(dao.deleteById(saved.getId()));

        assertNull(dao.findById(saved.getId()));
    }

    @Test
    void deleteByIdReportsFailureForAnUnknownId() {
        assertFalse(dao.deleteById(-1L));
    }

    @Test
    void countGrowsByOneAfterEachSave() {
        int before = dao.count();

        assertTrue(dao.save(newBook("Refactoring", "Martin Fowler", 52.25)));

        assertEquals(before + 1, dao.count());
    }

    @Test
    void deleteAllEmptiesTheTable() {
        assumeOnlyTestRowsArePresent();
        dao.save(newBook("Refactoring", "Martin Fowler", 52.25));
        assertTrue(dao.count() >= 2, "need several rows to prove deleteAll removes all of them");

        dao.deleteAll();

        assertEquals(0, dao.count());
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    void deleteAllOnAnAlreadyEmptyTableIsHarmless() {
        assumeOnlyTestRowsArePresent();
        dao.deleteAll();
        assertEquals(0, dao.count());

        assertDoesNotThrow(() -> dao.deleteAll());

        assertEquals(0, dao.count());
    }

    /**
     * deleteAll() is a `DELETE FROM books` with no WHERE clause, so it is only
     * safe to exercise on a table that holds nothing but this test's own rows.
     * On a database with real data the test is skipped rather than run.
     */
    private static void assumeOnlyTestRowsArePresent() {
        List<Book> foreign = dao.findAll().stream()
                .filter(b -> b.getTitle() == null || !b.getTitle().startsWith(TITLE_PREFIX))
                .toList();

        assumeTrue(foreign.isEmpty(),
                () -> "BOOKS holds " + foreign.size() + " row(s) this test did not create"
                        + " - skipping the destructive deleteAll test");
    }

    /** Unique titles keep parallel or repeated runs from colliding. */
    private static Book newBook(String title, String author, double price) {
        return new Book(TITLE_PREFIX + title + "-" + System.nanoTime(), author, price);
    }

    private static boolean booksTableIsReachable() {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement st = c.createStatement()) {
            st.executeQuery("SELECT 1 FROM books WHERE 1 = 0").close();
            return true;
        } catch (Throwable connectionOrTableMissing) {
            System.out.println("[DAOBookIT] skipped: " + connectionOrTableMissing.getMessage());
            return false;
        }
    }
}
