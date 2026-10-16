-- 01: the three demo users. Password for all: Demo-Password-2026 (bcrypt, verified with BCrypt.checkpw).
-- Ids: uid(1,1) admin, uid(1,2) manager, uid(1,3) consultant.
INSERT INTO app_user (id, email, name, role, active, language, password_hash, must_change_password, last_login_at, created_at)
VALUES
 (pg_temp.uid(1, 1), 'admin@demo.example',      'Demo Admin',      'ADMIN',      true, 'EN', '{bcrypt}$2a$10$lBkdXuCjLzG6rGlMLwgjsemTa5fzxm3ZTaJlWXeKDXd3LQDrEvZKS', false, now() - interval '1 day',        now() - interval '400 days'),
 (pg_temp.uid(1, 2), 'manager@demo.example',    'Demo Manager',    'MANAGER',    true, 'TR', '{bcrypt}$2a$10$lBkdXuCjLzG6rGlMLwgjsemTa5fzxm3ZTaJlWXeKDXd3LQDrEvZKS', false, now() - interval '3 hours',    now() - interval '400 days'),
 (pg_temp.uid(1, 3), 'consultant@demo.example', 'Demo Consultant', 'CONSULTANT', true, 'TR', '{bcrypt}$2a$10$lBkdXuCjLzG6rGlMLwgjsemTa5fzxm3ZTaJlWXeKDXd3LQDrEvZKS', false, now() - interval '20 minutes', now() - interval '400 days');
