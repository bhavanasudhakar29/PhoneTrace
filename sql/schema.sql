-- =====================================================================
--  PhoneTrace - Used Phone History Verification System
--  Database schema + sample data (MySQL 8)
--  Run this whole file once in MySQL Workbench or the MySQL command line.
-- =====================================================================

DROP DATABASE IF EXISTS phonetrace;
CREATE DATABASE phonetrace;
USE phonetrace;

-- ---------------------------------------------------------------------
-- 1. OWNERS : every person who has ever owned a phone
--    Stored once here (normalization) and referenced by ID elsewhere.
-- ---------------------------------------------------------------------
CREATE TABLE owners (
    owner_id  INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    phone     CHAR(10)     NOT NULL UNIQUE,
    CONSTRAINT chk_owner_phone CHECK (CHAR_LENGTH(phone) = 10)
);

-- ---------------------------------------------------------------------
-- 2. PHONES : one row per device. IMEI is globally unique -> natural PK.
-- ---------------------------------------------------------------------
CREATE TABLE phones (
    imei   CHAR(15)    PRIMARY KEY,
    brand  VARCHAR(50) NOT NULL,
    model  VARCHAR(50) NOT NULL,
    CONSTRAINT chk_imei_length CHECK (CHAR_LENGTH(imei) = 15)
);

-- ---------------------------------------------------------------------
-- 3. OWNERSHIPS : one phone -> many owners over time (one-to-many)
--    to_date = NULL means "this is the CURRENT owner".
--    Deleting a phone deletes its history (CASCADE).
--    An owner who appears in history can NOT be deleted (RESTRICT).
-- ---------------------------------------------------------------------
CREATE TABLE ownerships (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    imei       CHAR(15) NOT NULL,
    owner_id   INT      NOT NULL,
    from_date  DATE     NOT NULL,
    to_date    DATE     NULL,
    CONSTRAINT fk_own_phone FOREIGN KEY (imei)     REFERENCES phones(imei)     ON DELETE CASCADE,
    CONSTRAINT fk_own_owner FOREIGN KEY (owner_id) REFERENCES owners(owner_id) ON DELETE RESTRICT,
    CONSTRAINT chk_own_dates CHECK (to_date IS NULL OR to_date >= from_date)
);

-- ---------------------------------------------------------------------
-- 4. REPAIRS : every repair done on a phone
-- ---------------------------------------------------------------------
CREATE TABLE repairs (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    imei         CHAR(15)     NOT NULL,
    part         VARCHAR(50)  NOT NULL,
    shop_name    VARCHAR(100) NOT NULL,
    repair_date  DATE         NOT NULL,
    genuine      CHAR(1)      NOT NULL DEFAULT 'Y',
    CONSTRAINT fk_rep_phone FOREIGN KEY (imei) REFERENCES phones(imei) ON DELETE CASCADE,
    CONSTRAINT chk_genuine CHECK (genuine IN ('Y', 'N'))
);

-- ---------------------------------------------------------------------
-- 5. THEFT_REPORTS : stolen-phone complaints
-- ---------------------------------------------------------------------
CREATE TABLE theft_reports (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    imei         CHAR(15)    NOT NULL,
    report_date  DATE        NOT NULL,
    status       VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_theft_phone FOREIGN KEY (imei) REFERENCES phones(imei) ON DELETE CASCADE,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'RECOVERED'))
);

-- =====================================================================
--  SAMPLE DATA
--  Dates are relative to today (CURDATE) so the demo always works,
--  no matter which day you run it.
-- =====================================================================

INSERT INTO owners (owner_id, name, phone) VALUES
 (1, 'Karthik', '9840011111'),
 (2, 'Arun',    '9840022222'),
 (3, 'Bala',    '9840033333'),
 (4, 'Charan',  '9840044444'),
 (5, 'Deepak',  '9840055555'),
 (6, 'Priya',   '9840066666'),
 (7, 'Meena',   '9840077777'),
 (8, 'Ravi',    '9840088888');   -- not an owner yet, use him for live transfer demos

INSERT INTO phones (imei, brand, model) VALUES
 ('356789012345671', 'Samsung', 'Galaxy S23'),   -- CLEAN phone
 ('356789012345672', 'Apple',   'iPhone 13'),    -- SUSPICIOUS phone
 ('356789012345673', 'OnePlus', '11'),           -- STOLEN phone
 ('356789012345674', 'Redmi',   'Note 12');      -- stolen once, but RECOVERED

-- Phone 1 (clean): single owner
INSERT INTO ownerships (imei, owner_id, from_date, to_date) VALUES
 ('356789012345671', 1, DATE_SUB(CURDATE(), INTERVAL 580 DAY), NULL);

-- Phone 2 (suspicious): 4 owners, 3 of them within the last year
INSERT INTO ownerships (imei, owner_id, from_date, to_date) VALUES
 ('356789012345672', 2, DATE_SUB(CURDATE(), INTERVAL 650 DAY), DATE_SUB(CURDATE(), INTERVAL 300 DAY)),
 ('356789012345672', 3, DATE_SUB(CURDATE(), INTERVAL 300 DAY), DATE_SUB(CURDATE(), INTERVAL 215 DAY)),
 ('356789012345672', 4, DATE_SUB(CURDATE(), INTERVAL 215 DAY), DATE_SUB(CURDATE(), INTERVAL 110 DAY)),
 ('356789012345672', 5, DATE_SUB(CURDATE(), INTERVAL 110 DAY), NULL);

-- Phone 3 (stolen) and Phone 4 (recovered)
INSERT INTO ownerships (imei, owner_id, from_date, to_date) VALUES
 ('356789012345673', 6, DATE_SUB(CURDATE(), INTERVAL 900 DAY), NULL),
 ('356789012345674', 7, DATE_SUB(CURDATE(), INTERVAL 400 DAY), NULL);

INSERT INTO repairs (imei, part, shop_name, repair_date, genuine) VALUES
 ('356789012345671', 'Battery',    'Samsung Service Centre', DATE_SUB(CURDATE(), INTERVAL 100 DAY), 'Y'),
 ('356789012345672', 'Display',    'Quick Fix Mobiles',      DATE_SUB(CURDATE(), INTERVAL 250 DAY), 'N'),
 ('356789012345672', 'Display',    'Cell World',             DATE_SUB(CURDATE(), INTERVAL 120 DAY), 'N'),
 ('356789012345672', 'Battery',    'iCare Repairs',          DATE_SUB(CURDATE(), INTERVAL 80 DAY),  'Y'),
 ('356789012345674', 'Back Panel', 'Mi Service Centre',      DATE_SUB(CURDATE(), INTERVAL 150 DAY), 'Y');

INSERT INTO theft_reports (imei, report_date, status) VALUES
 ('356789012345673', DATE_SUB(CURDATE(), INTERVAL 21 DAY),  'ACTIVE'),
 ('356789012345674', DATE_SUB(CURDATE(), INTERVAL 200 DAY), 'RECOVERED');

-- =====================================================================
--  Project login used by the Java code (DBConnection.java)
-- =====================================================================
CREATE USER IF NOT EXISTS 'phonetrace'@'localhost' IDENTIFIED BY 'phonetrace123';
GRANT ALL PRIVILEGES ON phonetrace.* TO 'phonetrace'@'localhost';
FLUSH PRIVILEGES;

-- Quick check
SELECT 'PhoneTrace database ready' AS status;
