-- V14: employment foundation permissions for Admin Employer/Candidate/Skill/
-- Candidate-Skill/Job-Category management (A7.1).
--
-- Why new permissions: the V2 seed does not cover employment profile or skill
-- administration. Reusing users:manage would blur identity administration with
-- employment profile management; reusing jobs:manage would be premature because
-- job postings are implemented in a later phase.
--
-- Two narrow codes are introduced:
--   employment:manage  — employer, skill, and job-category admin management
--   candidates:manage  — candidate profile and candidate↔skill admin management
--
-- Grants (only):
--   SUPER_ADMIN → employment:manage, candidates:manage
--   ADMIN       → employment:manage, candidates:manage
--
-- MODERATOR, EDITOR, CANDIDATE, EMPLOYER, CLIENT deliberately do NOT receive
-- these permissions. Anonymous callers reach only the explicit public endpoints.
--
-- Idempotent-safe by construction (WHERE NOT EXISTS guards); the unique
-- constraint uq_permissions_code backstops the permission insert.

INSERT INTO permissions (code, description)
SELECT 'employment:manage',
       'Manage employers, skills and job categories (admin-side).'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'employment:manage');

INSERT INTO permissions (code, description)
SELECT 'candidates:manage',
       'Manage candidate profiles and candidate↔skill assignments (admin-side).'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'candidates:manage');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'employment:manage'
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'candidates:manage'
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);
