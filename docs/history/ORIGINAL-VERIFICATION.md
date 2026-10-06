# Verification - repaired release, 26 September 2026

## Passed

- Maven build with JDK 25, producing Java 17-compatible bytecode and a deployable WAR.
- 17 automated Java tests: 12 existing circulation/payment/security tests and 5 added workflow tests. Zero failures/errors on H2 and Microsoft SQL Server 2025 Express.
- Read-only connection to the configured existing LuminaLibrary database using both the Windows SQL client and the exact Microsoft JDBC driver shipped in the WAR.
- SQL regression data used separate LuminaVerification databases, not the user's library database.
- Additive migrations run repeatedly without deleting data; received legacy purchases acquire their correct received quantity.
- Tomcat 10.1.59 deployment, JSP compilation, public pages, account pages, catalogue, management pages, circulation, reservations, fines, uploads forms, dashboard and reports.
- Extended HTTP tests: master-data create/read/update/delete/deactivate, member creation/contact/type updates, multiline purchase, partial receipt, over-receipt rejection, copy location, loan issue/due-date/return/void, reservation create/edit/cancel, branch/role restrictions, and all 15 report types with filters.
- Fine HTTP tests: overdue assessment, partial payment, payment history, staff receipt, reference/method correction, reversal, return, amount adjustment and settlement.
- Existing end-to-end digital checkout, protected PDF access, cross-account denial, reading progress, receipt, notification read status and CSRF rejection.
- PDF and Excel report exports returned valid file signatures.
- Browser visual inspection of the homepage, sign-in, staff dashboard and multiline acquisition form; original appearance retained.
- Double-click database-check script executed successfully through Windows PowerShell.

## Boundaries

Live PayHere charging and SMTP delivery need external credentials and were not executed. Public deployment/load testing was not performed. No actual user accounts, loans or uploaded files were migrated from the unrelated LuminaLibraryDB schema. The source database connection was tested read-only; startup migrations will run when the user starts this release against that database.

Reports and the member/administrative history views are appropriate for an academic/local application; very large databases may need server-side pagination. Transaction deletion is intentionally implemented as audited void/reversal/cancellation where history is linked.

## Reproduce

Build: `mvn clean package` (embedded isolated test data).
Run HTTP tests only against a disposable SQL/demo installation:

    python tests/http_smoke.py http://localhost:8080/lumina
    python tests/http_crud.py http://localhost:8080/lumina

An optional final argument to http_crud.py enables an overdue SQL fixture via local Windows-authenticated sqlcmd. It only accepts a database name beginning LuminaVerification_. Never pass a real library database.

For SQL Java tests, create a NEW empty verification database, then run Maven with test.db.mode=sqlserver and test.db.url containing that database's JDBC URL. Authentication is loaded from the configured local properties. The tests intentionally create/modify verification records.
