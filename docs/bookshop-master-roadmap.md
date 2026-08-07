# Bookshop — Maven Master Roadmap, Explained

**A phase-by-phase companion for finishing the bookshop and mastering Maven.**

You are on day 3 of Maven. You already built a working single-module project
with real tests, resource filtering, and a fat jar. This guide takes your own
roadmap (Phases 1–9) and explains **every concept on it**: what it is, why it
exists, when you will use it, and how to build it *in your bookshop* — with
code taken from your actual files, not invented examples.

Rules of the road:

1. **Type everything yourself.** Copy-pasting teaches your clipboard, not you.
2. **Run the command after every change.** Maven is learned in the terminal.
3. **Break things on purpose** when the guide says so. The error message is
   the lesson.
4. Each phase ends with a **checkpoint**: commands whose output proves you
   understood the phase.

---

# Chapter 0 — Where You Are Right Now (Audit)

I read your project. Here is the honest status against your roadmap:

| Phase | Status |
|---|---|
| 1 — Core application | **Done** (missing: remove/update — see below) |
| 2 — Resources & configuration | **Half done** — filtering on, but `App` never loads the file |
| 3 — Database (JDBC) | Not started |
| 4 — Profiles | Started (`dev` exists) — needs `test`, `prod`, and a fix |
| 5 — Testing (integration, tags, Failsafe) | Unit tests done; the rest not started |
| 6 — Packaging | **Done** (shade works) — one file to understand |
| 7 — Local repository | Not started |
| 8 — Multi-module | Not started |
| 9 — Advanced Maven | Not started |

## Two bugs you have *right now* — fix them first

**Bug 0.1 — the placeholder that never resolves.**
Your `src/main/resources/shop.properties` says:

```properties
shop.name=${shop.name}
version=${project.version}
```

and `shop.name` is defined **only inside the `dev` profile**. So run this:

```bash
mvn clean process-resources
cat target/classes/shop.properties
```

Without `-Pdev`, the output literally contains `shop.name=${shop.name}` —
Maven does **not** fail on an unknown property during filtering; it silently
leaves the placeholder text in the file. Your app would one day print
`Welcome to ${shop.name}` in production. This exact bug ships to real
production systems all the time.

**The fix:** always give filtered properties a *default* in the main
`<properties>` block, and let profiles *override* it:

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>17</maven.compiler.release>
    <junit.version>5.11.0</junit.version>
    <shop.name>My Bookshop</shop.name>   <!-- default for all builds -->
</properties>
```

Rule to remember: **profiles override, properties provide defaults.**

**Bug 0.2 — the same plugin declared twice.**
Your `<plugins>` block declares `exec-maven-plugin` **twice**: once with the
`<mainClass>` configuration, once with the `announce` execution. Maven merges
duplicate declarations, but the behavior is confusing and real projects treat
it as an error. One plugin = one `<plugin>` block; multiple jobs go in
multiple `<execution>` entries inside it. Also, your first guide told you to
delete the `announce` execution once the phase-binding idea clicked — do
that now. The merged, clean version:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <mainClass>com.example.App</mainClass>
    </configuration>
</plugin>
```

**Also do this today (30 seconds):** the project is still not a git
repository. Phases 3 and 8 restructure your whole project — you want an
undo button before that.

```bash
cd ~/Desktop/Projects/LearningMaven_Junit/bookshop
git init
printf 'target/\n.idea/\n*.iml\n' > .gitignore
git add -A && git commit -m "bookshop: end of phase 1-2"
```

**Checkpoint 0**

- [ ] `mvn clean process-resources && cat target/classes/shop.properties`
      shows a real shop name *without* `-Pdev`.
- [ ] Only one `exec-maven-plugin` block remains in the pom.
- [ ] `git log` shows your first commit.

---

# Phase 1 — Core Application (mostly done — one gap)

You have `Book`, `BookShop`, `App`, and tests. Your roadmap says "CRUD
operations" — you have **C**reate (`addBook`) and **R**ead (`findByAuthor`),
but no **U**pdate or **D**elete. Add the missing two, because Phase 3 (the
database) will need all four:

```java
// In BookShop.java
public boolean removeByTitle(String title) {
    return books.removeIf(b -> b.getTitle().equals(title));
}

public Optional<Book> findByTitle(String title) {
    return books.stream()
                .filter(b -> b.getTitle().equals(title))
                .findFirst();
}
```

**Why `Optional`?** A search can legitimately find nothing. Returning `null`
forces every caller to remember a null-check; forgetting one is the classic
`NullPointerException`. `Optional<Book>` makes "maybe absent" part of the
method's *signature* — the compiler reminds the caller. Use `Optional` for
return values of finder methods; never for fields or parameters.

**A Java note while you're in this file:** rename the field `Books` to
`books` in `BookShop.java`. In Java, capitalized names are for *types*,
lower-case for *variables*. Every Java developer reading `Books.add(b)`
stumbles for a second. Small thing, strong convention.

Write a test for each new method before moving on (you know JUnit now:
`assertTrue(shop.removeByTitle("Clean Code"))`, and for the Optional:
`assertTrue(shop.findByTitle("Nope").isEmpty())`).

**Checkpoint 1**

- [ ] `mvn test` runs at least 6 tests, all green.
- [ ] You can say out loud what C, R, U, D map to in `BookShop`.

---

# Phase 2 — Resources & Configuration (finish it)

## The concept: what "resources" really are

`src/main/resources` files are copied into `target/classes` during the
`process-resources` phase, which means they end up **inside your jar**, on
the *classpath* — the set of locations the JVM searches for classes and
files. That is the whole trick: your code can load them the same way whether
it runs from IntelliJ, from `mvn exec:java`, or from a fat jar on a server,
because in all three cases the file is "on the classpath".

**When to use resources:** any file your app needs at runtime that ships
*with* the app — config defaults, seed data, SQL scripts, templates.
**When not to:** files that differ per machine or contain secrets. Those
stay *outside* the jar (Phase 4 shows how profiles help).

## Exercise 2.1 — actually load `shop.properties` in `App`

Your file is filtered and copied — but no code reads it. Fix that:

```java
package com.example;

import com.example.Model.BookShop;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class App {
    public static void main(String[] args) throws IOException {
        Properties config = new Properties();
        try (InputStream in =
                     App.class.getResourceAsStream("/shop.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "shop.properties not found on classpath");
            }
            config.load(in);
        }

        System.out.println("Welcome to " + config.getProperty("shop.name")
                + " v" + config.getProperty("version"));

        BookShop bookShop = new BookShop();
        // ... your existing books ...
        System.out.println(bookShop.totalValue());
        System.out.println(bookShop.toJson());
    }
}
```

Three things to understand in that code, because you will write it in every
Java project for the rest of your life:

* **`getResourceAsStream("/shop.properties")`** — the leading `/` means
  "from the classpath root". Without it, Java looks *relative to the
  package* (`com/example/shop.properties`). Beginners lose hours to this
  slash. Root your paths.
* **`try (...)`** — try-with-resources. The stream is closed automatically
  even if loading throws. Anything that implements `AutoCloseable` (streams,
  and in Phase 3: database connections!) belongs in one of these.
* **The null check** — `getResourceAsStream` returns `null` (not an
  exception) when the file is missing. Fail loudly and early with a clear
  message; the alternative is a confusing `NullPointerException` two lines
  later.

Run it and watch the filtered value flow all the way through:

```bash
mvn compile exec:java          # Welcome to My Bookshop v1.0-SNAPSHOT
mvn compile exec:java -Pdev    # Welcome to My Dev Bookshop v1.0-SNAPSHOT
```

## Exercise 2.2 — seed data: `books.json`

Your shop hardcodes its books in `main`. Move them to a resource — this is
the "seed data" pattern, and it finally uses Gson in *both* directions:

`src/main/resources/books.json`:

```json
[
  { "title": "Clean Code",      "author": "Robert C. Martin", "price": 39.99 },
  { "title": "Effective Java",  "author": "Joshua Bloch",     "price": 45.50 },
  { "title": "Design Patterns", "author": "Erich Gamma",      "price": 55.00 }
]
```

Loading JSON into objects (deserialization) in `BookShop`:

```java
public void loadFrom(InputStream json) {
    Gson gson = new Gson();
    Book[] loaded = gson.fromJson(
        new InputStreamReader(json, StandardCharsets.UTF_8), Book[].class);
    books.addAll(Arrays.asList(loaded));
}
```

And in `App`, replace the three `addBook` calls:

```java
try (InputStream in = App.class.getResourceAsStream("/books.json")) {
    bookShop.loadFrom(in);
}
```

**One catch, worth understanding:** filtering is ON for *all* of
`src/main/resources` in your pom. If a JSON file ever contained the text
`${...}`, Maven would try to substitute it. The professional pattern is to
filter only what needs filtering:

```xml
<resources>
    <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
        <includes><include>*.properties</include></includes>
    </resource>
    <resource>
        <directory>src/main/resources</directory>
        <filtering>false</filtering>
        <excludes><exclude>*.properties</exclude></excludes>
    </resource>
</resources>
```

Read it as: "properties files get filtered; everything else is copied
untouched." **When to use filtering:** stamping build-time facts (version,
build date, profile name) into config. **When not to:** anything a user or
another program wrote — binary files especially can be corrupted by
filtering.

**Checkpoint 2**

- [ ] `mvn exec:java` greets you with the shop name and version.
- [ ] The books come from `books.json`, not from Java code.
- [ ] You can explain the difference between `src/main/resources` and
      `target/classes`, and *which phase* copies one to the other.

---

# Phase 3 — Database Integration (the big new skill)

Until now your books die when the JVM exits. A database makes them
*persistent*. This phase teaches JDBC (Java's low-level database API), the
DAO pattern, and two Maven concepts: **dependency scopes** and keeping
config out of code.

## 3.0 Concept map — the pieces and why each exists

```
App
 └── BookShop (business logic — knows NOTHING about SQL)
      └── BookDAO (interface — "a place books live")
            ├── InMemoryBookDAO   (a List — what you have today)
            └── JdbcBookDAO       (real Oracle database)
                  └── ConnectionFactory  (opens connections)
                        └── DatabaseConfig (reads database.properties)
```

**Why so many layers?** Each one answers one question:

* **DAO (Data Access Object)** — an *interface* that describes storage
  operations (`save`, `findByTitle`, `findAll`, `deleteByTitle`) without
  saying *how* they happen. Your `BookShop` depends only on this interface.
  Result: you can swap Oracle for in-memory (fast tests! Phase 5) without
  touching business logic. This is the single most transferable pattern in
  this whole guide — Spring's repositories, which you'll meet in your real
  project, are DAOs with superpowers.
* **ConnectionFactory** — one place that knows how to open a database
  connection, so the URL/user/password code isn't repeated in every method.
* **DatabaseConfig** — reads `database.properties` from the classpath
  (Phase 2 skill, reused!). Config lives in files, not in `.java` files,
  because config changes per environment but code shouldn't.

## 3.1 The driver dependency — and your first *scope* decision

A **JDBC driver** is a jar that translates Java's standard `Connection` /
`PreparedStatement` calls into one database's network protocol. A driver is
**database-specific**: an Oracle URL needs the Oracle driver, and no other.
Oracle's is called *OJDBC*:

```xml
<properties>
    <ojdbc.version>23.6.0.24.10</ojdbc.version>
</properties>

<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
    <version>${ojdbc.version}</version>
    <scope>runtime</scope>
</dependency>
```

**Reading the artifact name.** `ojdbc11` is not a version number — the
trailing digit is the **minimum Java version** the jar is compiled for:
`ojdbc11` needs Java 11+, `ojdbc8` needs Java 8. You are on Java 17
(`maven.compiler.release`), so `ojdbc11` is your choice. The `<version>`
(`23.6.0.24.10`) is the *database* release the driver ships with — and
Oracle drivers are backward/forward compatible across a wide range, so a
23.x driver talks happily to a 19c or 21c server. Pick the newest.

> **Trap — the wrong driver is silent until runtime.** If your pom declares
> `com.mysql:mysql-connector-j` but your URL starts with `jdbc:oracle:thin:`,
> **everything compiles fine**. You only find out when you run, and the
> message is the notoriously unhelpful
> `java.sql.SQLException: No suitable driver found for jdbc:oracle:thin:@...`.
> That error almost never means "bad password" — it means *no driver on the
> classpath recognised this URL prefix*. Check your dependency first.

**Why `<scope>runtime</scope>`?** Walk through what scope means with the
three dependencies your pom now has:

| Dependency | Scope | Compile classpath | Test classpath | Runtime / packaged |
|---|---|---|---|---|
| gson | compile (default) | yes | yes | yes |
| junit-jupiter | test | no | yes | no |
| ojdbc11 | runtime | **no** | yes | yes |

Your *code* never mentions an Oracle class — you write against
`java.sql.Connection`, an interface from the JDK. The driver is only needed
when the program *runs*. Declaring it `runtime` makes the compiler **stop
you** if you accidentally import `oracle.jdbc.*` — protecting the
swappability the DAO pattern just bought you. **Rule: drivers, logging
backends, anything you use only through an interface → `runtime`.**

*How does the driver get found, then, if you never name it?* Since JDBC 4.0,
`DriverManager` scans the classpath for jars carrying a
`META-INF/services/java.sql.Driver` file and registers what it finds. OJDBC
ships one. This is why the old `Class.forName("oracle.jdbc.OracleDriver")`
line you'll see in every tutorial from 2009 is **obsolete** — delete it on
sight.

Run `mvn dependency:tree` after adding it and find the driver in the output.

## 3.2 The schema — teach the database about books

`src/main/resources/schema.sql`:

```sql
CREATE TABLE books (
    id     NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    title  VARCHAR2(200) NOT NULL,
    author VARCHAR2(120) NOT NULL,
    price  NUMBER(8,2)   NOT NULL
);
```

Four Oracle-specific things in those five lines — none of them optional:

* **`VARCHAR2`, not `VARCHAR`.** Oracle accepts `VARCHAR` today but has
  reserved the right to change its meaning for thirty years. Every Oracle
  codebase writes `VARCHAR2`. Do the same.
* **`NUMBER(8,2)` for money** — Oracle's exact-decimal type (the equivalent
  of `DECIMAL` elsewhere). Never `BINARY_DOUBLE` for prices: binary floats
  cannot represent `0.10` exactly, which is precisely why your JUnit
  assertions needed a `0.001` delta. A database column should not need a
  delta.
* **`GENERATED BY DEFAULT AS IDENTITY`** replaces MySQL's `AUTO_INCREMENT`.
  Identity columns arrived in Oracle 12c; before that, every project hand-
  rolled a `SEQUENCE` plus a `BEFORE INSERT` trigger. If you inherit an
  older Oracle schema you *will* meet that pattern — now you'll recognise
  what it's emulating.
* **No `IF NOT EXISTS`** in Oracle before 23ai. Re-running this script on an
  existing table raises `ORA-00955: name is already used by an existing
  object`. On Oracle 23ai (Free) you *can* write
  `CREATE TABLE IF NOT EXISTS books (...)`; on 19c/21c the idiom is to drop
  first and ignore the error, or wrap the DDL in a PL/SQL block. Know which
  version you're targeting.

**A word on `id`.** Your `Book` class has no `id` field, and this guide does
not add one — the DAO looks books up by title. The identity column still
earns its place: it gives every row a stable primary key so the table has a
real identity of its own, independent of a title someone may want to edit
later.

### Running Oracle locally

Installing an Oracle server by hand is a genuinely unpleasant afternoon.
Don't. Oracle publishes **Oracle Database Free** (formerly XE) as a
container image, and `gvenzl/oracle-free` wraps it with the convenience
options you want:

```bash
docker run -d --name oracle-free -p 1521:1521 \
    -e ORACLE_PASSWORD=oracle \
    -e APP_USER=shopuser \
    -e APP_USER_PASSWORD=shoppass \
    gvenzl/oracle-free:23-slim
```

`APP_USER` / `APP_USER_PASSWORD` create your `shopuser` account
automatically, already granted what an application needs. **Be patient on
the first run:** an Oracle container takes 30–90 seconds to become usable
while it initialises the database — much slower than MySQL or Postgres.
Watch for the ready line:

```bash
docker logs -f oracle-free       # wait for "DATABASE IS READY TO USE!"
```

Then create the table by opening a SQL shell inside the container:

```bash
docker exec -it oracle-free sqlplus shopuser/shoppass@localhost/FREEPDB1
```

Paste the `CREATE TABLE` above (SQL\*Plus executes when it sees the `;`),
then `SELECT * FROM books;` to confirm it exists, and `exit`.

> **Schema ≠ database.** In MySQL you would `CREATE DATABASE bookshop`. In
> Oracle there is *one* database, and each **user owns a schema** — the
> namespace holding their tables. Creating the user `shopuser` *is* creating
> the schema; `shopuser`'s tables are simply `SHOPUSER.BOOKS`. This
> difference matters again in Phase 4, where "a separate test database"
> becomes "a separate test user."

## 3.3 Config: `database.properties` + `DatabaseConfig`

`src/main/resources/database.properties`:

```properties
db.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
db.user=shopuser
db.password=shoppass
```

### Decoding that URL — the #1 Oracle beginner trap

Oracle's JDBC URL is stranger than MySQL's, and it comes in **two forms
that look nearly identical but mean different things**:

```
Service-name form — use this one:

      jdbc:oracle:thin:@//localhost:1521/FREEPDB1
                       └┬┘└───┬───┘ └─┬┘ └───┬──┘
                        │     │       │      └─ service name
                        │     │       └─ listener port (default 1521)
                        │     └─ host
                        └─ "@//" introduces the service form

Legacy SID form:

      jdbc:oracle:thin:@localhost:1521:FREE
                       │              └─ ":" separator means this is a SID
                       └─ "@" alone introduces the SID form
```

Piece by piece:

* **`thin`** — the pure-Java driver, no native Oracle client needed. There
  is also an `oci` driver that requires an Oracle client installed on the
  machine. You want `thin`, always, for a Java app.
* **`@//host:port/SERVICE`** — the modern **service name** form. Note the
  double slash and the **`/`** before the service.
* **`@host:port:SID`** — the legacy **SID** form, with a **`:`**. A SID
  names one database *instance*; a service name is a logical alias that can
  point at several. Oracle has recommended service names since 8i.

For the container above, the values are: SID `FREE`, and two service names
— `FREE` (the container/CDB) and **`FREEPDB1`** (the pluggable database
where `shopuser`'s tables actually live). **Use `FREEPDB1`.** Connecting to
`FREE` succeeds and then reports that your table doesn't exist, which is a
maddening thing to debug. Modern Oracle is *multitenant*: the CDB is a
shell, your data lives in a PDB.

Two failures worth recognising immediately:

| Symptom | Cause |
|---|---|
| `ORA-12514: TNS:listener does not currently know of service requested` | wrong service name (e.g. `FREEPDB1` misspelled, or you used the SID form with a service name) |
| `ORA-12541: TNS:no listener` | nothing is listening on that host/port — container not started, still initialising, or wrong port |
| `ORA-00942: table or view does not exist` right after a *successful* connect | you connected to the CDB (`FREE`) instead of the PDB (`FREEPDB1`), or to the wrong user's schema |

### Exercise 3.3b — prove the connection works *before* writing a DAO

Do not debug a driver, a URL, a password, and your SQL all at once. Isolate.
Add a throwaway class and run it:

```java
package com.example.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class ConnectionCheck {
    public static void main(String[] args) throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        System.out.println("URL  = " + config.url());
        System.out.println("USER = " + config.user());

        try (Connection con = new ConnectionFactory(config).open()) {
            DatabaseMetaData md = con.getMetaData();
            System.out.println("Connected to : " + md.getDatabaseProductName()
                    + " " + md.getDatabaseProductVersion());
            System.out.println("Driver       : " + md.getDriverVersion());
            System.out.println("Valid        : " + con.isValid(5));
        }
    }
}
```

```bash
mvn compile exec:java -Dexec.mainClass=com.example.db.ConnectionCheck
```

Printing the URL first is not padding — it is the fastest way to catch a
filtering mistake, because a placeholder that never resolved shows up
literally as `URL = ${db.url}` instead of failing with a confusing driver
error. When this class prints an Oracle version string, *then* move on. If
it doesn't, the table above names your error.

An even smaller check, no Java at all — does anything answer on the port?

```bash
docker ps                       # is the container up?
nc -zv localhost 1521           # is the listener accepting connections?
```

Delete `ConnectionCheck` once the DAO works, or keep it — a one-command
connectivity probe is a genuinely useful thing to have in a project.

`DatabaseConfig.java` — Phase 2 code, new file:

```java
package com.example.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {
    private final Properties props = new Properties();

    public DatabaseConfig() {
        try (InputStream in = DatabaseConfig.class
                .getResourceAsStream("/database.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                    "database.properties not found on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Could not read database.properties", e);
        }
    }

    public String url()      { return props.getProperty("db.url"); }
    public String user()     { return props.getProperty("db.user"); }
    public String password() { return props.getProperty("db.password"); }
}
```

`ConnectionFactory.java`:

```java
package com.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionFactory {
    private final DatabaseConfig config;

    public ConnectionFactory(DatabaseConfig config) {
        this.config = config;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(
            config.url(), config.user(), config.password());
    }
}
```

**Why a factory instead of calling `DriverManager` everywhere?** One reason
now, one later. Now: the credentials wiring lives in exactly one place.
Later: when you meet *connection pools* (HikariCP, and inside Spring), you
will swap this class's internals and nothing else changes. A factory is a
seam — a place designed for future change.

## 3.4 The DAO — interface first, then two implementations

`BookDAO.java` — the contract:

```java
package com.example;

import com.example.Model.Book;

import java.util.List;
import java.util.Optional;

public interface BookDAO {
  void save(Book book);                       // C

  List<Book> findAll();                       // R

  Optional<Book> findByTitle(String title);   // R

  List<Book> findByAuthor(String author);     // R

  boolean updatePrice(String title, double newPrice); // U

  boolean deleteByTitle(String title);        // D
}
```

`InMemoryBookDAO.java` — your current `ArrayList` logic moves here almost
unchanged. Ten minutes of cut-and-paste; it will become your *test* DAO.

`JdbcBookDAO.java` — the real one. Here are three of the six methods; the
pattern repeats, so write the others yourself:

```java
package com.example.db;

import com.example.Model.Book;
import com.example.BookDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcBookDAO implements BookDAO {
  private final ConnectionFactory factory;

  public JdbcBookDAO(ConnectionFactory factory) {
    this.factory = factory;
  }

  @Override
  public void save(Book book) {
    String sql = "INSERT INTO books (title, author, price) VALUES (?, ?, ?)";
    try (Connection con = factory.open();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, book.getTitle());
      ps.setString(2, book.getAuthor());
      ps.setDouble(3, book.getPrice());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("save failed for " + book.getTitle(), e);
    }
  }

  @Override
  public List<Book> findAll() {
    String sql = "SELECT title, author, price FROM books";
    List<Book> result = new ArrayList<>();
    try (Connection con = factory.open();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        result.add(new Book(rs.getString("title"),
                rs.getString("author"),
                rs.getDouble("price")));
      }
    } catch (SQLException e) {
      throw new RuntimeException("findAll failed", e);
    }
    return result;
  }

  @Override
  public boolean deleteByTitle(String title) {
    String sql = "DELETE FROM books WHERE title = ?";
    try (Connection con = factory.open();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, title);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("delete failed for " + title, e);
    }
  }
}
```

Concepts inside that code — each one is a habit, not a detail:

* **`PreparedStatement` with `?` placeholders — always.** Never build SQL
  by concatenating strings (`"WHERE title = '" + title + "'"`). A title
  like `O'Reilly's Guide` breaks it, and a *malicious* title deletes your
  table — SQL injection, the #1 web vulnerability for two decades. The
  `?` mechanism sends values separately from the SQL, making injection
  impossible.
* **try-with-resources on `Connection`, `PreparedStatement`, `ResultSet`.**
  Database connections are scarce, and on Oracle they are *expensive*: each
  session costs real server memory, and the instance enforces a hard
  `sessions` limit (Oracle Free ships with a low one). Leak connections and
  you eventually get `ORA-00018: maximum number of sessions exceeded` —
  which looks like a server problem and is always a client bug. Same
  `try (...)` you learned in Phase 2; this is why it exists.
* **`executeUpdate()` returns the affected-row count** — that's how
  `deleteByTitle` knows whether the book existed. `executeQuery()` returns
  a `ResultSet` you walk with `rs.next()`.
* **Wrapping `SQLException` in `RuntimeException`.** `SQLException` is
  *checked*: without wrapping, every caller up the chain must declare
  `throws SQLException` — spreading JDBC knowledge into code that shouldn't
  know the database exists. Wrap at the boundary. (Later you can define
  `class DataAccessException extends RuntimeException` — Spring does exactly
  this.)

## 3.5 Rewire `BookShop` to use the DAO

`BookShop` stops owning a list and starts *delegating*:

```java
public class BookShop {
    private final BookDAO dao;

    public BookShop(BookDAO dao) {          // dependency injection — by hand
        this.dao = dao;
    }

    public void addBook(Book b) { dao.save(b); }

    public double totalValue() {
        return dao.findAll().stream().mapToDouble(Book::getPrice).sum();
    }

    public List<Book> findByAuthor(String a) { return dao.findByAuthor(a); }

    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create()
                                .toJson(dao.findAll());
    }
}
```

Passing the DAO in through the constructor is called **dependency
injection** — the class doesn't create its collaborator, it *receives* it.
You are doing manually what the Spring framework automates. Feeling this
pain (the wiring in `App` below) is the best preparation for appreciating
Spring later.

`App.java` becomes the *composition root* — the one place that decides
which implementation runs:

```java
DatabaseConfig config = new DatabaseConfig();
ConnectionFactory factory = new ConnectionFactory(config);
BookDAO dao = new JdbcBookDAO(factory);      // ← the ONLY line that
BookShop bookShop = new BookShop(dao);       //   knows Oracle is involved
```

Swap that one line to `new InMemoryBookDAO()` and the whole app runs
without a database. That flexibility is what Phases 4 and 5 will exploit.

**Checkpoint 3**

- [ ] `ConnectionCheck` prints a real Oracle version string.
- [ ] `mvn exec:java` inserts books and prints them back **from Oracle**
      (run it twice — books persist between runs now; you may want an
      `if empty then seed` guard).
- [ ] This shows your rows:

      ```bash
      docker exec -i oracle-free sqlplus -S shopuser/shoppass@localhost/FREEPDB1 <<< "SELECT * FROM books;"
      ```

- [ ] You can explain: why the driver is `runtime` scope, why `?`
      placeholders, why the DAO is an interface, and the difference between
      a SID and a service name.
- [ ] `git commit` — this was a big phase.

---

# Phase 4 — Profiles (one build, many environments)

## The concept

Your app now has config (`database.properties`) that **must** differ
between your laptop (`dev`), the test environment (`test`), and the real
server (`prod`) — different URLs, users, passwords. A **profile** is a
named, optional slice of pom that activates on demand and typically swaps
property values. Combined with resource filtering (Phase 2), one `mvn
package -Pprod` produces a prod-configured jar.

**When to use profiles:** environment differences (URLs, credentials,
log levels), optional build steps (skip slow checks in dev).
**When NOT to:** producing *functionally different* apps — that's a code
smell; the same jar should ideally run anywhere, only *config* changing.
(In your future real project, Spring's `application-{profile}.properties`
does this at *runtime* — same idea, later binding.)

## Exercise 4.1 — parameterize `database.properties`

Replace hardcoded values with placeholders:

```properties
db.url=${db.url}
db.user=${db.user}
db.password=${db.password}
```

Defaults in the main `<properties>` (remember Bug 0.1 — defaults always):

```xml
<properties>
    ...
    <db.url>jdbc:oracle:thin:@//localhost:1521/FREEPDB1</db.url>
    <db.user>shopuser</db.user>
    <db.password>shoppass</db.password>
</properties>
```

And three profiles:

```xml
<profiles>
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <shop.name>My Dev Bookshop</shop.name>
        </properties>
    </profile>

    <profile>
        <id>test</id>
        <properties>
            <shop.name>Bookshop TEST</shop.name>
            <!-- same URL — a different SCHEMA, not a different database -->
            <db.user>shoptest</db.user>
            <db.password>shoppass</db.password>
        </properties>
    </profile>

    <profile>
        <id>prod</id>
        <properties>
            <shop.name>The Bookshop</shop.name>
            <db.url>jdbc:oracle:thin:@//prod-server:1521/BOOKPDB</db.url>
            <db.user>prod_user</db.user>
            <!-- no password here! see 4.3 -->
        </properties>
    </profile>
</profiles>
```

**Notice what changed between `dev` and `test`: the *user*, not the URL.**
This is the schema-vs-database distinction from Phase 3 showing up in your
build config. On MySQL you would point at `bookshop_test`; on Oracle you
connect to the same PDB as a different user, and get that user's schema.
Same isolation, different mechanism — and a thing interviewers ask about.

Create the test user so `-Ptest` actually works. Connect as the admin
account (`system`, password `oracle` from your `docker run`):

```bash
docker exec -it oracle-free sqlplus system/oracle@localhost/FREEPDB1
```

```sql
CREATE USER shoptest IDENTIFIED BY shoppass;
GRANT CONNECT, RESOURCE TO shoptest;
ALTER USER shoptest QUOTA UNLIMITED ON USERS;
```

Then reconnect as `shoptest` and run your `CREATE TABLE books` again — the
new schema starts empty. (`CONNECT` allows logging in, `RESOURCE` allows
creating tables, and the `QUOTA` grant is the one people forget: without it
the user may create a table but every `INSERT` fails with `ORA-01950: no
privileges on tablespace 'USERS'`.)

## Exercise 4.2 — see it work, three ways

```bash
mvn help:active-profiles              # which profiles are on right now?
mvn clean process-resources -Ptest
cat target/classes/database.properties   # → db.user=shoptest

mvn clean process-resources -Pprod
cat target/classes/database.properties   # → prod-server URL
```

Things to notice:

* `-P` selects profiles; several can combine: `-Ptest,prod` (last property
  definition wins — try it and check which URL survives).
* `activeByDefault` on `dev` turns off the moment you pass **any** `-P` —
  a classic surprise. `help:active-profiles` is how you check reality
  instead of guessing.
* Profiles can also activate automatically — by OS, JDK, or a property
  (`mvn ... -Denv=ci`). File that away; explicit `-P` is clearer while
  learning.

## Exercise 4.3 — the credentials rule

Notice `prod` sets **no password**. Real credentials never go in the pom —
the pom is committed to git, and git never forgets. The standard escape
hatches, in the order you'll meet them in real life:

1. **`~/.m2/settings.xml`** — a per-machine, never-committed file that can
   also define profiles and properties. The build server has one with prod
   values; your laptop doesn't.
2. **Environment variables** — `${env.DB_PASSWORD}` in the pom reads the
   `DB_PASSWORD` env var. This is what your GitHub Actions build (Phase 9)
   will use, fed from repository *secrets*.

Try option 2 right now so it's not abstract: set
`<db.password>${env.DB_PASSWORD}</db.password>` in the prod profile, run
`DB_PASSWORD=supersecret mvn process-resources -Pprod`, and cat the result.

**Checkpoint 4**

- [ ] Three profiles exist; `mvn help:active-profiles` shows `dev` by
      default.
- [ ] `target/classes/database.properties` provably differs between
      `-Ptest` and `-Pprod`.
- [ ] You can explain why passwords don't live in the pom, and name two
      places they *can* live.

---

# Phase 5 — Testing, Level 2 (unit vs integration)

## The concept: two kinds of tests, two plugins

Everything you've written so far is a **unit test**: fast, isolated, no
network, no database — pure Java in, assertion out. A test that talks to a
real Oracle instance is an **integration test**: slower, needs
infrastructure, can fail for reasons that aren't bugs (container not
started, wrong service name, expired password). Mixing the two in one bucket
ruins both — so Maven ships two test plugins:

| | Surefire | Failsafe |
|---|---|---|
| Runs in phase | `test` | `integration-test` + `verify` |
| File pattern | `*Test.java` | `*IT.java` |
| Meant for | unit tests | integration tests |
| Fails the build | immediately | in `verify`, *after* cleanup |

The odd-sounding name "Failsafe" is the point: it does **not** fail the
build during `integration-test`, so the `post-integration-test` phase can
still tear down containers/servers; the failure is reported in `verify`.

## Exercise 5.1 — your DAO makes unit testing easy (this is the payoff)

`BookShopTest` should no longer touch Oracle — inject the in-memory DAO:

```java
@BeforeEach
void setUp() {
    bookShop = new BookShop(new InMemoryBookDAO());
    bookShop.addBook(new Book("Clean Code", "Robert C. Martin", 39.99));
    // ...
}
```

Same assertions as before, still milliseconds-fast, zero infrastructure.
*This* is why the DAO interface exists. If you had baked JDBC directly into
`BookShop`, every test would need a database.

## Exercise 5.2 — a real integration test

`src/test/java/com/example/db/JdbcBookDAOIT.java` — note the **IT** suffix:

```java
package com.example.db;

import com.example.Model.Book;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class JdbcBookDAOIT {

  private ConnectionFactory factory;
  private JdbcBookDAO dao;

  @BeforeEach
  void setUp() throws SQLException {
    factory = new ConnectionFactory(new DatabaseConfig());
    dao = new JdbcBookDAO(factory);
    try (Connection con = factory.open()) {
      con.createStatement().execute("DELETE FROM books");
    }
  }

  @Test
  void savedBookCanBeFoundAgain() {
    dao.save(new Book("Refactoring", "Martin Fowler", 47.00));

    var found = dao.findByTitle("Refactoring");

    assertTrue(found.isPresent());
    assertEquals("Martin Fowler", found.get().getAuthor());
  }

  @Test
  void deleteReturnsFalseWhenBookAbsent() {
    assertFalse(dao.deleteByTitle("Ghost Book"));
  }
}
```

New ideas in there:

* **`@Tag("integration")`** — a JUnit 5 label. Plugins (and IDEs) can
  include/exclude by tag: `mvn test -Dgroups=integration` runs only tagged
  tests; `-DexcludedGroups=integration` skips them. Tags are how big teams
  slice suites: `slow`, `smoke`, `requires-network`.
* **Cleaning the table in `@BeforeEach`** — integration tests must not
  depend on leftover state from a previous run or from each other. Every
  test starts from a known world. (This is also why they run as `shoptest`,
  never against your real schema — wire your `test` profile in.)
* **Oracle does not auto-commit `DELETE`… or does it?** JDBC opens
  connections in auto-commit mode, so the cleanup above commits and you can
  ignore transactions for now. The moment you turn auto-commit off — which
  real applications do — remember that Oracle *never* implicitly commits
  DML, and an uncommitted `DELETE` will block another session's write until
  you commit or roll back. That is the most common "my integration test
  hangs forever" cause on Oracle.
* **Test naming as documentation** — `deleteReturnsFalseWhenBookAbsent`
  tells you the contract without opening `JdbcBookDAO`.

## Exercise 5.3 — wire Failsafe

Surefire's pattern (`*Test.java`) already ignores `JdbcBookDAOIT` — verify
that: `mvn test` should not run it. Then add Failsafe:

```xml
<plugin>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Now observe the split in action:

```bash
mvn test      # unit tests only — works with Oracle stopped!
mvn verify    # unit tests, THEN package, THEN integration tests
```

Try it with Oracle stopped (`docker stop oracle-free`): `mvn test` stays
green, `mvn verify` fails with `ORA-12541: TNS:no listener` — exactly the
separation you want. A teammate without an Oracle container can still
build; CI runs the full `verify`. Start it again with
`docker start oracle-free` (much faster than the first run — the database
is already initialised).

**Checkpoint 5**

- [ ] `mvn test` runs only unit tests (no DB needed).
- [ ] `mvn verify` runs `JdbcBookDAOIT` and its report lands in
      `target/failsafe-reports/`.
- [ ] You can explain the `*Test` vs `*IT` convention and what `@Tag` adds
      beyond it.

---

# Phase 6 — Packaging (you've done it — now *understand* it)

You already ship a working fat jar. This phase is short: three
understandings to lock in, using tools, not faith.

## 6.1 — Look inside your jars

```bash
mvn clean package
ls -lh target/*.jar
jar tf target/bookshop-1.0-SNAPSHOT.jar | head -30
jar tf target/bookshop-1.0-SNAPSHOT.jar | grep -c gson
```

`jar tf` lists a jar's contents (it's just a zip). You should see your
classes, your resources (`shop.properties`, `books.json` — Phase 2 promised
they'd be in here!), **and** `com/google/gson/...` classes — physical proof
of what "fat jar" means. Extract the manifest too:

```bash
unzip -p target/bookshop-1.0-SNAPSHOT.jar META-INF/MANIFEST.MF
```

`Main-Class: com.example.App` is the line your jar-plugin `<archive>`
config wrote — it's how `java -jar` knows where `main` is.

## 6.2 — The mystery file: `dependency-reduced-pom.xml`

That file sitting in your project root was generated by the **shade
plugin**. Why: if someone used your fat jar *as a dependency*, they'd get
Gson twice — once bundled inside your jar, once as your declared
transitive dependency. So shade writes an alternate pom with the bundled
dependencies *removed*, and installs that one. Harmless — but it belongs in
`.gitignore` (add it), and knowing what generated files are for beats being
puzzled by them.

## 6.3 — Fat jar vs thin jar: when each

* **Fat (shaded) jar** — one file, contains everything, `java -jar` and
  done. Use for: CLI tools, simple services, anything you copy to a server.
  Your bookshop: correct choice.
* **Thin jar + dependencies elsewhere** — the jar-plugin default. Use for:
  libraries (consumers bring their own dependencies — shading a *library*
  causes version conflicts for its users), and app servers that provide
  dependencies.
* In your real project: Spring Boot has its own `spring-boot-maven-plugin`
  doing a smarter fat jar (nested jars, not merged). Same problem, same
  idea, different tool.

**Checkpoint 6**

- [ ] You located Gson's classes inside your fat jar with `jar tf`.
- [ ] You can say what `dependency-reduced-pom.xml` is for.
- [ ] `java -jar target/bookshop-1.0-SNAPSHOT.jar` runs the full app
      (DB and all) from a single file.

---

# Phase 7 — The Local Repository (become your own dependency)

## The concept

Every dependency you've ever used was downloaded into
`~/.m2/repository`, laid out as `groupId/artifactId/version/`. **`mvn
install`** copies *your* artifact into that same structure. From that
moment, `com.example:bookshop:1.0-SNAPSHOT` is a coordinate any project on
your machine can declare — exactly like Gson. This is the moment Maven's
"everything is an artifact with coordinates" worldview clicks.

## Exercise 7.1 — install and inspect

```bash
mvn clean install
ls ~/.m2/repository/com/example/bookshop/1.0-SNAPSHOT/
```

You'll find the jar, the pom, and bookkeeping files. Note the lifecycle
logic of what just happened: `install` sits *after* `verify`, so your
integration tests ran first — Maven refuses to install an artifact that
fails its quality gates. That ordering *is* the lifecycle lesson.

## Exercise 7.2 — consume your own artifact

Make a scratch project *next to* bookshop (not inside!):

```bash
cd ~/Desktop/Projects/LearningMaven_Junit
mvn archetype:generate -DgroupId=com.example -DartifactId=shop-client \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DarchetypeVersion=1.5 -DinteractiveMode=false
```

In `shop-client/pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>bookshop</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

In its `App.java`:

```java
BookShop shop = new BookShop(new InMemoryBookDAO());
shop.addBook(new Book("Domain-Driven Design", "Eric Evans", 62.00));
System.out.println(shop.toJson());
```

`mvn -f shop-client/pom.xml compile exec:java` — your bookshop classes,
resolved from `~/.m2` by coordinates. Also run `mvn dependency:tree` in
shop-client: Gson appears *transitively* through bookshop, because
bookshop's pom declares it. You are now on the producer side of dependency
management.

**One SNAPSHOT gotcha to experience:** change something visible in
bookshop's `toJson`, run `mvn install` in bookshop, rebuild shop-client —
the change appears. That's the meaning of `-SNAPSHOT`: "re-check for newer
builds of this version." A release version (`1.0`) is immutable — cached
forever. Phase 9 returns to this.

**Checkpoint 7**

- [ ] `shop-client` compiles against bookshop from the local repo.
- [ ] `dependency:tree` in shop-client shows gson under bookshop.
- [ ] You can explain SNAPSHOT vs release in one sentence.

---

# Phase 8 — Multi-Module (the graduation project)

## The concept — and an honest "when to use"

A **multi-module** (a.k.a. *reactor*) build is one parent pom orchestrating
several sub-projects that build together, in dependency order, versioned as
one. **Why teams do it:** enforced boundaries — code in `core` physically
*cannot* call JDBC if the JDBC dependency only exists in the `database`
module; the compiler enforces your architecture. Also: shared version/
dependency management in one place, and reusable pieces (another team can
depend on `bookshop-core` without dragging the 10 MB Oracle driver along).

**Honest counterpoint:** for a program this size, a single module is
objectively fine. You are doing this to *learn the mechanics* — which is a
great reason, because nearly every real Java codebase you'll join is
multi-module, and "how do the poms relate?" will be your first-week
question there.

## Target structure

```
bookshop-parent/
├── pom.xml                  packaging=pom — the orchestra conductor
├── bookshop-core/
│   ├── pom.xml
│   └── src/…                Book, BookDAO, InMemoryBookDAO, BookShop,
│                            unit tests   (NO Gson, NO JDBC here)
├── bookshop-database/
│   ├── pom.xml              depends on: bookshop-core
│   └── src/…                DatabaseConfig, ConnectionFactory,
│                            JdbcBookDAO, database.properties, schema.sql,
│                            JdbcBookDAOIT
└── bookshop-app/
    ├── pom.xml              depends on: bookshop-core, bookshop-database
    └── src/…                App, shop.properties, books.json, shade plugin
```

Design decision worth pausing on: **where does Gson go?** `toJson` lives
in `BookShop` (core) today, which would drag Gson into core. Purists keep
core dependency-free: move JSON work to the app module (a small
`JsonPrinter` class). Do that — feeling *why* ("core stays clean") is the
architectural lesson of the whole phase.

## 8.1 — The parent pom

`bookshop-parent/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>bookshop-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>        <!-- ← builds no jar itself -->

    <modules>
        <module>bookshop-core</module>
        <module>bookshop-database</module>
        <module>bookshop-app</module>
    </modules>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.release>17</maven.compiler.release>
        <junit.version>5.11.0</junit.version>
        <gson.version>2.11.0</gson.version>
        <ojdbc.version>23.6.0.24.10</ojdbc.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.junit</groupId>
                <artifactId>junit-bom</artifactId>
                <version>${junit.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.google.code.gson</groupId>
                <artifactId>gson</artifactId>
                <version>${gson.version}</version>
            </dependency>
            <dependency>
                <groupId>com.oracle.database.jdbc</groupId>
                <artifactId>ojdbc11</artifactId>
                <version>${ojdbc.version}</version>
            </dependency>
            <!-- modules can depend on each other version-free too -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>bookshop-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>bookshop-database</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <!-- your existing pluginManagement block moves here,
                 plus failsafe 3.3.0 -->
        </pluginManagement>
    </build>
</project>
```

Your original pom's management blocks **finally move to where they always
belonged** — this is what `dependencyManagement` was *for*. The pattern:
**parent = versions and policy; child = "I use this", version-free.**

## 8.2 — A child pom

`bookshop-core/pom.xml` — notice everything that *isn't* here:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>bookshop-parent</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>bookshop-core</artifactId>   <!-- groupId & version inherited -->

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-params</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

`bookshop-database/pom.xml` adds:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>bookshop-core</artifactId>   <!-- inter-module dependency -->
</dependency>
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
    <scope>runtime</scope>
</dependency>
```

`bookshop-app/pom.xml` adds core + database + gson, plus the exec, jar
(`mainClass`), and shade plugin config from your current pom.

## 8.3 — Migration plan (do it in this order)

1. `git commit` the working single-module state. Seriously.
2. Create `bookshop-parent/` with the parent pom and three empty module
   folders each holding a child pom and `src/main/java`, `src/test/java`.
3. Move files: `Book`, `BookDAO`, `InMemoryBookDAO`, `BookShop`,
   `BookShopTest` → core. `db/*` classes, `database.properties`,
   `schema.sql`, `JdbcBookDAOIT` → database. `App`, `shop.properties`,
   `books.json` → app. Packages stay the same; only the module (and thus
   the jar) changes.
4. Move the `<profiles>` block to the parent (profiles inherit).
5. From `bookshop-parent/`: `mvn clean verify`, and read the **reactor
   summary**:

```
[INFO] Reactor Summary for bookshop-parent 1.0-SNAPSHOT:
[INFO] bookshop-parent .................... SUCCESS
[INFO] bookshop-core ...................... SUCCESS
[INFO] bookshop-database .................. SUCCESS
[INFO] bookshop-app ....................... SUCCESS
```

Maven computed that order from the dependencies — you never told it.
That's the **reactor**. Useful flags once this works:

```bash
mvn -pl bookshop-core test          # build just one module
mvn -pl bookshop-app -am package    # ...and (-am) whatever it needs first
```

6. Prove the boundary is real: add
   `import java.sql.Connection;`-using code to a core class → still
   compiles (JDK class), but try using `JdbcBookDAO` from core → **compile
   error**, core doesn't see the database module. Your architecture is now
   compiler-enforced. Remove the experiment.

**Checkpoint 8**

- [ ] `mvn clean verify` from the parent: 4× SUCCESS in reactor order.
- [ ] `java -jar bookshop-app/target/bookshop-app-1.0-SNAPSHOT.jar` works.
- [ ] Child poms contain **zero** version numbers for managed deps.
- [ ] You can explain reactor order and what `-pl`/`-am` do.

---

# Phase 9 — Advanced Maven (the professional toolkit)

Each item here is a "know what it is + do it once in bookshop" topic.

## 9.1 Maven Wrapper (`mvnw`)

**Problem:** teammate has Maven 3.6, you have 3.9 — "works on my machine."
**Fix:** commit a tiny script that downloads and uses a *pinned* Maven:

```bash
cd bookshop-parent
mvn wrapper:wrapper -Dmaven=3.9.9
./mvnw clean verify        # identical build for every machine & CI
```

From now on type `./mvnw` instead of `mvn` in this project — and in your
real project, *always* set up the wrapper on day one. (Spring Boot
generators include it automatically. Now you know what that file is.)

## 9.2 `dependency:tree` and exclusions

```bash
mvn dependency:tree
```

Read the tree: gson and ojdbc11 are *direct*; things indented under them
are *transitive* — dependencies of your dependencies, resolved
automatically. Oracle's driver is a good specimen to read: depending on the
artifact you pick it can pull in `oraclepki`, `osdt_core`, `osdt_cert` (the
wallet/TLS support) and more. If you never use Oracle Wallet you are
shipping megabytes for nothing — which is exactly what `dependency:analyze`
and exclusions, below, are for.

Two related tools:

* **Conflicts.** When two paths bring different versions of one library,
  Maven picks the *nearest* declaration ("nearest-wins") — not the newest!
  `mvn dependency:tree -Dverbose` shows what was omitted and why. In real
  projects this explains 90% of `NoSuchMethodError` at runtime.
* **Exclusions.** When a dependency drags in something you don't want
  (conflicting logging framework is the classic):

```xml
<dependency>
    <groupId>some.lib</groupId>
    <artifactId>some-lib</artifactId>
    <exclusions>
        <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

* Also run `mvn dependency:analyze` on each module — it flags *used but
  undeclared* dependencies (you're borrowing a transitive; declare it) and
  *declared but unused* ones (delete them).

## 9.3 Optional dependencies

`<optional>true</optional>` on a dependency means: "I use this, but
projects depending on *me* don't automatically get it." A library that can
integrate with Redis *if present* marks its Redis client optional; users
who want that feature declare it themselves. **When you'll care:** the day
bookshop-core is a shared library. Until then, just recognize it in other
people's poms.

## 9.4 BOM — you already use one

Your `junit-bom` import is a **Bill of Materials**: a pom whose whole job
is a `dependencyManagement` list of versions that are tested together.
Importing it (`<type>pom</type><scope>import</scope>`) is why your JUnit
dependencies need no `<version>`. Where you'll meet BOMs again:
`spring-boot-dependencies` manages *hundreds* of libraries this way — the
reason Spring Boot poms look so bare. You could even write a
`bookshop-bom` if outside teams consumed your modules — same mechanism.

## 9.5 SNAPSHOT vs Release, `install` vs `deploy`

The full mental model in four lines:

* `1.0-SNAPSHOT` = "moving development version" — repositories re-check
  for updates; every `install` overwrites.
* `1.0` (release) = immutable, forever. Re-releasing a changed `1.0` is a
  cardinal sin — caches worldwide would disagree about its contents.
* `install` = publish to **your machine** (`~/.m2`).
* `deploy` = publish to a **shared remote repository** so teammates and CI
  get it. Needs a `<distributionManagement>` section + credentials in
  `settings.xml`.

The release flow you'll see at work: develop on `1.0-SNAPSHOT` → release
day: re-version to `1.0`, build, deploy, tag in git → immediately bump to
`1.1-SNAPSHOT`. (The `maven-release-plugin` or the `versions-maven-plugin`
automate the re-versioning: try `mvn versions:set -DnewVersion=1.1-SNAPSHOT`
in bookshop-parent and watch it update every module pom in one command.)

## 9.6 Repository managers (Nexus / Artifactory)

Where does `deploy` deploy *to*? Companies run their own repository server
— Sonatype **Nexus** or JFrog **Artifactory**. It plays three roles:
(1) hosts the company's private artifacts, (2) *proxies + caches* Maven
Central so 200 developers don't each download the internet, (3) becomes
the single choke-point for security scanning. In your real project you'll
likely find a `<repositories>` or `settings.xml` mirror section pointing
at one — now you'll know what you're looking at. Nothing to do in
bookshop; recognizing it is the skill. (If curious: Nexus runs locally in
Docker in ten minutes — a good weekend experiment, not a day-3 task.)

## 9.7 Archetypes

You've used one twice (`maven-archetype-quickstart`) — an **archetype** is
just a project template. Real teams sometimes maintain their own ("our
standard microservice layout"). Know the term, move on; modern Spring
projects usually start from start.spring.io instead.

## 9.8 CI/CD with GitHub Actions

**The concept:** Continuous Integration = a neutral machine builds and
tests *every push*, so "works on my machine" stops being an argument. This
is where everything in this guide converges — wrapper (9.1), `verify`
lifecycle (Phase 5), profiles (Phase 4).

Push your repo to GitHub, then create
`.github/workflows/build.yml`:

```yaml
name: build
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest

    services:
      oracle:
        image: gvenzl/oracle-free:23-slim
        env:
          ORACLE_PASSWORD: oracle
          APP_USER: shoptest
          APP_USER_PASSWORD: shoppass
        ports: ["1521:1521"]
        options: >-
          --health-cmd="healthcheck.sh"
          --health-interval=20s --health-timeout=10s --health-retries=20

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven
      - name: Create schema
        run: |
          docker exec -i "$(docker ps -qf ancestor=gvenzl/oracle-free:23-slim)" \
            sqlplus -S shoptest/shoppass@localhost/FREEPDB1 \
            < src/main/resources/schema.sql
      - name: Build & run all tests
        run: ./mvnw -B clean verify -Ptest
```

Read the file top to bottom and connect each line to something you built:
the Oracle **service container** exists because your Failsafe tests need a
database; `APP_USER` recreates the `shoptest` schema you made by hand in
Phase 4; the `-Ptest` profile points the build at that user; `verify` runs
the *whole* quality gate; `cache: maven` is `~/.m2` persisted between runs;
`-B` (batch mode) silences the download progress spam in logs.

Three Oracle-in-CI realities worth knowing before they cost you an evening:

* **The image is big and slow.** Expect a couple of minutes before the
  database is usable — hence `--health-retries=20` with a 20-second
  interval rather than the snappier values a MySQL service gets. Too few
  retries and your job fails with `ORA-12541` on a database that was merely
  still booting.
* **The schema must be created explicitly.** MySQL's `MYSQL_DATABASE` gives
  you a ready database; on Oracle the user exists but the `books` table
  does not, so CI runs `schema.sql` itself. Good discipline anyway — your
  schema is now version-controlled and applied identically everywhere.
* **Licensing.** Oracle Database Free is genuinely free to use and
  redistribute, which is why this workflow is legal. Full Oracle Database
  images are not — do not casually swap the image for `enterprise` in a
  company repo.

When the green check appears on your commit — that's CI, working.

**Checkpoint 9**

- [ ] `./mvnw -v` works on a fresh terminal (wrapper committed).
- [ ] You ran `dependency:tree -Dverbose` and found at least one
      version conflict resolution (or proved there are none).
- [ ] You can define, in one sentence each: BOM, SNAPSHOT, deploy,
      Nexus, archetype.
- [ ] A GitHub Actions run of your project is green.

---

# Graduation — Mapping Bookshop to Your Real Project

Every skill you just built maps directly onto a modern (likely Spring)
codebase. When you open your real project, this is the translation table:

| You built by hand | Real project equivalent |
|---|---|
| `getResourceAsStream` + `Properties` | `application.properties` auto-loaded |
| Constructor-injecting `BookDAO` | `@Autowired` / constructor injection — Spring wires it |
| `BookDAO` interface + JDBC impl | Spring Data `Repository` — impl generated for you |
| `ConnectionFactory` | HikariCP connection pool, auto-configured |
| Maven `-P` profiles + filtering | Spring runtime profiles (`application-prod.properties`) |
| Shade fat jar | `spring-boot-maven-plugin` repackaged jar |
| Parent pom + BOM | `spring-boot-starter-parent` / `spring-boot-dependencies` |
| `*IT` + Failsafe + Oracle service | `@SpringBootTest` + Testcontainers (`OracleContainer`) |
| GitHub Actions `verify` | the same, verbatim |

You will recognize *all* of it — because you've now felt the problem each
piece solves. That was the point of building it the hard way first.

## The final master checklist

- [ ] Bugs 0.1 and 0.2 fixed; project in git.
- [ ] Full CRUD with `Optional` finders, all unit-tested.
- [ ] Config and seed data load from the classpath.
- [ ] JDBC DAO with PreparedStatements and try-with-resources; Oracle
      persists your books.
- [ ] dev / test / prod profiles pointing at separate Oracle *schemas*; no
      secret in any committed file.
- [ ] `mvn test` = fast and DB-free; `mvn verify` = the full gate.
- [ ] One fat jar runs the whole app.
- [ ] Three modules, reactor-ordered, versions only in the parent.
- [ ] Wrapper committed, CI green.

When every box is checked, you haven't "done a tutorial" — you have built,
tested, packaged, restructured, and continuously integrated a database-
backed Java application with Maven, from an empty folder. That *is* the
skill. Go start the real project.

---

# Appendix — Command Cheat Sheet (new commands from this guide)

| Command | What it does |
|---|---|
| `mvn clean process-resources` | run filtering only; inspect `target/classes` |
| `mvn help:active-profiles` | show which profiles are on |
| `mvn package -Pprod` | build with a profile |
| `mvn test -Dgroups=integration` | run only tests tagged `integration` |
| `mvn verify` | full gate: unit + package + integration tests |
| `mvn install` | publish to `~/.m2/repository` |
| `mvn -pl bookshop-core test` | build one module of a reactor |
| `mvn -pl bookshop-app -am package` | one module + its prerequisites |
| `mvn dependency:tree -Dverbose` | show conflict resolutions |
| `mvn dependency:analyze` | find undeclared/unused dependencies |
| `mvn versions:set -DnewVersion=…` | re-version all modules at once |
| `mvn wrapper:wrapper -Dmaven=3.9.9` | add the Maven wrapper |
| `jar tf target/app.jar` | list what's inside a jar |
| `./mvnw -B clean verify` | what CI runs |

## Oracle cheat sheet

| Command | What it does |
|---|---|
| `docker start oracle-free` | start the database (after the first `docker run`) |
| `docker logs -f oracle-free` | watch for `DATABASE IS READY TO USE!` |
| `docker exec -it oracle-free sqlplus shopuser/shoppass@localhost/FREEPDB1` | open a SQL shell |
| `nc -zv localhost 1521` | is the listener even accepting connections? |
| `mvn compile exec:java -Dexec.mainClass=com.example.db.ConnectionCheck` | prove JDBC connectivity in isolation |

| URL / error | Meaning |
|---|---|
| `jdbc:oracle:thin:@//host:1521/FREEPDB1` | service-name form — **use this** |
| `jdbc:oracle:thin:@host:1521:FREE` | legacy SID form (note the `:`) |
| `No suitable driver found` | wrong or missing driver dependency |
| `ORA-12541: TNS:no listener` | database not running / wrong port |
| `ORA-12514: service not known` | wrong service name |
| `ORA-00942: table or view does not exist` | connected to the CDB instead of the PDB, or wrong schema |
| `ORA-01950: no privileges on tablespace` | user needs `QUOTA UNLIMITED ON USERS` |
