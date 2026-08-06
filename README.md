# 📚 Bookshop — Learning Maven & JUnit

A small but real Java project I'm building to **master Apache Maven and JUnit 5 from the ground up** — no frameworks, no magic, everything by hand first. Each concept is added to this same project, phase by phase, so I can feel the problem before using the tool that solves it.

> This is a learning lab, not a product. It's small on purpose, and I break it on purpose.

## 🛠 Tech Stack

| Tool | Purpose |
|---|---|
| Java 17 | Language |
| Apache Maven | Build tool (the main subject of this repo) |
| JUnit 5 (Jupiter) | Unit & parameterized testing |
| Gson | JSON serialization |
| MySQL + JDBC | Persistence (in progress) |

## 🚀 Getting Started

```bash
git clone git@github.com:MOHAMEDELWAZANI/LearningMaven.git
cd LearningMaven

mvn test        # run the test suite
mvn package     # build the fat jar (shade plugin)
mvn exec:java   # run the app through Maven

# or run the packaged fat jar directly:
java -jar target/bookshop-1.0-SNAPSHOT.jar
```

## 📁 Project Structure

```
bookshop/
├── pom.xml                          the heart of the project
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── App.java             entry point
│   │   │   ├── Book.java            immutable domain entity
│   │   │   └── BookShop.java        business logic (add, total, search, JSON)
│   │   └── resources/
│   │       └── shop.properties      filtered config (Maven resource filtering)
│   └── test/
│       └── java/com/example/
│           └── BookShopTest.java    unit + parameterized tests
└── docs/                            the guides I'm learning from
```

## 🗺 Learning Roadmap & Progress

- [x] **Phase 1 — Core application**: entity, service, JUnit 5 tests, Surefire
- [x] **Phase 2 — Resources & configuration**: classpath resources, `getResourceAsStream`, resource filtering, Maven properties
- [ ] **Phase 3 — Database integration**: JDBC, DAO pattern, ConnectionFactory, dependency scopes
- [x] **Phase 4 — Profiles**: `dev` done; `test` / `prod` with filtered configs in progress
- [ ] **Phase 5 — Testing level 2**: integration tests, `@Tag`, Failsafe plugin
- [x] **Phase 6 — Packaging**: executable jar, `Main-Class` manifest, Shade fat jar
- [ ] **Phase 7 — Local repository**: `mvn install`, consuming this project as a dependency
- [ ] **Phase 8 — Multi-module**: parent POM, `bookshop-core` / `bookshop-database` / `bookshop-app`, the reactor
- [ ] **Phase 9 — Advanced Maven**: wrapper, `dependency:tree`, BOMs, SNAPSHOT vs release, CI with GitHub Actions

## 📌 Maven Concepts Practiced So Far

- Standard directory layout & the default lifecycle (`validate → compile → test → package → verify → install → deploy`)
- The POM: GAV coordinates, properties, `dependencyManagement` + the JUnit **BOM**
- Dependency **scopes** (`compile`, `test`, and soon `runtime`)
- `pluginManagement` vs `plugins` — declaring versions vs activating behavior
- Binding plugin goals to lifecycle phases (`exec`, `shade`)
- Resource **filtering** with profile-driven property overrides
- Calling goals directly: `mvn exec:java`, `mvn dependency:tree`, `mvn help:effective-pom`

## 📖 Docs

The `docs/` folder contains the guides driving this project, including the
**Bookshop Master Roadmap Guide** — a phase-by-phase walkthrough of every
concept above, with examples taken from this codebase.

---

*Built by [Mohamed El Wazani](https://github.com/MOHAMEDELWAZANI) — day by day, one `mvn` command at a time.*
