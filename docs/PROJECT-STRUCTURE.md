# Lumina Library — project structure guide

This edition reorganizes the supplied LuminaLibrary_Card_Fixed project. It retains Java 17,
Maven WAR packaging, Jakarta Servlet/JSP, SQL Server, optional H2 demo mode, and external
Tomcat. It does not convert the application to Spring Boot, JPA or Hibernate.

## 1. Open and run the project

1. Extract the full ZIP to a normal folder.
2. Open the extracted LuminaLibrary folder or its pom.xml in your Java IDE.
3. Use JDK 17 or later and import it as a Maven project.
4. Double-click Check-Database.cmd, then Start-Lumina.cmd.
5. Wait for Tomcat startup, then open http://localhost:8080/lumina/.

The included dist/lumina.war is already rebuilt. Maven is required when changing Java code
and rebuilding, but not for launching the included WAR. Use Build.ps1 after source edits.
For the existing optional H2 demo mode, use Start-Demo.cmd.

The existing launcher still accepts -TomcatHome, -JavaHome, -Port, -DemoDatabase and
-ConfigFile. Its default port is still 8080 and its application context is still /lumina.
The test port and disposable test database are not configured in the delivered project.

## 2. Packages and responsibilities

| Package | Responsibility | Start reading here |
|---|---|---|
| config | Properties, startup, migrations, database diagnostic and editable directory definitions | AppConfig, ApplicationInitializer |
| controller | URL dispatch, request parsing, access checks, form handling and JSP responses | MainController, BookController |
| service | Borrowing, circulation, fines, payments, reservations, purchasing, notifications and file handling | CirculationService, PaymentService |
| repository | SQL statements and shared JDBC execution | BookRepository, LoanRepository, JdbcRepository |
| model | Typed account permission view and login-attempt state | UserAccount, LoginAttempt |
| dto | Data submitted between layers | PurchaseItem |
| security | Password/token operations and role/branch permission checks | Security, AccessControl |
| util | Shared formatting, validation, barcodes and QR codes | ViewUtils, BusinessRules |

There are 68 Java source files in these eight packages. Each file has a defined use;
there are no empty placeholder packages or unused model files added just for appearance.

## 3. Complete Java tree

```text
src/main/java/lk/lumina/
  config/
    AppConfig.java
    ApplicationInitializer.java
    DatabaseCheck.java
    DatabaseMigrations.java
    EntityDefinitions.java
  controller/
    AccountController.java
    AdminController.java
    AuthController.java
    BaseController.java
    BookController.java
    BorrowingController.java
    CirculationController.java
    FileResponse.java
    FineController.java
    HomeController.java
    InventoryController.java
    MainController.java
    ManagementController.java
    MediaController.java
    NotificationController.java
    PaymentController.java
    ProcurementController.java
    ReportController.java
    ReservationController.java
  dto/
    PurchaseItem.java
  model/
    LoginAttempt.java
    UserAccount.java
  repository/
    AdministrationRepository.java
    AuditRepository.java
    BookRepository.java
    DashboardRepository.java
    FineRepository.java
    InventoryRepository.java
    JdbcRepository.java
    LoanRepository.java
    NotificationRepository.java
    PaymentRepository.java
    PurchaseRepository.java
    ReceiptRepository.java
    ReminderRepository.java
    ReportRepository.java
    ReservationRepository.java
    SettingsRepository.java
    UserRepository.java
    WorkspaceRepository.java
  security/
    AccessControl.java
    Security.java
  service/
    AuditService.java
    BookReferenceService.java
    BorrowingService.java
    CirculationService.java
    FileStorageService.java
    FineService.java
    InventoryService.java
    MailService.java
    NotificationService.java
    PaymentGatewayService.java
    PaymentService.java
    ProcurementService.java
    ReceiptService.java
    ReminderService.java
    ReportService.java
    ReservationService.java
    SettingsService.java
  util/
    Barcodes.java
    BookQr.java
    BusinessRules.java
    ViewUtils.java
```

## 4. How a request works

MainController is the **only registered servlet**. It keeps the original URL mappings,
security headers, login/session checks, CSRF validation, upload limits, exception handling
and static-file forwarding. It dispatches to the relevant feature controller.

Feature controllers are ordinary Java classes, not independently registered servlets.
They use BaseController for input parsing, redirects and forwarding to JSPs. This keeps
all existing links and forms valid without creating conflicting servlet mappings.

For a business operation:

```text
Browser form
  -> MainController (session and CSRF checks)
  -> Feature controller (input and role checks)
  -> Service (business rules and transaction)
  -> Repository (SQL)
  -> JdbcRepository (prepared statement / database connection)
  -> SQL Server
```

Simple page reads can go directly from a controller to a repository. JSPs remain in
src/main/webapp/WEB-INF/views. Images, CSS, JavaScript, animation frames and the PDF viewer
remain in src/main/webapp/assets, preserving their public URLs.

### Example: issue a physical book

1. The circulation form posts action=issue to /action.
2. MainController verifies the session and CSRF token.
3. CirculationController checks staff access and parses the member/copy IDs.
4. CirculationService.issue checks copy availability, reservations, borrowing limits,
   overdue loans, fines and branch scope.
5. LoanRepository reads and updates the loan/copy/reservation records within the original
   JdbcRepository.tx transaction.
6. NotificationService and AuditService write the member notification and audit entry.
7. The controller redirects to the circulation page.

### Example: borrow and pay for a digital book

1. BorrowingController receives action=borrow.
2. PaymentService.createOrder validates the book and duration, then creates/reuses an order.
3. PaymentController displays the checkout and handles the demo outcome.
4. PaymentService.settle records the outcome and activates/extends the digital loan.
5. ReceiptService captures the original receipt details.
6. BorrowingService.reading checks ownership, payment and expiry before PDF access.

PaymentGatewayService retains the optional PayHere integration and callback signature checks.

### Example: purchase several books

ProcurementController builds a List<PurchaseItem>. Each PurchaseItem contains book ID,
quantity and unit cost. ProcurementService validates the purchase and uses PurchaseRepository
to save every line in one transaction. Receiving stock also updates physical copies.

## 5. Why models do not duplicate every database table

The original application uses JDBC rows represented as Map<String,Object>, and the JSPs
expect those same keys and values. This contract is retained to preserve behavior.
UserAccount provides a typed view for permission checks; LoginAttempt holds rate-limit state;
PurchaseItem carries a purchase line. The code does not claim to be an ORM entity model.

Repository methods retain prepared-statement value binding and the original result-map
format. Connection parameters mean that an operation joins its caller's existing transaction.
Some catalog/workspace/report queries retain dynamic, internally constructed filter or
table fragments, with the existing allowlists and bound values. This is a conservative
refactor, not a rewrite of every data representation or business rule.

## 6. What moved from the old files

| Old file | New location / responsibilities |
|---|---|
| Web.java | MainController; feature controllers; BaseController; FileResponse; FileStorageService; ViewUtils; AccessControl; EntityDefinitions; repositories |
| Library.java | PaymentService, BorrowingService, CirculationService, ReservationService, ReminderService, NotificationService, AuditService, SettingsService, BusinessRules |
| Workflows.java | FineService, CirculationService, ReservationService, ProcurementService, InventoryService and AccessControl |
| DB.java | repository/JdbcRepository.java |
| Config.java | config/AppConfig.java |
| Bootstrap.java | config/ApplicationInitializer.java |
| Migrations.java | config/DatabaseMigrations.java |
| DatabaseCheck.java | config/DatabaseCheck.java; Check-Database.ps1 updated to this class name |
| Payments.java | service/PaymentGatewayService.java |
| Procurement.java | service/ProcurementService.java and dto/PurchaseItem.java |
| Receipts.java | service/ReceiptService.java |
| Report.java | service/ReportService.java and repository/ReportRepository.java |
| Mail.java | service/MailService.java |
| BookReferences.java | service/BookReferenceService.java |
| Security.java | security/Security.java |
| Barcodes.java, BookQr.java | util/Barcodes.java and util/BookQr.java |

216 query call sites were extracted into domain repositories. Startup schema/migration and
seed operations remain with the startup/configuration code. ReportService retains the report
filter/query construction and document-export behavior; ReportRepository executes its queries.

Java test assertions were retained while their imports and class references were updated.
JSP changes are limited to the relocated Java imports and helper class references.

## 7. What to explain in your presentation

“Lumina Library is a Java Servlet and JSP application deployed as a WAR on Tomcat. We have
organized the code by responsibility: controllers handle web requests, services implement
library rules, repositories hold SQL operations, and configuration handles startup. Security,
models, DTOs and shared utilities have separate packages. The restructuring preserves the
original website and database behavior.”

Suggested five-minute demonstration:

1. Show the eight Java packages in the IDE.
2. Open MainController and explain session checking and feature dispatch.
3. Open CirculationController, CirculationService and LoanRepository to trace issuing a book.
4. Show PurchaseItem as a small DTO and UserAccount as a model used for permissions.
5. Show WEB-INF/views and assets, then open the running website.
6. Mention the passing Java and Tomcat/SQL Server regression checks in VERIFICATION.md.

## 8. Existing database and uploads

The supplied config/lumina.properties and demo.properties are unchanged. No test data,
test SQL credentials, runtime caches or temporary server configuration are packaged.

Your existing SQL database stays outside the ZIP. Existing uploads also stay outside the WAR:
the default location is runtime/lumina-data/pdf and runtime/lumina-data/covers under the
installation's CATALINA_BASE. When moving to a new extracted folder, retain that existing
upload data or point storage.path to it. SQL records alone do not contain the uploaded files.
Existing website passwords are not reset by this refactor.

See VERIFICATION.md for actual results and the external-service testing boundaries.
