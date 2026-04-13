-- =====================================================
-- BASE DE DONNÉES MUTUELLE WEB
-- =====================================================

CREATE DATABASE IF NOT EXISTS mutuelle_db;
USE mutuelle_db;

-- =====================================================
-- 1. TABLES DE BASE
-- =====================================================

CREATE TABLE user (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    tel VARCHAR(20),
    email VARCHAR(150) UNIQUE NOT NULL,
    address TEXT,
    type ENUM('SUPER_ADMIN', 'ADMIN', 'MEMBER') NOT NULL,
    avatar VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_type (type)
);

CREATE TABLE administrator (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    admin_role ENUM('SECRETAIRE_GENERALE', 'PRESIDENT', 'TRESORIER') NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_username (username)
);

CREATE TABLE member (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    registration_number VARCHAR(20) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    inscription_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    INDEX idx_username (username),
    INDEX idx_registration_number (registration_number),
    INDEX idx_active (active)
);

-- =====================================================
-- 2. STRUCTURE TEMPORELLE
-- =====================================================

CREATE TABLE exercise (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    year VARCHAR(10) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    interest_rate DECIMAL(5,2) DEFAULT 3.00,
    inscription_amount DECIMAL(15,2) NOT NULL DEFAULT 50000,
    solidarity_amount DECIMAL(15,2) NOT NULL DEFAULT 150000,
    agape_amount DECIMAL(15,2) NOT NULL DEFAULT 45000,
    penalty_amount DECIMAL(15,2) NOT NULL DEFAULT 15000,
    active BOOLEAN DEFAULT FALSE,
    administrator_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    -- Removing UNIQUE KEY constraint if it causes issues on some DB engines, but prompt includes it.
    -- Better to handle logic in Service layer though Spring.
    INDEX idx_year (year),
    INDEX idx_active (active)
);

CREATE TABLE session (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    exercise_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    session_number INT NOT NULL,
    date DATE NOT NULL,
    state ENUM('OPEN', 'SAVING', 'CLOSED', 'ARCHIVED') DEFAULT 'OPEN',
    active BOOLEAN DEFAULT TRUE,
    closed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exercise_id) REFERENCES exercise(id) ON DELETE CASCADE,
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    INDEX idx_state (state),
    INDEX idx_active (active)
);

-- =====================================================
-- 3. CAISSES
-- =====================================================

CREATE TABLE cashbox (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name ENUM('INSCRIPTION', 'SOLIDARITY', 'SAVING', 'REFUELING', 'PENALTY') NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    last_update DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_name (name)
);

INSERT INTO cashbox (name, balance) VALUES
('INSCRIPTION', 0),
('SOLIDARITY', 0),
('SAVING', 0),
('REFUELING', 0),
('PENALTY', 0);

-- =====================================================
-- 4. SOLIDARITÉ
-- =====================================================

CREATE TABLE solidarity (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method ENUM('CASH', 'BANK_TRANSFER', 'MOBILE_MONEY') DEFAULT 'CASH',
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    INDEX idx_member (member_id),
    INDEX idx_payment_date (payment_date)
);

CREATE TABLE solidarity_debt (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT UNSIGNED NOT NULL,
    total_due DECIMAL(15,2) NOT NULL DEFAULT 150000,
    total_paid DECIMAL(15,2) NOT NULL DEFAULT 0,
    remaining_debt DECIMAL(15,2) NOT NULL DEFAULT 150000,
    last_payment_date DATE,
    status ENUM('UP_TO_DATE', 'LATE', 'CRITICAL') DEFAULT 'UP_TO_DATE',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    UNIQUE KEY unique_member (member_id),
    INDEX idx_status (status)
);

-- =====================================================
-- 5. ÉPARGNE
-- =====================================================

CREATE TABLE saving (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT UNSIGNED NOT NULL,
    session_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    cumulative_total DECIMAL(15,2) NOT NULL,
    type ENUM('DEPOSIT', 'WITHDRAWAL') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    INDEX idx_member (member_id),
    INDEX idx_session (session_id),
    INDEX idx_type (type)
);

-- =====================================================
-- 6. EMPRUNTS
-- =====================================================

CREATE TABLE borrowing (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT UNSIGNED NOT NULL,
    session_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    requested_amount DECIMAL(15,2) NOT NULL,
    approved_amount DECIMAL(15,2) NOT NULL,
    interest_amount DECIMAL(15,2) NOT NULL,
    net_amount_received DECIMAL(15,2) NOT NULL,
    remaining_balance DECIMAL(15,2) NOT NULL,
    status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'DEFAULTED') DEFAULT 'PENDING',
    due_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    INDEX idx_member (member_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date)
);

CREATE TABLE borrowing_saving (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    borrowing_id BIGINT UNSIGNED NOT NULL,
    saving_id BIGINT UNSIGNED NOT NULL,
    percentage DECIMAL(5,2) NOT NULL,
    FOREIGN KEY (borrowing_id) REFERENCES borrowing(id) ON DELETE CASCADE,
    FOREIGN KEY (saving_id) REFERENCES saving(id) ON DELETE CASCADE,
    UNIQUE KEY unique_borrowing_saving (borrowing_id, saving_id)
);

-- =====================================================
-- 7. REMBOURSEMENTS ET PÉNALITÉS
-- =====================================================

CREATE TABLE refund (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    borrowing_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    session_id BIGINT UNSIGNED NOT NULL,
    exercise_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    remaining_balance DECIMAL(15,2) NOT NULL,
    refund_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (borrowing_id) REFERENCES borrowing(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercise(id) ON DELETE CASCADE,
    INDEX idx_borrowing (borrowing_id),
    INDEX idx_member (member_id)
);

CREATE TABLE penalty (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    borrowing_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL DEFAULT 15000,
    applied_date DATE NOT NULL,
    status ENUM('PENDING', 'PAID', 'DISTRIBUTED') DEFAULT 'PENDING',
    distributed_to_savers BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (borrowing_id) REFERENCES borrowing(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    INDEX idx_borrowing (borrowing_id),
    INDEX idx_status (status)
);

-- =====================================================
-- 8. INTÉRÊTS DES EMPRUNTS
-- =====================================================

CREATE TABLE interest_distribution (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    borrowing_id BIGINT UNSIGNED NOT NULL,
    session_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    total_interest DECIMAL(15,2) NOT NULL,
    distributed_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    remaining_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    status ENUM('PENDING', 'DISTRIBUTING', 'COMPLETED') DEFAULT 'PENDING',
    distribution_date DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (borrowing_id) REFERENCES borrowing(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    INDEX idx_borrowing (borrowing_id),
    INDEX idx_session (session_id),
    INDEX idx_status (status)
);

CREATE TABLE interest_distribution_detail (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    distribution_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    amount_received DECIMAL(15,2) NOT NULL,
    received_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (distribution_id) REFERENCES interest_distribution(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    INDEX idx_distribution (distribution_id),
    INDEX idx_member (member_id)
);

-- =====================================================
-- 9. AIDES (SECOURS)
-- =====================================================

CREATE TABLE help_type (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    default_amount DECIMAL(15,2),
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE help (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    help_type_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    unit_amount DECIMAL(15,2) NOT NULL,
    target_amount DECIMAL(15,2) NOT NULL,
    collected_amount DECIMAL(15,2) DEFAULT 0,
    limit_date DATETIME NOT NULL,
    status ENUM('ACTIVE', 'COMPLETED', 'EXPIRED', 'CANCELLED') DEFAULT 'ACTIVE',
    eligible_verified BOOLEAN DEFAULT FALSE,
    verified_by BIGINT UNSIGNED,
    verified_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (help_type_id) REFERENCES help_type(id),
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    FOREIGN KEY (verified_by) REFERENCES administrator(id),
    INDEX idx_member (member_id),
    INDEX idx_status (status),
    INDEX idx_eligible (eligible_verified)
);

CREATE TABLE contribution (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    help_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status ENUM('PENDING', 'COMPLETED') DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (help_id) REFERENCES help(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member(id),
    INDEX idx_help (help_id),
    INDEX idx_member (member_id)
);

-- =====================================================
-- 10. INSCRIPTION ET FONDS SOCIAL
-- =====================================================

CREATE TABLE registration (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT UNSIGNED NOT NULL,
    exercise_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (exercise_id) REFERENCES exercise(id),
    UNIQUE KEY unique_member_exercise (member_id, exercise_id),
    INDEX idx_member (member_id),
    INDEX idx_exercise (exercise_id)
);

CREATE TABLE social_fund (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT UNSIGNED NOT NULL,
    exercise_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (exercise_id) REFERENCES exercise(id),
    INDEX idx_member (member_id),
    INDEX idx_exercise (exercise_id)
);

-- =====================================================
-- 11. AGAPÈ
-- =====================================================

CREATE TABLE agape (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL DEFAULT 45000,
    deducted_from_inscription BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
    INDEX idx_session (session_id)
);

-- =====================================================
-- 12. RENFLOUEMENT
-- =====================================================

CREATE TABLE refueling (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    exercise_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    total_outflows DECIMAL(15,2) NOT NULL,
    eligible_member_count INT NOT NULL,
    amount_per_member DECIMAL(15,2) NOT NULL,
    total_collected DECIMAL(15,2) NOT NULL,
    surplus_to_inscription DECIMAL(15,2) NOT NULL,
    distribution_date DATE NOT NULL,
    status ENUM('CALCULATED', 'DISTRIBUTED', 'CLOSED') DEFAULT 'CALCULATED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exercise_id) REFERENCES exercise(id) ON DELETE CASCADE,
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    INDEX idx_exercise (exercise_id),
    INDEX idx_status (status)
);

CREATE TABLE refueling_distribution (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    refueling_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    amount_received DECIMAL(15,2) NOT NULL,
    is_in_rule BOOLEAN NOT NULL,
    distributed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (refueling_id) REFERENCES refueling(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    INDEX idx_refueling (refueling_id),
    INDEX idx_member (member_id)
);

-- =====================================================
-- 13. PAIEMENTS GÉNÉRAL
-- =====================================================

CREATE TABLE payments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT UNSIGNED NOT NULL,
    cashbox_id BIGINT UNSIGNED NOT NULL,
    administrator_id BIGINT UNSIGNED NOT NULL,
    payment_type ENUM('INSCRIPTION', 'SOLIDARITY', 'REFUELING', 'SAVING_DEPOSIT', 'BORROWING_REFUND', 'PENALTY') NOT NULL,
    reference_id BIGINT UNSIGNED,
    amount DECIMAL(15,2) NOT NULL,
    payment_method ENUM('CASH', 'BANK_TRANSFER', 'MOBILE_MONEY') DEFAULT 'CASH',
    transaction_id VARCHAR(100),
    phone_number VARCHAR(20),
    status ENUM('PENDING', 'COMPLETED', 'FAILED') DEFAULT 'COMPLETED',
    payment_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (cashbox_id) REFERENCES cashbox(id),
    FOREIGN KEY (administrator_id) REFERENCES administrator(id),
    INDEX idx_member (member_id),
    INDEX idx_cashbox (cashbox_id),
    INDEX idx_payment_type (payment_type),
    INDEX idx_status (status)
);

-- =====================================================
-- 14. TRANSACTIONS (LOG UNIFIÉ)
-- =====================================================

CREATE TABLE transaction_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transaction_date DATETIME NOT NULL,
    member_id BIGINT UNSIGNED,
    cashbox_id BIGINT UNSIGNED NOT NULL,
    type ENUM('INFLOW', 'OUTFLOW') NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    reference_table VARCHAR(50),
    reference_id BIGINT UNSIGNED,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (cashbox_id) REFERENCES cashbox(id),
    INDEX idx_date (transaction_date),
    INDEX idx_member (member_id),
    INDEX idx_cashbox (cashbox_id),
    INDEX idx_type (type),
    INDEX idx_category (category)
);

-- =====================================================
-- 15. STATUT DES MEMBRES (HISTORIQUE)
-- =====================================================

CREATE TABLE member_status_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT UNSIGNED NOT NULL,
    status ENUM('IN_RULE', 'NOT_IN_RULE', 'INSOLVENT', 'INACTIVE') NOT NULL,
    solidarity_debt DECIMAL(15,2) NOT NULL,
    refueling_debt DECIMAL(15,2) NOT NULL,
    borrowing_debt DECIMAL(15,2) NOT NULL,
    total_debt DECIMAL(15,2) NOT NULL,
    calculated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    INDEX idx_member (member_id),
    INDEX idx_status (status),
    INDEX idx_calculated_at (calculated_at)
);

-- =====================================================
-- 16. COMMUNICATION (CHAT)
-- =====================================================

CREATE TABLE chat_message (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT UNSIGNED NOT NULL,
    receiver_id BIGINT UNSIGNED NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    external_message_id VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES user(id),
    FOREIGN KEY (receiver_id) REFERENCES user(id),
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
);

-- =====================================================
-- 17. CONFIGURATION ET HISTORIQUE
-- =====================================================

CREATE TABLE settings_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    setting_name VARCHAR(50) NOT NULL,
    old_value DECIMAL(15,2),
    new_value DECIMAL(15,2) NOT NULL,
    modified_by BIGINT UNSIGNED NOT NULL,
    modified_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (modified_by) REFERENCES administrator(id),
    INDEX idx_setting_name (setting_name),
    INDEX idx_modified_date (modified_date)
);

-- =====================================================
-- 18. VUES (Simplified if MySQL version or user permissions are restricted, but using the provided script)
-- =====================================================

-- =====================================================
-- 19. TRIGGERS
-- =====================================================

DELIMITER //

CREATE TRIGGER after_solidarity_insert
AFTER INSERT ON solidarity
FOR EACH ROW
BEGIN
    INSERT INTO solidarity_debt (member_id, total_paid, remaining_debt, last_payment_date, status)
    VALUES (NEW.member_id, NEW.amount, 150000 - NEW.amount, NEW.payment_date, 'UP_TO_DATE')
    ON DUPLICATE KEY UPDATE
        total_paid = total_paid + NEW.amount,
        remaining_debt = GREATEST(0, 150000 - (total_paid + NEW.amount)),
        last_payment_date = NEW.payment_date;
END//

CREATE TRIGGER after_payment_insert
AFTER INSERT ON payments
FOR EACH ROW
BEGIN
    INSERT INTO transaction_log (transaction_date, member_id, cashbox_id, type, category, amount, reference_table, reference_id, description)
    VALUES (
        NEW.payment_date, 
        NEW.member_id, 
        NEW.cashbox_id, 
        CASE WHEN NEW.payment_type IN ('INSCRIPTION', 'SOLIDARITY', 'REFUELING', 'SAVING_DEPOSIT', 'BORROWING_REFUND', 'PENALTY') THEN 'INFLOW' ELSE 'OUTFLOW' END,
        NEW.payment_type,
        NEW.amount,
        'payments',
        NEW.id,
        CONCAT('Paiement ', NEW.payment_type, ' - ', NEW.amount)
    );
    
    UPDATE cashbox 
    SET balance = balance + NEW.amount 
    WHERE id = NEW.cashbox_id;
END//

DELIMITER ;

-- =====================================================
-- 20. DONNÉES INITIALES
-- =====================================================

INSERT INTO user (id, name, first_name, tel, email, address, type, password, created_at) 
VALUES (1, 'Admin', 'Root', '000000000', 'root', 'Système', 'SUPER_ADMIN', '$2a$12$80PnFJjxCbroyQbgrHxCQ.4JAk9cJ8fPxKsAnmxtK8oaUicCeb8Pe', NOW());

INSERT INTO administrator (id, user_id, admin_role, username, active) 
VALUES (1, 1, 'PRESIDENT', 'root', TRUE);
