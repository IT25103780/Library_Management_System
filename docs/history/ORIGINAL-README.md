# Lumina Library - repaired and completed edition

## Start on this computer

1. Extract the entire ZIP. Do not run the website from inside the ZIP viewer.
2. Double-click `Check-Database.cmd`. It tests both Windows and Java SQL connections without changing records.
3. Double-click `Start-Lumina.cmd`. This creates the configured database only if missing, starts Tomcat, and upgrades the schema without dropping existing tables.
4. Wait for server startup and open http://localhost:8080/lumina/ . Keep the server window open. Stop with Ctrl+C.

Requirements: JDK 17+, SQL Server with SQL authentication/TCP enabled, and Apache Tomcat 10.1. This computer's Downloads folder contains Tomcat 10.1; the launcher detects it. Maven is needed only to rebuild. The ZIP includes the rebuilt `dist/lumina.war` and all Java dependencies inside that WAR.

If Tomcat is elsewhere, open PowerShell in the extracted folder:

    .\Start-Lumina.ps1 -TomcatHome 'C:\path\to\apache-tomcat-10.1' -Port 8080

If port 8080 is occupied, use `-Port 8081`, then open http://localhost:8081/lumina/ .

## Database connection repair

The supplied project already had valid local SQL credentials. Its saved Smart Tomcat configuration did not load the external properties, and its code silently defaulted to H2. That could make SQL records appear missing even though the website ran.

This version defaults to SQL Server, discovers `config/lumina.properties` from the project/launcher, prints the selected mode and configuration path, and never silently falls back to H2. An invalid connection fails visibly. `Initialize-Database.ps1` now reads the same server, database and credentials as the Java application instead of using a separate hard-coded password.

Your supplied SQL connection values are retained in `config/lumina.properties`. On another computer, change `db.url`, `db.user`, and `db.password`. Do not confuse SQL credentials with website accounts. The application uses Microsoft SQL Server, as in your ZIP; it has not been converted to MySQL mentioned in the older proposal.

A named instance may be configured with `jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=LuminaLibrary;encrypt=true;trustServerCertificate=true`. A fixed TCP port is more predictable. On the inspected computer SQL Express listens on port 1433.

For IntelliJ / Smart Tomcat, select JDK 17+ and Tomcat 10.1, context `/lumina`, and set this VM option using the actual extracted path:

    -Dlumina.config="C:/path/to/LuminaLibrary/config/lumina.properties"

Rebuild with `Build.ps1` before using compiled classes in an IDE. Old `.idea`, `.smarttomcat`, build caches and local database files are deliberately excluded from this ZIP.

## Existing data and upgrade

Back up your existing SQL database and uploaded files before replacing an installation. Startup adds the new columns through `Migrations.java`; it does not recreate or erase existing tables. Existing website passwords remain unchanged. The older, separate `LuminaLibraryDB` database uses a different schema and is not automatically imported into `LuminaLibrary`.

Uploads live under `CATALINA_BASE/lumina-data/pdf` and `covers`, outside the WAR. If you previously used Smart Tomcat, keep or copy that instance's `lumina-data` folder, or set `storage.path` to it with forward slashes. Database rows alone do not contain uploaded PDF/image bytes. Never replace an old upload folder with the sample folder.

`database/03-upgrade-reference.sql` documents the additive SQL changes. The application applies these automatically. `02-schema-reference.sql` is a fresh-install schema reference, not a script to run on existing tables.

## Website accounts

On a NEW empty database only, initial usernames are `admin`, `librarian`, `branch_manager`, `management`, and `reader`. Their initial password is `Lumina@2026!`. Existing accounts are not reset during upgrades. Administrators can reset an account password from Accounts; signed-in users can change their own password from Profile.

## Completed functions

See `FUNCTIONS.md` and `REQUIREMENTS-COVERAGE.md` for the complete operation list and important business rules. Additions include loan due-date changes and voiding, member activity/contact/type management, reservation modification and staff entry, overdue fine assessment, partial offline payments, payment histories/receipts/corrections/reversals, fine corrections, copy shelf/branch editing and withdrawal, multiline purchases, staged receipt, overdue/member reports, filtered monthly reports, dashboard counts and searchable activity logs.

The original design, digital borrowing, PDF reader, demonstration checkout and optional provider integration have been preserved. The PDFs excluded digital reading and online gateways from the academic scope, but your supplied website already contained them.

## External services

Offline fine recording and in-app reminders work without external services. Payment mode is `demo`; it does not charge money. Live PayHere requires your merchant account, secret and public HTTPS callback URL. SMTP settings are required for emailed password recovery and optional notification emails. Without SMTP, members can ask an administrator to reset their account password. The local demo database has an explicit development reset-link helper; SQL mode never exposes password reset links publicly. Actual email delivery and live financial transactions have not been tested.

## Demo database (explicit alternative)

    .\Start-Lumina.ps1 -DemoDatabase -Port 8081

This uses H2 and separate demo records. It is not your SQL Server database. Use it only when deliberately demonstrating without SQL Server.

## Build and verification

    .\Build.ps1

Or `mvn clean package` then copy `target/lumina.war` into `dist/lumina.war`. See `VERIFICATION.md` for the exact tests performed. HTTP tests create records and must run against a disposable test database, not your real library.
