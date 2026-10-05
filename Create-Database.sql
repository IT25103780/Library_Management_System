-- Run once in SQL Server Management Studio. Never points to the original library database.
IF DB_ID(N'Lumina_reservations') IS NULL CREATE DATABASE [Lumina_reservations];
GO
-- The application creates its schema automatically on first startup.
