-- =========================================================
-- SCHEMA BOOTSTRAP: cria banco e usuário base
-- =========================================================

CREATE DATABASE IF NOT EXISTS tg_connect
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'tguser'@'localhost' IDENTIFIED BY 'pass123';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, TRIGGER, REFERENCES
    ON tg_connect.* TO 'tguser'@'localhost';

FLUSH PRIVILEGES;
