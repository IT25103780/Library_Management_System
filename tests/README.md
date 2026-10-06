# Test instructions

See ../VERIFICATION.md for coverage and results. The HTTP suites create records; use a disposable installation/database only. Python's standard library is sufficient.

- `mvn test`: isolated H2 Java regression tests.
- `python tests/http_smoke.py URL`: original website, digital reading, exports, ownership and CSRF.
- `python tests/http_crud.py URL`: extended CRUD, multibook purchasing, circulation, reservations and role checks.
- `python tests/http_crud.py URL LuminaVerification_NAME`: additionally uses local sqlcmd to mark only the test member's loan overdue and verify partial payment/correction/reversal/settlement. The URL must use that same disposable SQL database.

SQL Java tests use `-Dtest.db.mode=sqlserver` and `-Dtest.db.url=jdbc:sqlserver://localhost:1433;databaseName=NEW_TEST_DATABASE;encrypt=true;trustServerCertificate=true`.
Use a fresh database for each complete Java run because the original ordered regression suite assumes seeded records without previous loans.

- `python tests/http_structure.py URL`: generated PDF/cover uploads, editing, fixed references, PDF range/HEAD, payment cancellation/retry/refund, profile and password changes. Disposable demo-payment deployment only.
- `python tests/http_enhancements.py URL`: payment polling, receipts and password validation.
- `node tests/card-format.cjs`: card input formatting regression checks.
