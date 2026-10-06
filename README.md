# Lumina Library — structured Java edition

The existing website is organized into config, controller, service, repository, model, dto,
security and util packages. Its external Tomcat / Servlet / JSP / SQL Server setup is retained.

## Start the website

1. Extract the entire ZIP; do not launch from the ZIP viewer.
2. Double-click Check-Database.cmd.
3. Double-click Start-Lumina.cmd.
4. Wait for Tomcat startup and open http://localhost:8080/lumina/.
5. Keep the server window open; press Ctrl+C there to stop it.

Requires JDK 17+, Apache Tomcat 10.1 and the configured SQL Server connection.
The launcher still discovers Tomcat in Downloads or uses CATALINA_HOME. The rebuilt
dist/lumina.war includes the application dependencies. Maven is only needed to rebuild.

For an explicitly selected Tomcat installation or another port:

```powershell
.\Start-Lumina.ps1 -TomcatHome 'C:\path\to\apache-tomcat-10.1' -Port 8081
```

For the existing optional H2 demo mode, double-click Start-Demo.cmd or use:

```powershell
.\Start-Lumina.ps1 -DemoDatabase -Port 8081
```

## Configuration and existing data

config/lumina.properties and config/demo.properties are unchanged from the supplied ZIP.
The normal launcher uses SQL Server; demo mode is an explicit alternative. On a different
computer, configure db.url, db.user and db.password for that computer's SQL Server.
The application does not silently switch to H2 when SQL Server fails.

Existing website account passwords stay unchanged. On a new empty database, the initial
usernames are admin, librarian, branch_manager, management and reader, with the supplied
bootstrap password Lumina@2026!.

The original additive schema initialization/migration behavior is preserved in
config/ApplicationInitializer.java and config/DatabaseMigrations.java. This refactor does
not introduce database tables or change the existing schema files.

Uploads are stored outside the WAR, normally in runtime/lumina-data/pdf and covers.
Keep your existing upload folder when updating an installation. If using a new extracted
folder, copy the existing upload data or configure storage.path to point to it. A database
connection alone cannot restore PDF/image bytes that are stored in the old upload folder.

## Open and rebuild in a Java IDE

Open the LuminaLibrary folder/pom.xml as a Maven project, choose JDK 17+, and expand
src/main/java/lk/lumina to see the organized packages. Use the same external Tomcat setup.

For an IDE-managed Tomcat instance, retain context /lumina and provide the actual config path:

```text
-Dlumina.config="C:/path/to/LuminaLibrary/config/lumina.properties"
```

After Java source edits:

```powershell
.\Build.ps1
```

This runs the Java tests, builds target/lumina.war and updates dist/lumina.war, which the
existing launcher deploys. Do not run an old WAR after changing only source files.

## Guides

- [Structure and presentation guide](docs/PROJECT-STRUCTURE.md): every Java file, package purpose,
  old-to-new mapping and example request flows.
- [Verification results](VERIFICATION.md): actual tests, environment and boundaries.
- [Function inventory](FUNCTIONS.md): original functionality.
- [Requirements coverage](REQUIREMENTS-COVERAGE.md): existing business rules and operation coverage.
- [Historical notes](docs/history/): the original release documentation, preserved for reference.

## External services

Demo checkout does not charge money. Live PayHere requires merchant configuration and a
public callback URL. Email requires SMTP configuration. Those external deliveries were not
executed during this refactor. HTTP regression tests must use disposable data.
