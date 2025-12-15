-- Créer un utilisateur SUPERADMIN pour les tests
INSERT INTO users (nom, prenom, email, password, role, email_verified, active, created_at, updated_at)
VALUES ('Admin', 'Super', 'superadmin@citesignal.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'SUPERADMIN', true, true, NOW(), NOW());