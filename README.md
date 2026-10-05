# Book Management


## Run

Extract the whole folder, then double-click Start.cmd. Requires Java 17 or later. Tomcat is included. Open http://localhost:8101/lumina-books/ . Sign in as admin with password Lumina@2026! for the complete module. Additional accounts: librarian, reader, branch_manager, management (same initial password).

## Your CRUD demonstration

1. Add a book and physical copies.
2. Search the catalogue and view book availability.
3. Edit book details, ISBN, cover, copy branch and shelf.
4. Delete unused books/copies; deactivate or withdraw records with history.

## Source ownership

- `src/main/java/lk/lumina/Web.java`: your module's request controller. Other members' routes and actions have been removed.
- `src/main/java/lk/lumina/ModuleAccess.java`: this member's server-side route, entity and role boundary, navigation and identity.
- `src/main/webapp/WEB-INF/views/`: your module's pages, plus the shared home, catalogue, login and account pages.
- `src/main/java/lk/lumina/Library.java` and `Workflows.java`: local shared transaction rules for stock, queues and fines. These are support code, not separately exposed member modules.
- `DB.java`, `Config.java`, `Security.java`, `Bootstrap.java`, `Migrations.java`: shared database/login/setup infrastructure.
- `DemoFixtures.java`: independent demonstration records, created only for a new demo database.
- `src/main/resources/schema.sql`: database structure, also copied into database/. Related tables are retained for references and history.

## Independence and data

This is a separate project derived from the supplied LuminaLibrary_Card_Fixed.zip, not a link to another project. It has its own compiled WAR, server runtime, database, cookies and port. Read-only catalogue data and sample users/branches support your module. Editing outside the assigned module is rejected by the server, even if an old URL is entered manually. Changes in one member's project do not appear in another member's project.

The local database and uploads are stored in data/ after first run. Stop the server with Ctrl+C before backing up the whole extracted project. Do not delete data/ if you need to retain your records. Demo data is fictional. No original SQL credentials or existing library records are included.

## SQL Server (optional)

Run database/Create-Database.sql in your own SQL Server. Edit config/sqlserver.properties with your SQL authentication details. Then run `powershell -ExecutionPolicy Bypass -File .\Start.ps1 -SqlServer`. It uses a separate database named Lumina_books. SQL Server is optional; Start.cmd uses the embedded database. SQL Server mode does not add the extra demo transaction fixtures.

## Rebuild after editing source

Install Maven and JDK 17+, then double-click Build.cmd. It compiles and tests the source, and replaces the compiled file in dist/. Stop and restart the server to load it. Use `Start.ps1 -Port 8201` if the default port is busy. The launcher is offline and never installs Java or downloads software.

## Academic scope

The cinematic homepage, digital reader and online payment screens are not part of this member edition. Fines are recorded offline. A cancellation/deactivation/void preserves history and is labelled accurately; it is not claimed to be a physical database deletion. Login and common dependencies are included in each project so all six can run independently.
