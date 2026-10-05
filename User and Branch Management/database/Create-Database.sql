-- Run once in SQL Server Management Studio. Never points to the original library database.
IF DB_ID(N'Lumina_users') IS NULL CREATE DATABASE [Lumina_users];
GO
-- The application creates its schema automatically on first startup.
