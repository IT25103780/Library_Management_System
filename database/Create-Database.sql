-- Run once in SQL Server Management Studio. Never points to the original library database.
IF DB_ID(N'Lumina_books') IS NULL CREATE DATABASE [Lumina_books];
GO
-- The application creates its schema automatically on first startup.
