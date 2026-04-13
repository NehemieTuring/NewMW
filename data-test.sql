-- =====================================================
-- SCRIPT DE PEUPLEMENT DE LA BASE DE DONNÉES DE TEST
-- MUTUELLE WEB
-- =====================================================

USE mutuelle_db;

-- Désactiver les contraintes de clé étrangère temporairement pour faciliter l'insertion
SET FOREIGN_KEY_CHECKS = 0;

-- Supprimer les données existantes (optionnel, à utiliser avec précaution)
TRUNCATE TABLE chat_message;
TRUNCATE TABLE member_status_log;
TRUNCATE TABLE transaction_log;
TRUNCATE TABLE payments;
TRUNCATE TABLE refueling_distribution;
TRUNCATE TABLE refueling;
TRUNCATE TABLE contribution;
TRUNCATE TABLE help;
TRUNCATE TABLE help_type;
TRUNCATE TABLE interest_distribution_detail;
TRUNCATE TABLE interest_distribution;
TRUNCATE TABLE penalty;
TRUNCATE TABLE refund;
TRUNCATE TABLE borrowing_saving;
TRUNCATE TABLE borrowing;
TRUNCATE TABLE saving;
TRUNCATE TABLE solidarity_debt;
TRUNCATE TABLE solidarity;
TRUNCATE TABLE social_fund;
TRUNCATE TABLE registration;
TRUNCATE TABLE agape;
TRUNCATE TABLE session;
TRUNCATE TABLE exercise;
TRUNCATE TABLE member;
TRUNCATE TABLE administrator;
TRUNCATE TABLE user;
TRUNCATE TABLE cashbox;
TRUNCATE TABLE settings_history;

-- Réactiver les contraintes
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 1. UTILISATEURS
-- =====================================================

-- Mot de passe encodé : "password123" (BCrypt)
INSERT INTO user (id, name, first_name, tel, email, address, type, avatar, password, created_at) VALUES
-- SUPER_ADMIN
(1, 'Admin', 'Super', '691234567', 'superadmin@mutuelle.com', 'Yaoundé', 'SUPER_ADMIN', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
-- Administrateurs
(2, 'Nkolo', 'Jean', '698765432', 'jean.nkolo@mutuelle.com', 'Douala', 'ADMIN', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(3, 'Essonba', 'Marie', '697123456', 'marie.essonba@mutuelle.com', 'Yaoundé', 'ADMIN', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(4, 'Mvondo', 'Paul', '696543210', 'paul.mvondo@mutuelle.com', 'Bafoussam', 'ADMIN', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
-- Membres (10 membres)
(5, 'Fouda', 'Alain', '655111222', 'alain.fouda@email.com', 'Yaoundé', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(6, 'Ngono', 'Brigitte', '655222333', 'brigitte.ngono@email.com', 'Douala', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(7, 'Tchinda', 'Claude', '655333444', 'claude.tchinda@email.com', 'Garoua', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(8, 'Wandji', 'Diane', '655444555', 'diane.wandji@email.com', 'Buea', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(9, 'Kouam', 'Eric', '655555666', 'eric.kouam@email.com', 'Bafoussam', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(10, 'Nana', 'Flore', '655666777', 'flore.nana@email.com', 'Ngaoundéré', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(11, 'Bikoro', 'Georges', '655777888', 'georges.bikoro@email.com', 'Kribi', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(12, 'Essama', 'Hélène', '655888999', 'helene.essama@email.com', 'Ebolowa', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(13, 'Mbeze', 'Isabelle', '655999000', 'isabelle.mbeze@email.com', 'Maroua', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
(14, 'Owona', 'Jacques', '655000111', 'jacques.owona@email.com', 'Yaoundé', 'MEMBER', NULL, '$2a$10$7aS0eH8E6uZq3LzXqGzZv.W6tR6dH9XqVbZ2dG7fQ8jL0pN1oI2q', NOW()),
-- SUPER_ADMIN ROOT (NOUVEAU) - mdp: root
(15, 'Root', 'SuperAdmin', '000000000', 'root@mutuelle.com', 'Système', 'SUPER_ADMIN', NULL, '$2a$12$80PnFJjxCbroyQbgrHxCQ.4JAk9cJ8fPxKsAnmxtK8oaUicCeb8Pe', NOW());

-- =====================================================
-- 2. ADMINISTRATEURS
-- =====================================================

-- Les IDs user : super_admin=1, jean=2, marie=3, paul=4, root=15
INSERT INTO administrator (id, user_id, admin_role, username, active) VALUES
(1, 1, 'SECRETAIRE_GENERALE', 'superadmin', TRUE),
(2, 2, 'SECRETAIRE_GENERALE', 'jean_nkolo', TRUE),
(3, 3, 'PRESIDENT', 'marie_essonba', TRUE),
(4, 4, 'TRESORIER', 'paul_mvondo', TRUE),
(5, 15, 'SECRETAIRE_GENERALE', 'root', TRUE);

-- =====================================================
-- 3. MEMBRES
-- =====================================================

-- Membres (IDs user 5 à 14)
-- registration_number, username, administrator_id (ici on utilise l'admin 2 (Jean) comme créateur pour tous)
INSERT INTO member (user_id, administrator_id, registration_number, username, active, inscription_date) VALUES
(5, 2, 'MUT-2025-001', 'alain_fouda', TRUE, DATE_SUB(NOW(), INTERVAL 8 MONTH)),
(6, 2, 'MUT-2025-002', 'brigitte_ngono', TRUE, DATE_SUB(NOW(), INTERVAL 6 MONTH)),
(7, 2, 'MUT-2025-003', 'claude_tchinda', TRUE, DATE_SUB(NOW(), INTERVAL 5 MONTH)),
(8, 2, 'MUT-2025-004', 'diane_wandji', TRUE, DATE_SUB(NOW(), INTERVAL 4 MONTH)),
(9, 2, 'MUT-2025-005', 'eric_kouam', TRUE, DATE_SUB(NOW(), INTERVAL 3 MONTH)),
(10, 2, 'MUT-2025-006', 'flore_nana', TRUE, DATE_SUB(NOW(), INTERVAL 2 MONTH)),
(11, 2, 'MUT-2025-007', 'georges_bikoro', TRUE, DATE_SUB(NOW(), INTERVAL 1 MONTH)),
(12, 2, 'MUT-2025-008', 'helene_essama', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(13, 2, 'MUT-2025-009', 'isabelle_mbeze', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(14, 2, 'MUT-2025-010', 'jacques_owona', TRUE, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- Administrateurs en tant que membres
(2, 2, 'MUT-EXEC-001', 'jean_nkolo', TRUE, DATE_SUB(NOW(), INTERVAL 1 YEAR)),
(3, 2, 'MUT-EXEC-002', 'marie_essonba', TRUE, DATE_SUB(NOW(), INTERVAL 1 YEAR)),
(4, 2, 'MUT-EXEC-003', 'paul_mvondo', TRUE, DATE_SUB(NOW(), INTERVAL 1 YEAR));

-- =====================================================
-- 4. EXERCICES
-- =====================================================

-- Exercice précédent (2024) clôturé, et exercice actuel (2025)
INSERT INTO exercise (id, year, start_date, end_date, interest_rate, inscription_amount, solidarity_amount, agape_amount, penalty_amount, active, administrator_id) VALUES
(1, '2024', '2024-01-01', '2024-12-31', 3.00, 50000, 150000, 45000, 15000, FALSE, 2),
(2, '2025', '2025-01-01', '2025-12-31', 3.00, 50000, 150000, 45000, 15000, TRUE, 2);

-- =====================================================
-- 5. SESSIONS
-- =====================================================

-- Session 1 (2024, clôturée), Session 2 (2025, en cours), Session 3 (2025, clôturée)
INSERT INTO session (id, exercise_id, administrator_id, session_number, date, state, active, closed_at) VALUES
(1, 1, 2, 1, '2024-01-15', 'CLOSED', FALSE, '2024-01-20 18:00:00'),
(2, 1, 2, 2, '2024-06-15', 'CLOSED', FALSE, '2024-06-20 18:00:00'),
(3, 2, 2, 1, '2025-01-15', 'OPEN', TRUE, NULL),
(4, 2, 2, 2, '2025-06-15', 'CLOSED', FALSE, '2025-06-20 18:00:00');

-- =====================================================
-- 6. SOLIDARITÉ (paiements et dettes)
-- =====================================================

-- Paiements pour chaque membre
-- Pour les membres 5-14, on simule des situations variées :
-- Membre 5: a payé entièrement (150000) en deux fois
-- Membre 6: a payé 100000 (dette 50000)
-- Membre 7: a payé 0 (dette 150000)
-- Membre 8: a payé 150000 (OK)
-- Membre 9: a payé 50000 (dette 100000)
-- Membre 10: a payé 0 (dette 150000)
-- Membre 11: a payé 150000 (OK)
-- Membre 12: a payé 0 (dette 150000) mais récent
-- Membre 13: a payé 150000 (OK)
-- Membre 14: a payé 0 (dette 150000) très récent

INSERT INTO solidarity (member_id, administrator_id, amount, payment_date, payment_method, notes) VALUES
(1, 2, 100000, DATE_SUB(NOW(), INTERVAL 7 MONTH), 'CASH', 'Premier versement'),
(1, 2, 50000, DATE_SUB(NOW(), INTERVAL 5 MONTH), 'BANK_TRANSFER', 'Solde'),
(2, 2, 100000, DATE_SUB(NOW(), INTERVAL 4 MONTH), 'CASH', NULL),
(4, 2, 150000, DATE_SUB(NOW(), INTERVAL 3 MONTH), 'MOBILE_MONEY', NULL),
(5, 2, 50000, DATE_SUB(NOW(), INTERVAL 2 MONTH), 'CASH', NULL),
(7, 2, 150000, DATE_SUB(NOW(), INTERVAL 1 MONTH), 'BANK_TRANSFER', NULL),
(9, 2, 150000, DATE_SUB(NOW(), INTERVAL 15 DAY), 'CASH', NULL);

-- Les dettes sont mises à jour par le trigger, mais on peut insérer directement dans solidarity_debt pour initialiser
-- (les triggers les mettront à jour automatiquement lors des insertions ci-dessus, donc pas besoin d'insertion manuelle)

-- =====================================================
-- 7. ÉPARGNE
-- =====================================================

-- Pour chaque session, des dépôts et retraits pour divers membres
-- Session 1 (2024-01-15)
INSERT INTO saving (member_id, session_id, administrator_id, amount, cumulative_total, type) VALUES
(1, 1, 2, 50000, 50000, 'DEPOSIT'),
(2, 1, 2, 30000, 30000, 'DEPOSIT'),
(3, 1, 2, 10000, 10000, 'DEPOSIT'),
(4, 1, 2, 20000, 20000, 'DEPOSIT'),
(5, 1, 2, 15000, 15000, 'DEPOSIT'),
(6, 1, 2, 25000, 25000, 'DEPOSIT');

-- Session 2 (2024-06-15)
INSERT INTO saving (member_id, session_id, administrator_id, amount, cumulative_total, type) VALUES
(1, 2, 2, 20000, 70000, 'DEPOSIT'),
(2, 2, 2, 5000, 35000, 'DEPOSIT'),
(3, 2, 2, 30000, 40000, 'DEPOSIT'),
(4, 2, 2, 10000, 30000, 'DEPOSIT'),
(5, 2, 2, 5000, 20000, 'DEPOSIT'),
(7, 2, 2, 40000, 40000, 'DEPOSIT');

-- Session 3 (2025-01-15, ouverte)
INSERT INTO saving (member_id, session_id, administrator_id, amount, cumulative_total, type) VALUES
(1, 3, 2, 10000, 80000, 'DEPOSIT'),
(2, 3, 2, 5000, 40000, 'DEPOSIT'),
(3, 3, 2, 10000, 50000, 'DEPOSIT'),
(4, 3, 2, 20000, 50000, 'DEPOSIT'),
(5, 3, 2, 5000, 25000, 'DEPOSIT'),
(6, 3, 2, 10000, 35000, 'DEPOSIT'),
(7, 3, 2, 15000, 55000, 'DEPOSIT'),
(8, 3, 2, 5000, 5000, 'DEPOSIT'),
(9, 3, 2, 20000, 20000, 'DEPOSIT'),
(10, 3, 2, 1000, 1000, 'DEPOSIT');

-- Session 4 (2025-06-15, clôturée)
INSERT INTO saving (member_id, session_id, administrator_id, amount, cumulative_total, type) VALUES
(1, 4, 2, 5000, 85000, 'DEPOSIT'),
(2, 4, 2, 10000, 50000, 'DEPOSIT'),
(3, 4, 2, 20000, 70000, 'DEPOSIT'),
(4, 4, 2, 5000, 55000, 'DEPOSIT'),
(5, 4, 2, 10000, 35000, 'DEPOSIT'),
(7, 4, 2, 20000, 75000, 'DEPOSIT'),
(9, 4, 2, 10000, 30000, 'DEPOSIT');

-- Retraits (un seul pour tester)
INSERT INTO saving (member_id, session_id, administrator_id, amount, cumulative_total, type) VALUES
(1, 4, 2, 10000, 75000, 'WITHDRAWAL');

-- =====================================================
-- 8. EMPRUNTS
-- =====================================================

-- Emprunts pour divers membres
-- Règle: 97% versé, 3% intérêts. Les emprunts sont accordés pendant une session.
-- On crée des emprunts avec différents statuts

-- Emprunt 1: membre 5, session 3, montant 200000, reçu 194000, intérêt 6000, due_date dans 6 mois
INSERT INTO borrowing (id, member_id, session_id, administrator_id, requested_amount, approved_amount, interest_amount, net_amount_received, remaining_balance, status, due_date) VALUES
(1, 1, 3, 2, 200000, 200000, 6000, 194000, 200000, 'ACTIVE', DATE_ADD(NOW(), INTERVAL 6 MONTH));

-- Emprunt 2: membre 6, session 4, montant 100000, reçu 97000, intérêt 3000, due_date dans 3 mois
INSERT INTO borrowing (id, member_id, session_id, administrator_id, requested_amount, approved_amount, interest_amount, net_amount_received, remaining_balance, status, due_date) VALUES
(2, 2, 4, 2, 100000, 100000, 3000, 97000, 100000, 'ACTIVE', DATE_ADD(NOW(), INTERVAL 3 MONTH));

-- Emprunt 3: membre 7, session 4, montant 150000, reçu 145500, intérêt 4500, due_date déjà dépassée (il y a 1 mois) -> pénalité
INSERT INTO borrowing (id, member_id, session_id, administrator_id, requested_amount, approved_amount, interest_amount, net_amount_received, remaining_balance, status, due_date) VALUES
(3, 3, 4, 2, 150000, 150000, 4500, 145500, 150000, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 MONTH));

-- Emprunt 4: membre 8, session 3, montant 50000, reçu 48500, intérêt 1500, remboursé partiellement
INSERT INTO borrowing (id, member_id, session_id, administrator_id, requested_amount, approved_amount, interest_amount, net_amount_received, remaining_balance, status, due_date) VALUES
(4, 4, 3, 2, 50000, 50000, 1500, 48500, 30000, 'ACTIVE', DATE_ADD(NOW(), INTERVAL 4 MONTH));

-- Emprunt 5: membre 9, session 3, montant 80000, reçu 77600, intérêt 2400, remboursé entièrement (complet)
INSERT INTO borrowing (id, member_id, session_id, administrator_id, requested_amount, approved_amount, interest_amount, net_amount_received, remaining_balance, status, due_date) VALUES
(5, 5, 3, 2, 80000, 80000, 2400, 77600, 0, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 MONTH));

-- =====================================================
-- 9. REMBOURSEMENTS
-- =====================================================

-- Remboursements pour l'emprunt 4 (membre 8)
INSERT INTO refund (borrowing_id, member_id, session_id, exercise_id, amount, remaining_balance, refund_date) VALUES
(4, 4, 4, 2, 20000, 30000, DATE_SUB(NOW(), INTERVAL 1 MONTH));

-- Remboursement complet pour l'emprunt 5
INSERT INTO refund (borrowing_id, member_id, session_id, exercise_id, amount, remaining_balance, refund_date) VALUES
(5, 5, 4, 2, 80000, 0, DATE_SUB(NOW(), INTERVAL 1 MONTH));

-- =====================================================
-- 10. PÉNALITÉS
-- =====================================================

-- Pénalité pour l'emprunt 3 (membre 7) car due_date dépassée
INSERT INTO penalty (borrowing_id, member_id, administrator_id, amount, applied_date, status, distributed_to_savers) VALUES
(3, 3, 2, 15000, DATE_SUB(NOW(), INTERVAL 15 DAY), 'PENDING', FALSE);

-- =====================================================
-- 11. AIDES (SECOURS)
-- =====================================================

-- Types d'aide
INSERT INTO help_type (id, name, description, default_amount, active) VALUES
(1, 'Maladie grave', 'Aide pour frais médicaux', 100000, TRUE),
(2, 'Décès', 'Aide funéraire', 200000, TRUE),
(3, 'Accident', 'Aide suite à accident', 150000, TRUE);

-- Aides
-- Aide 1: pour membre 10 (maladie), active, unit_amount 20000, target 100000, limit_date dans 30 jours
INSERT INTO help (id, help_type_id, member_id, administrator_id, unit_amount, target_amount, collected_amount, limit_date, status, eligible_verified) VALUES
(1, 1, 6, 2, 20000, 100000, 0, DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', TRUE);

-- Aide 2: pour membre 11 (décès), complétée
INSERT INTO help (id, help_type_id, member_id, administrator_id, unit_amount, target_amount, collected_amount, limit_date, status, eligible_verified) VALUES
(2, 2, 7, 2, 25000, 200000, 200000, DATE_SUB(NOW(), INTERVAL 10 DAY), 'COMPLETED', TRUE);

-- Aide 3: pour membre 12 (accident), expirée
INSERT INTO help (id, help_type_id, member_id, administrator_id, unit_amount, target_amount, collected_amount, limit_date, status, eligible_verified) VALUES
(3, 3, 8, 2, 30000, 150000, 90000, DATE_SUB(NOW(), INTERVAL 5 DAY), 'EXPIRED', TRUE);

-- Contributions pour l'aide 2 (complétée)
INSERT INTO contribution (help_id, member_id, amount, status) VALUES
(2, 1, 25000, 'COMPLETED'),
(2, 2, 25000, 'COMPLETED'),
(2, 3, 25000, 'COMPLETED'),
(2, 4, 25000, 'COMPLETED'),
(2, 5, 25000, 'COMPLETED'),
(2, 9, 25000, 'COMPLETED'),
(2, 10, 25000, 'COMPLETED'),
(2, 6, 25000, 'COMPLETED');

-- Contributions pour l'aide 3 (expirée)
INSERT INTO contribution (help_id, member_id, amount, status) VALUES
(3, 1, 30000, 'COMPLETED'),
(3, 2, 30000, 'COMPLETED'),
(3, 3, 30000, 'COMPLETED');

-- =====================================================
-- 12. INSCRIPTION ET FONDS SOCIAL (pour l'exercice 2025)
-- =====================================================

-- Inscription des membres pour 2025 (ils ont tous payé 50000 à leur inscription)
-- La table registration est remplie automatiquement lors de l'inscription du membre (via trigger? non, pas de trigger pour ça). On l'alimente manuellement.
INSERT INTO registration (member_id, exercise_id, amount, payment_date) VALUES
(1, 2, 50000, (SELECT inscription_date FROM member WHERE id=1)),
(2, 2, 50000, (SELECT inscription_date FROM member WHERE id=2)),
(3, 2, 50000, (SELECT inscription_date FROM member WHERE id=3)),
(4, 2, 50000, (SELECT inscription_date FROM member WHERE id=4)),
(5, 2, 50000, (SELECT inscription_date FROM member WHERE id=5)),
(6, 2, 50000, (SELECT inscription_date FROM member WHERE id=6)),
(7, 2, 50000, (SELECT inscription_date FROM member WHERE id=7)),
(8, 2, 50000, (SELECT inscription_date FROM member WHERE id=8)),
(9, 2, 50000, (SELECT inscription_date FROM member WHERE id=9)),
(10, 2, 50000, (SELECT inscription_date FROM member WHERE id=10));

-- =====================================================
-- 13. AGAPÈ
-- =====================================================

-- Agapè pour les sessions
INSERT INTO agape (session_id, amount, deducted_from_inscription) VALUES
(1, 45000, TRUE),
(2, 45000, TRUE),
(3, 45000, TRUE),
(4, 45000, TRUE);

-- =====================================================
-- 14. RENFLOUEMENT (pour l'exercice 2024 clôturé)
-- =====================================================

-- Calcul du renflouement pour l'exercice 2024
INSERT INTO refueling (id, exercise_id, administrator_id, total_outflows, eligible_member_count, amount_per_member, total_collected, surplus_to_inscription, distribution_date, status) VALUES
(1, 1, 2, 300000, 4, 75000, 300000, 0, '2024-12-31', 'DISTRIBUTED');

-- Distribution aux membres présents en 2024 
INSERT INTO refueling_distribution (refueling_id, member_id, amount_received, is_in_rule) VALUES
(1, 1, 75000, TRUE),
(1, 2, 75000, TRUE),
(1, 3, 0, FALSE),
(1, 4, 75000, TRUE),
(1, 5, 75000, TRUE),
(1, 6, 0, FALSE);

-- =====================================================
-- 15. INTÉRÊTS DES EMPRUNTS (pour la session 4 clôturée)
-- =====================================================

-- Distribution des intérêts pour la session 4
INSERT INTO interest_distribution (id, borrowing_id, session_id, administrator_id, total_interest, distributed_amount, remaining_amount, status, distribution_date) VALUES
(1, 2, 4, 2, 3000, 3000, 0, 'COMPLETED', NOW()),
(2, 3, 4, 2, 4500, 4500, 0, 'COMPLETED', NOW());

-- Détails de distribution pour l'emprunt 2 
INSERT INTO interest_distribution_detail (distribution_id, member_id, amount_received) VALUES
(1, 1, 428), 
(1, 2, 428),
(1, 3, 428),
(1, 4, 428),
(1, 5, 428),
(1, 7, 428),
(1, 9, 428);

-- Pour l'emprunt 3, même chose
INSERT INTO interest_distribution_detail (distribution_id, member_id, amount_received) VALUES
(2, 1, 642),
(2, 2, 642),
(2, 3, 642),
(2, 4, 642),
(2, 5, 642),
(2, 7, 642),
(2, 9, 642);

-- =====================================================
-- 16. MESSAGES (CHAT)
-- =====================================================

-- Messages entre membres et administrateurs
INSERT INTO chat_message (sender_id, receiver_id, message, is_read, created_at) VALUES
(5, 2, 'Bonjour, je souhaite faire une demande d''emprunt', FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 5, 'Bonjour, veuillez remplir le formulaire en ligne', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 3, 'Quand est la prochaine session ?', FALSE, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 6, 'La prochaine session est prévue le 15 du mois prochain', TRUE, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(7, 4, 'J''ai un problème avec mon remboursement', FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 7, 'Je vous contacte en privé pour résoudre', TRUE, NOW()),
(8, 9, 'As-tu reçu ton aide ?', FALSE, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(9, 8, 'Oui, merci !', TRUE, DATE_SUB(NOW(), INTERVAL 10 HOUR));

-- =====================================================
-- 17. HISTORIQUE DES PARAMÈTRES
-- =====================================================

INSERT INTO settings_history (setting_name, old_value, new_value, modified_by, modified_date) VALUES
('inscription_amount', NULL, 50000, 2, '2025-01-01 00:00:00'),
('solidarity_amount', NULL, 150000, 2, '2025-01-01 00:00:00'),
('agape_amount', NULL, 45000, 2, '2025-01-01 00:00:00'),
('penalty_amount', NULL, 15000, 2, '2025-01-01 00:00:00');

-- =====================================================
-- 18. MISE À JOUR DES CAISSES
-- =====================================================

UPDATE cashbox SET balance = 320000 WHERE name = 'INSCRIPTION';
UPDATE cashbox SET balance = 750000 WHERE name = 'SOLIDARITY';
UPDATE cashbox SET balance = 3400 WHERE name = 'SAVING';
UPDATE cashbox SET balance = 0 WHERE name = 'REFUELING';
UPDATE cashbox SET balance = 0 WHERE name = 'PENALTY';

-- =====================================================
-- 19. MISE À JOUR DES STATUTS MEMBRES
-- =====================================================

INSERT INTO member_status_log (member_id, status, solidarity_debt, refueling_debt, borrowing_debt, total_debt, calculated_at)
SELECT 
    m.id,
    CASE 
        WHEN sd.remaining_debt IS NULL OR sd.remaining_debt > 0 THEN 'NOT_IN_RULE'
        ELSE 'IN_RULE'
    END,
    COALESCE(sd.remaining_debt, 150000),
    0, 
    COALESCE(b.total_borrowing, 0),
    COALESCE(sd.remaining_debt, 150000) + COALESCE(b.total_borrowing, 0),
    NOW()
FROM member m
LEFT JOIN solidarity_debt sd ON m.id = sd.member_id
LEFT JOIN (SELECT member_id, SUM(remaining_balance) AS total_borrowing FROM borrowing WHERE status IN ('ACTIVE','PENDING') GROUP BY member_id) b ON m.id = b.member_id;

-- =====================================================
-- 20. MISE À JOUR DES DETTES SOLIDARITÉ
-- =====================================================

-- For member IDs 3,6,8,10
INSERT INTO solidarity_debt (member_id, total_due, total_paid, remaining_debt, last_payment_date, status) VALUES
(3, 150000, 0, 150000, NULL, 'CRITICAL'),
(6, 150000, 0, 150000, NULL, 'CRITICAL'),
(8, 150000, 0, 150000, NULL, 'LATE'),
(10, 150000, 0, 150000, NULL, 'UP_TO_DATE')
ON DUPLICATE KEY UPDATE remaining_debt = VALUES(remaining_debt), status = VALUES(status);

-- =====================================================
-- FIN DU SCRIPT
-- =====================================================

SELECT 'Données de test insérées avec succès.' AS Message;
