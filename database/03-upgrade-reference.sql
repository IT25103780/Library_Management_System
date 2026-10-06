-- Optional reference only: startup runs the equivalent additive migrations automatically.
-- Run in the configured LuminaLibrary database, never in an unrelated schema.
IF COL_LENGTH('users','member_type') IS NULL ALTER TABLE users ADD member_type varchar(20) NOT NULL DEFAULT 'STUDENT';
IF COL_LENGTH('copies','shelf') IS NULL ALTER TABLE copies ADD shelf nvarchar(100);
IF COL_LENGTH('purchases','received_quantity') IS NULL ALTER TABLE purchases ADD received_quantity int NOT NULL DEFAULT 0;
IF COL_LENGTH('purchases','invoice_ref') IS NULL ALTER TABLE purchases ADD invoice_ref nvarchar(100);
IF COL_LENGTH('purchases','notes') IS NULL ALTER TABLE purchases ADD notes nvarchar(500);
IF COL_LENGTH('fines','adjusted') IS NULL ALTER TABLE fines ADD adjusted int NOT NULL DEFAULT 0;
GO
UPDATE purchases SET received_quantity=quantity WHERE status='RECEIVED' AND received_quantity=0;
