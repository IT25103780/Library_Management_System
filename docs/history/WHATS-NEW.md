# Latest edit — 1 October 2026

- Removed visible barcodes from every page, including HTML and PDF receipts. ISBN text and validation remain available for book records.
- Added one 88px QR code on the book details page, linking to that book. Set app.url to the reachable app address when deploying so phones can open the link.
- Demo checkout now has name, exactly 16 digits, MM/YY expiry and three-digit CVV fields. Browser validation only; unnamed fields are never submitted or stored. Any 16 digits are accepted; no bank or Luhn verification is performed.
- Refined signed-in page spacing, cards, navigation and animations using existing fonts and palette, with reduced-motion support.
- Rebuilt dist/lumina.war. Existing launch commands are unchanged.
- Verified: 21 Java tests, HTTP smoke and enhancement suites, browser rejection of 15 digits and successful demo checkout with 16 digits.

## Historical release notes (superseded where noted above)

# Lumina Library — payment and design update

The original home cinema, wording, fonts and chocolate / brown / ivory / gold palette are preserved. This package includes updated source code and a rebuilt `dist/lumina.war`.

## Try the updated app

1. Extract the entire ZIP first.
2. With Java 17+ and Tomcat 10.1 installed, run `Start-Demo.cmd` to use the embedded demonstration database. The existing `Start-Lumina.cmd` continues to use your SQL Server configuration.
3. Open `http://localhost:8080/lumina`.
4. On a fresh demo database, sign in with username `reader` or `admin` and password `Lumina@2026!`.
5. Choose a digital book, click **Borrow online**, and choose **Simulate successful payment**. No money is charged.

If Tomcat is not detected automatically, use:

```powershell
./Start-Lumina.ps1 -DemoDatabase -TomcatHome "C:/path/to/apache-tomcat-10.1" -Port 8080
```

## What changed

- Refined signed-in catalogue, bookshelf, reader controls, checkout, profile and staff/admin workspaces. Existing text, fonts, colors and home animation remain.
- Visa and Mastercard selection, secure-payment explanation, order review and repeated-click protection.
- After verified success, a gold circle and green check animate into view with **Payment Successful**, a small particle burst and the payment total. It closes after roughly four seconds. Close and Escape work immediately. Reduced-motion preferences are respected.
- The receipt stays visible after the confirmation disappears. Payment-history receipts do not trigger the animation. Refreshing the same successful result does not replay it in the same session.
- Pending payment polling checks a separate JSON status response, so polling cannot consume the success animation before the user sees it.
- Styled HTML receipts and a direct **Download Receipt** PDF, with transaction details, borrowing dates and barcode. Print / Save as PDF remains available.
- Payment-time book title, ISBN, member name and borrowing dates are stored as receipt snapshots. Existing successful payments are backfilled from available records during migration; information already changed before this upgrade cannot be reconstructed.
- Server-rendered EAN-13 barcodes for valid ISBNs. ISBN-10 is validated and converted. The included six original sample books use `LUM-0001` through `LUM-0006`, which are internal references, not assigned ISBNs. These receive scannable Code 128 barcodes explicitly labelled **Library reference · ISBN not assigned**. Enter the correct edition ISBN in book management for genuine ISBN-13 labels. No ISBN assignments have been invented.
- Password creation/reset/change requires 10–128 characters, uppercase, lowercase, number and special character. A short common-password denylist rejects obvious examples. An animated red/green line, live checklist, Show/Hide and confirmation matching are provided. The server enforces the same rules; existing login passwords remain usable.
- Additive database migrations run automatically on application startup.

## Enable actual card payments

The default remains demonstration mode so the ZIP can be tried without charging money. Card number, expiry and CVV are entered on PayHere's hosted payment page, where the provider and bank validate them. Lumina does not collect or store full card numbers or CVVs.

Use a PayHere merchant account approved for your deployed domain. Configure these settings privately in `config/lumina.properties` or equivalent environment variables:

```properties
payment.mode=payhere-sandbox
payhere.merchant.id=YOUR_MERCHANT_ID
payhere.merchant.secret=YOUR_MERCHANT_SECRET
app.url=https://your-domain.example/lumina
```

Test the hosted checkout with PayHere's sandbox credentials and test cards. For real payments, use your activated live credentials and change the mode to `payhere-live`. Restart after configuration changes. For an embedded database with a custom payment configuration, pass both `-DemoDatabase` and `-ConfigFile` to the launcher; the switch selects the launcher behavior, and the config file determines the database/payment modes.

The public URL must allow PayHere to call `/payment-notify`. Localhost alone cannot receive those external notifications. The app validates the merchant, signature, currency and amount before enabling reading access. Only the last four card digits supplied by the verified payment callback are saved for display. No live merchant credentials are included or invented, and no real charge was made during testing.

PayHere documentation: https://support.payhere.lk/api-%26-mobile-sdk/checkout-api

## Build and checks

Run `Build.ps1` to run the Java tests and rebuild the distributable WAR.

Verification for this update includes Java tests for password rules, ISBN validation and conversion, decoding the generated barcodes, immutable receipt snapshots, and decoding the ISBN barcode from an actual rendered PDF receipt. Existing borrowing, callback-signature, duplicate-payment, refund and permission tests remain.

The HTTP checks are intended for a disposable demo database:

```text
python tests/http_smoke.py http://localhost:8080/lumina
python tests/http_crud.py http://localhost:8080/lumina
python tests/http_enhancements.py http://localhost:8080/lumina
```

Browser review covered desktop catalogue, success confirmation and receipt, phone-size receipt and password feedback. Live bank authorization and SQL Server deployment must be checked with your own merchant and database setup; local integration verification used H2 demonstration mode.

Do not overwrite an existing deployment's external database or `lumina-data` folder when upgrading. This release ZIP does not include the temporary test database or test server logs.
